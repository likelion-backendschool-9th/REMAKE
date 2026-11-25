package com.remake.receipt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

class ReceiptImageFormatTest {

	@Test
	void 주요_브랜드와_호환_브랜드에서_HEIC를_찾는다() {
		assertThat(ReceiptImageFormat.detect(box("heic", "mif1"))).isEqualTo(ReceiptImageFormat.HEIC);
		assertThat(ReceiptImageFormat.detect(box("mif1", "heix"))).isEqualTo(ReceiptImageFormat.HEIC);
	}

	@Test
	void AVIF와_잘린_컨테이너는_거절한다() {
		assertUnsupported(box("avif", "mif1"));
		byte[] truncated = box("mif1", "heic");
		ByteBuffer.wrap(truncated).putInt(24);
		assertUnsupported(truncated);
	}

	private static byte[] box(String brand, String compatible) {
		return ByteBuffer.allocate(20).putInt(20)
				.put(("ftyp" + brand).getBytes(StandardCharsets.US_ASCII)).putInt(0)
				.put(compatible.getBytes(StandardCharsets.US_ASCII)).array();
	}

	private static void assertUnsupported(byte[] image) {
		assertThatThrownBy(() -> ReceiptImageFormat.detect(image))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE));
	}
}
