package com.remake.share.domain;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

/** 확장자 대신 MIME과 파일 헤더를 대조한다. 전체 이미지 디코딩·변환은 수행하지 않는다. */
public enum SharedRecipePhotoFormat {

	JPEG("image/jpeg", "jpg"),
	PNG("image/png", "png"),
	WEBP("image/webp", "webp");

	private final String contentType;
	private final String extension;

	SharedRecipePhotoFormat(String contentType, String extension) {
		this.contentType = contentType;
		this.extension = extension;
	}

	public String contentType() {
		return contentType;
	}

	public String extension() {
		return extension;
	}

	public static SharedRecipePhotoFormat fromContentType(String contentType) {
		return Arrays.stream(values())
				.filter(format -> format.contentType.equalsIgnoreCase(contentType))
				.findFirst()
				.orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE));
	}

	public void validateHeader(byte[] bytes) {
		boolean matches = switch (this) {
			case JPEG -> startsWith(bytes, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});
			case PNG -> startsWith(bytes, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
			case WEBP -> bytes.length >= 12
					&& startsWith(bytes, "RIFF".getBytes(StandardCharsets.US_ASCII))
					&& Arrays.equals(bytes, 8, 12, "WEBP".getBytes(StandardCharsets.US_ASCII), 0, 4);
		};
		if (!matches) {
			throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
		}
	}

	private static boolean startsWith(byte[] bytes, byte[] prefix) {
		return bytes.length >= prefix.length && Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
	}
}
