package com.remake.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.share.application.port.in.SharedRecipePhotoUpload;
import com.remake.share.application.port.out.SharedRecipePhotoStoragePort;
import com.remake.share.application.service.SharedRecipePhotoService;
import com.remake.share.domain.SharedRecipePhotoFormat;
import com.remake.share.domain.SharedRecipePhotoPolicy;

class SharedRecipePhotoValidationTest {

	private SharedRecipePhotoStoragePort storage;
	private SharedRecipePhotoService service;

	@BeforeEach
	void setUp() {
		storage = mock(SharedRecipePhotoStoragePort.class);
		service = new SharedRecipePhotoService(storage, SharedRecipePhotoPolicy.proposed());
	}

	@Test
	void 사진이_없으면_PHOTO_REQUIRED이다() {
		assertError(() -> service.store(null), ErrorCode.PHOTO_REQUIRED);
		assertError(() -> service.store(List.of()), ErrorCode.PHOTO_REQUIRED);
		assertError(() -> service.store(Collections.singletonList(null)), ErrorCode.PHOTO_REQUIRED);
		assertError(() -> service.store(List.of(new SharedRecipePhotoUpload("image/png", 1, null))),
				ErrorCode.PHOTO_REQUIRED);
		verifyNoInteractions(storage);
	}

	@Test
	void 빈_파일은_거절한다() {
		assertError(() -> service.store(List.of(upload("image/png", new byte[0]))), ErrorCode.PHOTO_REQUIRED);
		verifyNoInteractions(storage);
	}

	@Test
	void 최대_장수를_넘으면_스트림을_열지_않는다() {
		SharedRecipePhotoUpload photo = new SharedRecipePhotoUpload("image/png", 8, () -> {
			throw new AssertionError("장수 초과 시 스트림에 접근하면 안 됩니다.");
		});
		assertError(() -> service.store(Collections.nCopies(6, photo)), ErrorCode.INVALID_INPUT);
		verifyNoInteractions(storage);
	}

	@Test
	void 신고된_용량이_상한보다_크면_읽기_전에_거절한다() {
		SharedRecipePhotoUpload photo = new SharedRecipePhotoUpload("image/png",
				SharedRecipePhotoPolicy.PROPOSED_MAX_FILE_BYTES + 1L, () -> {
					throw new AssertionError("용량 초과 시 스트림에 접근하면 안 됩니다.");
				});
		assertError(() -> service.store(List.of(photo)), ErrorCode.FILE_TOO_LARGE);
		verifyNoInteractions(storage);
	}

	@Test
	void 실제_스트림이_상한을_넘으면_거절하고_닫는다() {
		// 작은 정책값으로 경계와 제한된 읽기를 검증한다.
		service = new SharedRecipePhotoService(storage, new SharedRecipePhotoPolicy(5, 12));
		boolean[] closed = {false};
		ByteArrayInputStream stream = new ByteArrayInputStream(new byte[100]) {
			@Override
			public void close() {
				closed[0] = true;
			}
		};
		assertError(() -> service.store(List.of(new SharedRecipePhotoUpload("image/png", 12, () -> stream))),
				ErrorCode.FILE_TOO_LARGE);
		assertThat(stream.available()).isEqualTo(87);
		assertThat(closed[0]).isTrue();
		verifyNoInteractions(storage);
	}

	@Test
	void 실제_크기가_신고된_크기와_다르면_거절한다() {
		assertError(() -> service.store(List.of(new SharedRecipePhotoUpload("image/png", 9,
				() -> new ByteArrayInputStream(png())))), ErrorCode.INVALID_INPUT);
		verifyNoInteractions(storage);
	}

	@Test
	void 뒤의_사진이_잘못됐어도_앞의_사진을_저장하지_않는다() {
		assertError(() -> service.store(List.of(upload("image/png", png()), upload("image/png", new byte[] {1}))),
				ErrorCode.UNSUPPORTED_FILE_TYPE);
		verifyNoInteractions(storage);
	}

	@ParameterizedTest
	@MethodSource("unsupportedTypes")
	void 허용하지_않은_MIME을_거절한다(String contentType) {
		assertError(() -> service.store(List.of(upload(contentType, png()))), ErrorCode.UNSUPPORTED_FILE_TYPE);
		verifyNoInteractions(storage);
	}

	static Stream<Arguments> unsupportedTypes() {
		return Stream.of(Arguments.of((String) null), Arguments.of("image/heic"), Arguments.of("image/gif"),
				Arguments.of("image/svg+xml"), Arguments.of("application/octet-stream"));
	}

	@ParameterizedTest
	@MethodSource("supportedHeaders")
	void 허용_형식은_MIME과_헤더가_일치해야_한다(String contentType, byte[] bytes) {
		SharedRecipePhotoFormat format = SharedRecipePhotoFormat.fromContentType(contentType);
		format.validateHeader(bytes);
		assertThat(format.contentType()).isEqualTo(contentType.toLowerCase(java.util.Locale.ROOT));
	}

	static Stream<Arguments> supportedHeaders() {
		return Stream.of(Arguments.of("image/png", png()),
				Arguments.of("IMAGE/JPEG", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff}),
				Arguments.of("image/webp", new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}));
	}

	@Test
	void PNG를_JPEG라고_보내거나_잘린_헤더를_보내면_거절한다() {
		assertError(() -> service.store(List.of(upload("image/jpeg", png()))), ErrorCode.UNSUPPORTED_FILE_TYPE);
		assertError(() -> SharedRecipePhotoFormat.PNG.validateHeader(new byte[] {(byte) 0x89}),
				ErrorCode.UNSUPPORTED_FILE_TYPE);
		assertError(() -> SharedRecipePhotoFormat.WEBP.validateHeader("RIFF1234FAKE".getBytes()),
				ErrorCode.UNSUPPORTED_FILE_TYPE);
	}

	@Test
	void 입력_스트림_실패는_저장소_호출_없이_처리한다() {
		IOException original = new IOException("test read failure");
		assertThatThrownBy(() -> service.store(List.of(new SharedRecipePhotoUpload("image/png", 8, () -> {
			throw original;
		}))))
				.isInstanceOf(BusinessException.class)
				.hasCause(original)
				.extracting(error -> ((BusinessException) error).getErrorCode())
				.isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
		verifyNoInteractions(storage);
	}

	@Test
	void 게시_트랜잭션_밖에서는_파일을_저장하지_않는다() {
		assertError(() -> service.store(List.of(upload("image/png", png()))), ErrorCode.INTERNAL_ERROR);
		verifyNoInteractions(storage);
	}

	@Test
	void 정책_경계값은_포함하고_잘못된_설정은_거절한다() {
		SharedRecipePhotoPolicy policy = SharedRecipePhotoPolicy.proposed();
		policy.validateCount(1);
		policy.validateCount(5);
		policy.validateSize(5 * 1024 * 1024);
		assertThatThrownBy(() -> new SharedRecipePhotoPolicy(0, 1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new SharedRecipePhotoPolicy(1, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new SharedRecipePhotoPolicy(1, Integer.MAX_VALUE))
				.isInstanceOf(IllegalArgumentException.class);
	}

	static SharedRecipePhotoUpload upload(String contentType, byte[] bytes) {
		return new SharedRecipePhotoUpload(contentType, bytes.length, () -> new ByteArrayInputStream(bytes));
	}

	static byte[] png() {
		return new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
	}

	private void assertError(Runnable call, ErrorCode code) {
		assertThatThrownBy(call::run).isInstanceOf(BusinessException.class)
				.extracting(error -> ((BusinessException) error).getErrorCode()).isEqualTo(code);
	}
}
