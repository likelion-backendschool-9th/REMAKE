package com.remake.share.application.port.in;

import com.remake.share.application.port.out.SharedRecipePhotoContent;

public interface GetSharedRecipePhotoUseCase {
	/** 로그인 후 공개 게시글만 조회한다. 응답 스트림은 웹 어댑터가 닫는다. */
	SharedRecipePhotoContent getPhoto(Long id);
}
