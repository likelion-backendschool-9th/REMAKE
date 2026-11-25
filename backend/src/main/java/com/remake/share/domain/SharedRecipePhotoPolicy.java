package com.remake.share.domain;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

/** 최신 공유 명세의 사진 1장에 적용하는 용량 정책. 허용 형식은 SharedRecipePhotoFormat에서 관리한다. */
public record SharedRecipePhotoPolicy(int maxFileBytes) {

	public static final int MAX_FILE_BYTES = 5 * 1024 * 1024;

	public SharedRecipePhotoPolicy {
		if (maxFileBytes < 1 || maxFileBytes == Integer.MAX_VALUE) {
			throw new IllegalArgumentException("사진 제한은 양수이고 파일 제한은 Integer.MAX_VALUE보다 작아야 합니다.");
		}
	}

	public void validateSize(long size) {
		if (size <= 0) {
			throw new BusinessException(ErrorCode.PHOTO_REQUIRED);
		}
		if (size > maxFileBytes) {
			throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "파일이 너무 커요 (최대 5MiB)");
		}
	}
}
