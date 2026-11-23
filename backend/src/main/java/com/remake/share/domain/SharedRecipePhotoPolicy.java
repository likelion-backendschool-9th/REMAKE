package com.remake.share.domain;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

/** #64 리뷰용 정책안. 확정된 기존 정책은 사진 1장 이상 필수이며 나머지는 제안값이다. */
public record SharedRecipePhotoPolicy(int maxPhotos, int maxFileBytes) {

	public static final int PROPOSED_MAX_PHOTOS = 5;
	public static final int PROPOSED_MAX_FILE_BYTES = 5 * 1024 * 1024;

	public SharedRecipePhotoPolicy {
		if (maxPhotos < 1 || maxFileBytes < 1 || maxFileBytes == Integer.MAX_VALUE) {
			throw new IllegalArgumentException("사진 제한은 양수이고 파일 제한은 Integer.MAX_VALUE보다 작아야 합니다.");
		}
	}

	public static SharedRecipePhotoPolicy proposed() {
		return new SharedRecipePhotoPolicy(PROPOSED_MAX_PHOTOS, PROPOSED_MAX_FILE_BYTES);
	}

	public void validateCount(int count) {
		if (count == 0) {
			throw new BusinessException(ErrorCode.PHOTO_REQUIRED);
		}
		if (count > maxPhotos) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
	}

	public void validateSize(long size) {
		if (size <= 0) {
			throw new BusinessException(ErrorCode.PHOTO_REQUIRED);
		}
		if (size > maxFileBytes) {
			throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
		}
	}
}
