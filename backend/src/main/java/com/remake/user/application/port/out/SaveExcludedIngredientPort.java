package com.remake.user.application.port.out;

import java.util.List;

import com.remake.user.domain.AllergenCode;

public interface SaveExcludedIngredientPort {

	/** 기존 제외 재료를 지우고 codes로 통째 교체한다. 빈 목록이면 전부 삭제. */
	void replaceAll(Long userId, List<AllergenCode> codes);
}
