package com.remake.share.application.port.out;

import java.util.Optional;

public interface LoadSharedRecipePhotoPort {

	/** 게시글 접근 권한 확인 후 DB에서 읽은 키만 전달한다. 반환한 스트림은 호출자가 닫는다. */
	Optional<SharedRecipePhotoContent> load(String imageKey);
}
