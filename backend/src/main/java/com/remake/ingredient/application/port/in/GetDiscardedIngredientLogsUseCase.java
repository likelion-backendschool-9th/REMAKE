package com.remake.ingredient.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

import com.remake.ingredient.domain.DiscardedIngredientLog;

/** 영수증 모듈이 버린 식비·식재료를 만들 때 쓰는 조회. 냉장고 테이블을 직접 읽지 않고 이 UseCase를 부른다. */
public interface GetDiscardedIngredientLogsUseCase {

	/** 회원의 [from, to)(KST) 기간에 버린 기록. 최근에 버린 것이 위에 온다. */
	List<DiscardedIngredientLog> getDiscardedLogs(Long userId, LocalDateTime from, LocalDateTime to);
}
