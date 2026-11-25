package com.remake.mealplan.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.mealplan.application.port.out.LoadMealPlanPort;
import com.remake.mealplan.application.port.out.LoadMealPlanSlotPort;
import com.remake.mealplan.application.port.out.SaveMealPlanPort;
import com.remake.mealplan.application.port.out.SaveMealPlanSlotPort;
import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanSlot;
import com.remake.mealplan.domain.MealPlanStatus;

import lombok.RequiredArgsConstructor;

/** 식단표·칸 읽기·저장 Port 구현. 엔티티는 이 패키지 밖으로 나가지 않는다. */
@Component
@RequiredArgsConstructor
class MealPlanPersistenceAdapter
		implements LoadMealPlanPort, SaveMealPlanPort, LoadMealPlanSlotPort, SaveMealPlanSlotPort {

	/** V6__create_meal_plans.sql의 "회원당 생성 중인 식단표는 하나" 인덱스 */
	static final String GENERATING_UNIQUE_INDEX = "uk_meal_plans_user_generating";

	private static final Comparator<MealPlanSlot> SLOT_ORDER = Comparator.comparing(MealPlanSlot::slotDate)
			.thenComparing(MealPlanSlot::mealType);

	private final MealPlanJpaRepository mealPlanRepository;
	private final MealPlanSlotJpaRepository slotRepository;

	@Override
	public boolean existsGenerating(Long userId) {
		return mealPlanRepository.existsByUserIdAndStatus(userId, MealPlanStatus.GENERATING);
	}

	@Override
	public boolean existsWithStatus(Long userId, MealPlanStatus status) {
		return mealPlanRepository.existsByUserIdAndStatus(userId, status);
	}

	@Override
	public Optional<MealPlan> loadById(Long mealPlanId) {
		return mealPlanRepository.findById(mealPlanId).map(MealPlanJpaEntity::toDomain);
	}

	@Override
	public List<MealPlan> loadByStatusStartingBetween(Long userId, MealPlanStatus status, LocalDate from,
			LocalDate to) {
		return mealPlanRepository
				.findByUserIdAndStatusAndStartDateBetweenOrderByStartDateAscIdAsc(userId, status, from, to)
				.stream()
				.map(MealPlanJpaEntity::toDomain)
				.toList();
	}

	@Override
	public Optional<MealPlan> loadByIdForUpdate(Long mealPlanId) {
		return mealPlanRepository.findByIdForUpdate(mealPlanId).map(MealPlanJpaEntity::toDomain);
	}

	@Override
	public List<GeneratingMealPlan> loadGeneratingAcceptedBefore(LocalDateTime acceptedBefore) {
		return mealPlanRepository
				.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(MealPlanStatus.GENERATING, acceptedBefore)
				.stream()
				.map(entity -> new GeneratingMealPlan(entity.getId(), entity.getCreatedAt()))
				.toList();
	}

	@Override
	public MealPlan saveNew(MealPlan mealPlan) {
		if (mealPlan.id() != null) {
			throw new IllegalArgumentException("이미 저장된 식단표입니다: " + mealPlan.id());
		}
		try {
			// 유니크 인덱스 위반을 여기서 잡으려고 바로 flush한다(커밋 때 터지면 500이 된다)
			return mealPlanRepository.saveAndFlush(MealPlanJpaEntity.from(mealPlan)).toDomain();
		} catch (DataIntegrityViolationException e) {
			String cause = String.valueOf(NestedExceptionUtils.getMostSpecificCause(e).getMessage());
			if (cause.contains(GENERATING_UNIQUE_INDEX)) {
				throw new BusinessException(ErrorCode.MEAL_PLAN_IN_PROGRESS);
			}
			throw e;
		}
	}

	@Override
	public void update(MealPlan mealPlan) {
		MealPlanJpaEntity entity = mealPlanRepository.findById(mealPlan.id())
				.orElseThrow(() -> new IllegalStateException("식단표가 없습니다: " + mealPlan.id()));
		entity.apply(mealPlan);
	}

	@Override
	public List<MealPlanSlot> loadUserSlotsBetween(Long userId, LocalDate from, LocalDate to) {
		return toSortedDomain(slotRepository.findUserSlotsBetween(userId, from, to));
	}

	@Override
	public List<MealPlanSlot> loadUserSlotsFrom(Long userId, LocalDate from) {
		return toSortedDomain(slotRepository.findUserSlotsFrom(userId, from));
	}

	@Override
	public void saveAll(List<MealPlanSlot> slots) {
		slotRepository.saveAll(slots.stream().map(MealPlanSlotJpaEntity::from).toList());
	}

	@Override
	public void deleteAllById(Collection<Long> slotIds) {
		if (!slotIds.isEmpty()) {
			slotRepository.deleteAllByIdInBatch(slotIds);
		}
	}

	private static List<MealPlanSlot> toSortedDomain(List<MealPlanSlotJpaEntity> entities) {
		return entities.stream().map(MealPlanSlotJpaEntity::toDomain).sorted(SLOT_ORDER).toList();
	}
}
