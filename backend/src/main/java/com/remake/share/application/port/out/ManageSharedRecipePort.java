package com.remake.share.application.port.out;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import com.remake.share.domain.SharedRecipe;

public interface ManageSharedRecipePort {
	Page<SharedRecipe> loadMine(Long userId, int page, int size);
	/** #297도 확정 대상 조회 전에 같은 잠금을 획득해야 한다. 삭제/확정 → 글 행 → 보상 순서. */
	void lockDeletionAndSettlement();
	Optional<SharedRecipe> lockOwned(Long userId, Long id);
	void markDeleted(Long id, LocalDateTime now);
}
