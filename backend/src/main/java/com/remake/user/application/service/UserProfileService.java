package com.remake.user.application.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.user.application.port.in.CompleteOnboardingUseCase;
import com.remake.user.application.port.in.GetExcludedIngredientsUseCase;
import com.remake.user.application.port.in.GetProfileUseCase;
import com.remake.user.application.port.in.GetUserGenerationPreferencesUseCase;
import com.remake.user.application.port.in.ReplaceExcludedIngredientsUseCase;
import com.remake.user.application.port.in.UpdateProfileUseCase;
import com.remake.user.application.port.out.LoadExcludedIngredientPort;
import com.remake.user.application.port.out.LoadUserPort;
import com.remake.user.application.port.out.SaveExcludedIngredientPort;
import com.remake.user.application.port.out.SaveUserPort;
import com.remake.user.domain.AllergenCode;
import com.remake.user.domain.User;

import lombok.RequiredArgsConstructor;

/**
 * 온보딩·프로필·제외 재료. AI 생성은 {@link GetUserGenerationPreferencesUseCase}만 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService implements CompleteOnboardingUseCase, GetProfileUseCase, UpdateProfileUseCase,
		GetExcludedIngredientsUseCase, ReplaceExcludedIngredientsUseCase, GetUserGenerationPreferencesUseCase {

	private final LoadUserPort loadUserPort;
	private final SaveUserPort saveUserPort;
	private final LoadExcludedIngredientPort loadExcludedIngredientPort;
	private final SaveExcludedIngredientPort saveExcludedIngredientPort;
	private final EventTracker eventTracker;

	@Override
	public void complete(CompleteOnboardingUseCase.Command command) {
		User user = requireUser(command.userId());
		List<AllergenCode> codes = normalizeCodes(command.allergenCodes());
		try {
			user = user.completeOnboarding(command.householdSize());
		}
		catch (IllegalStateException | IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
		}
		User saved = saveUserPort.save(user);
		saveExcludedIngredientPort.replaceAll(saved.id(), codes);
		eventTracker.track(EventName.ONBOARDING_COMPLETED, Screen.ONBOARDING,
				Map.of("excluded_count", codes.size()),
				EventTrackCommand.ofUser(saved.id()));
	}

	@Override
	@Transactional(readOnly = true)
	public Profile getProfile(Long userId) {
		return toProfile(requireUser(userId));
	}

	@Override
	public Profile updateProfile(UpdateProfileUseCase.Command command) {
		User user = requireUser(command.userId());
		try {
			user = user.updateProfile(command.householdSize(), command.recipeSupplementEnabled(),
					command.notificationEnabled());
		}
		catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
		}
		return toProfile(saveUserPort.save(user));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AllergenCode> getAllergenCodes(Long userId) {
		requireUser(userId);
		return loadExcludedIngredientPort.findAllergenCodesByUserId(userId);
	}

	@Override
	public List<AllergenCode> replaceAllergenCodes(Long userId, List<AllergenCode> allergenCodes) {
		requireUser(userId);
		List<AllergenCode> codes = normalizeCodes(allergenCodes);
		saveExcludedIngredientPort.replaceAll(userId, codes);
		return loadExcludedIngredientPort.findAllergenCodesByUserId(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public Preferences getPreferences(Long userId) {
		User user = requireUser(userId);
		return new Preferences(user.householdSize(), user.recipeSupplementEnabled(),
				loadExcludedIngredientPort.findAllergenCodesByUserId(userId));
	}

	private Profile toProfile(User user) {
		return new Profile(
				user.provider(),
				user.plan(),
				user.householdSize(),
				loadExcludedIngredientPort.countByUserId(user.id()),
				user.recipeSupplementEnabled(),
				user.notificationEnabled(),
				user.lastMealPurpose(),
				user.onboardingCompleted());
	}

	private static List<AllergenCode> normalizeCodes(List<AllergenCode> allergenCodes) {
		if (allergenCodes == null || allergenCodes.isEmpty()) {
			return List.of();
		}
		return List.copyOf(new LinkedHashSet<>(allergenCodes));
	}

	private User requireUser(Long userId) {
		return loadUserPort.findById(userId)
				.filter(User::isActive)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}
}
