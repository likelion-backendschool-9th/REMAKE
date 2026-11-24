package com.remake.user.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.remake.user.application.port.out.LoadExcludedIngredientPort;
import com.remake.user.application.port.out.SaveExcludedIngredientPort;
import com.remake.user.domain.AllergenCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ExcludedIngredientPersistenceAdapter implements LoadExcludedIngredientPort, SaveExcludedIngredientPort {

	private final UserExcludedIngredientJpaRepository repository;

	@Override
	public List<AllergenCode> findAllergenCodesByUserId(Long userId) {
		return repository.findByUserIdOrderByAllergenCodeAsc(userId).stream()
				.map(UserExcludedIngredientJpaEntity::getAllergenCode)
				.toList();
	}

	@Override
	public int countByUserId(Long userId) {
		return repository.countByUserId(userId);
	}

	@Override
	public void replaceAll(Long userId, List<AllergenCode> codes) {
		repository.deleteByUserId(userId);
		if (codes == null || codes.isEmpty()) {
			return;
		}
		repository.saveAll(codes.stream().map(code -> UserExcludedIngredientJpaEntity.of(userId, code)).toList());
	}
}
