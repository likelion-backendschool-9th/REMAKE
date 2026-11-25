package com.remake.receipt.domain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

public enum ReceiptImageFormat {
	JPEG, PNG, HEIC;

	private static final byte[] JPEG_SIGNATURE = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
	private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
	private static final Set<String> HEIC_BRANDS = Set.of("heic", "heix", "hevc", "hevx", "heim", "heis", "hevm", "hevs");

	public static ReceiptImageFormat detect(byte[] image) {
		if (startsWith(image, JPEG_SIGNATURE)) {
			return JPEG;
		}
		if (startsWith(image, PNG_SIGNATURE)) {
			return PNG;
		}
		if (hasHeicBrand(image)) {
			return HEIC;
		}
		throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
	}

	private static boolean hasHeicBrand(byte[] image) {
		if (image.length < 16 || !fourCharacters(image, 4).equals("ftyp")) {
			return false;
		}
		int boxSize = ByteBuffer.wrap(image).getInt();
		if (boxSize < 16 || boxSize > image.length || boxSize % 4 != 0) {
			return false;
		}
		if (HEIC_BRANDS.contains(fourCharacters(image, 8))) {
			return true;
		}
		// ftyp의 버전 필드 뒤에 HEIC 호환 브랜드가 들어갈 수 있다.
		for (int offset = 16; offset < boxSize; offset += 4) {
			if (HEIC_BRANDS.contains(fourCharacters(image, offset))) {
				return true;
			}
		}
		return false;
	}

	private static String fourCharacters(byte[] image, int offset) {
		return new String(image, offset, 4, StandardCharsets.US_ASCII);
	}

	private static boolean startsWith(byte[] image, byte[] signature) {
		return image.length >= signature.length
				&& Arrays.equals(image, 0, signature.length, signature, 0, signature.length);
	}
}
