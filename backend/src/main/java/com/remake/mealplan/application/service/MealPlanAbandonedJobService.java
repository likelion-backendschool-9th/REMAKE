package com.remake.mealplan.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.remake.global.error.ErrorCode;
import com.remake.mealplan.application.port.in.FailAbandonedMealPlansUseCase;
import com.remake.mealplan.application.port.out.LoadMealPlanPort;
import com.remake.mealplan.application.port.out.LoadMealPlanPort.GeneratingMealPlan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 끊긴 생성 작업 정리. 접수 뒤 {@link #ABANDONED_AFTER}가 지나도 GENERATING인 식단표를 실패로 끝낸다.
 *
 * <p>정상 작업은 몇십 초 안에 스스로 완성·실패로 끝나므로 이 정리에 걸리지 않는다. 걸리는 것은 배포로 컨테이너가 멈추며
 * 사라진 작업(대기열 포함)뿐이다. 실패 알림·횟수(차감 없음)·기존 식단(그대로)은 일반 실패와 같다.
 * E54 error_code는 AI_TIMEOUT: errors.md가 식단표 서버 작업 시간 제한을 AI_TIMEOUT으로 둔다(시간 제한 값은 #122 미결).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanAbandonedJobService implements FailAbandonedMealPlansUseCase {

	/**
	 * 끊긴 작업으로 보는 기준(가이드 05 "제안" 값, #122 서버 작업 시간 제한이 정해지면 맞춘다).
	 * 정상 작업이 걸릴 수 있는 최대 시간(AI 호출 시간 제한 × 시도 횟수 + 대기열에서 기다리는 시간)보다 길어야 한다.
	 * 더 짧으면 느리지만 살아 있는 작업을 실패로 끝내 버린다(늦게 끝난 결과는 버려진다).
	 */
	static final Duration ABANDONED_AFTER = Duration.ofMinutes(10);

	static final String ABANDONED_ERROR_CODE = ErrorCode.AI_TIMEOUT.name();

	private final LoadMealPlanPort loadMealPlanPort;
	private final MealPlanResultWriter resultWriter;
	private final Clock clock;

	@Override
	public int failAbandonedMealPlans() {
		LocalDateTime acceptedBefore = LocalDateTime.now(clock).minus(ABANDONED_AFTER);
		List<GeneratingMealPlan> abandoned = loadMealPlanPort.loadGeneratingAcceptedBefore(acceptedBefore);
		int failed = 0;
		for (GeneratingMealPlan plan : abandoned) {
			try {
				if (resultWriter.failAbandoned(plan.mealPlanId(), plan.acceptedAt(), ABANDONED_ERROR_CODE)) {
					failed++;
					log.warn("끊긴 식단표 생성 작업을 실패로 끝냄 mealPlanId={} acceptedAt={}", plan.mealPlanId(),
							plan.acceptedAt());
				}
			} catch (RuntimeException e) {
				// 하나가 실패해도 나머지는 계속한다. 남은 것은 다음 실행에서 다시 본다
				log.error("끊긴 식단표 정리 중 오류 mealPlanId={}", plan.mealPlanId(), e);
			}
		}
		return failed;
	}
}
