package com.remake.share.application.port.out;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import com.remake.share.domain.SharedRecipe;

public interface ManageSharedRecipePort {
	Page<SharedRecipe> loadMine(Long userId, int page, int size);
	/** 삭제와 주간 확정의 공통 트랜잭션 잠금. 주 완료 기록/글 행/대상 조회/보상보다 먼저 획득한다. */
	void lockDeletionAndSettlement();
	Optional<SharedRecipe> lockOwned(Long userId, Long id);
	void markDeleted(Long id, LocalDateTime now);
}
