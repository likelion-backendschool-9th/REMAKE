package com.remake.share;

import static com.remake.share.SharedRecipePhotoValidationTest.png;
import static com.remake.share.SharedRecipePhotoValidationTest.upload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.share.application.port.out.SharedRecipePhotoStoragePort;
import com.remake.share.application.service.SharedRecipePhotoService;
import com.remake.share.domain.SharedRecipePhoto;
import com.remake.share.domain.SharedRecipePhotoPolicy;
import com.remake.support.IntegrationTestSupport;

/** 실제 PostgreSQL 트랜잭션으로 커밋·롤백 콜백을 확인한다. 외부 저장소만 가짜로 둔다. */
class SharedRecipePhotoTransactionTest extends IntegrationTestSupport {

	private SharedRecipePhotoStoragePort storage;
	private SharedRecipePhotoService service;

	@BeforeEach
	void setUp() {
		storage = mock(SharedRecipePhotoStoragePort.class);
		service = new SharedRecipePhotoService(storage, SharedRecipePhotoPolicy.proposed());
	}

	@Test
	void 커밋되면_사진을_보존하고_입력_순서와_임의_키를_반환한다() {
		List<SharedRecipePhoto> result = transactionTemplate.execute(status -> service.store(
				List.of(upload("image/png", png()), upload("image/png", png()))));
		assertThat(result).extracting(SharedRecipePhoto::sortOrder).containsExactly(0, 1);
		assertThat(result).extracting(SharedRecipePhoto::imageKey).doesNotHaveDuplicates()
				.allMatch(key -> key.matches("shared-recipes/[0-9a-f-]{36}\\.png"));
		for (SharedRecipePhoto photo : result) {
			verify(storage).store(photo.imageKey(), png(), "image/png");
		}
		verify(storage, times(0)).delete(anyString());
	}

	@Test
	void 게시_DB_실패로_롤백되면_모든_파일을_삭제한다() {
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			service.store(List.of(upload("image/png", png()), upload("image/png", png())));
			// 일부 사용자 저장 후 실제 DB 제약 위반으로 커밋할 수 없는 상황을 만든다.
			Long userId = createUser();
			jdbcTemplate.update("UPDATE users SET provider = NULL WHERE id = ?", userId);
		})).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
		verify(storage, times(2)).store(stored.capture(), any(byte[].class), eq("image/png"));
		for (String key : stored.getAllValues()) {
			verify(storage).delete(key);
		}
	}

	@Test
	void rollbackOnly로_끝나도_파일을_정리한다() {
		transactionTemplate.executeWithoutResult(status -> {
			service.store(List.of(upload("image/png", png())));
			status.setRollbackOnly();
		});
		verify(storage).delete(anyString());
	}

	@Test
	void 두번째_업로드_실패시_실패한_키까지_정리하고_원인을_보존한다() {
		RuntimeException failure = new IllegalStateException("storage failure");
		AtomicInteger calls = new AtomicInteger();
		List<String> attempted = new ArrayList<>();
		doAnswer(invocation -> {
			attempted.add(invocation.getArgument(0));
			if (calls.incrementAndGet() == 2) {
				throw failure;
			}
			return null;
		}).when(storage).store(anyString(), any(byte[].class), anyString());
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> service.store(
				List.of(upload("image/png", png()), upload("image/png", png()), upload("image/png", png())))))
				.isInstanceOf(BusinessException.class).hasCause(failure)
				.extracting(error -> ((BusinessException) error).getErrorCode())
				.isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
		assertThat(attempted).hasSize(2);
		for (String key : attempted) {
			verify(storage).delete(key);
		}
	}

	@Test
	void 정리_실패가_다른_파일_정리와_원래_오류를_덮지_않고_롤백때_재시도한다() {
		RuntimeException failure = new IllegalStateException("upload failed");
		RuntimeException cleanupFailure = new IllegalStateException("delete failed");
		AtomicInteger calls = new AtomicInteger();
		doAnswer(invocation -> {
			if (calls.incrementAndGet() == 2) {
				throw failure;
			}
			return null;
		}).when(storage).store(anyString(), any(byte[].class), anyString());
		doThrow(cleanupFailure).doNothing().when(storage).delete(anyString());
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> service.store(
				List.of(upload("image/png", png()), upload("image/png", png())))))
				.isInstanceOf(BusinessException.class).hasCause(failure)
				.satisfies(error -> assertThat(error.getSuppressed()).containsExactly(cleanupFailure));
		// 첫 파일 정리 실패 → 두 번째 파일 정리 → 롤백 때 첫 파일 재시도.
		verify(storage, times(3)).delete(anyString());
	}

	@Test
	void 같은_트랜잭션의_여러_호출도_롤백시_함께_정리한다() {
		transactionTemplate.executeWithoutResult(status -> {
			service.store(List.of(upload("image/png", png())));
			service.store(List.of(upload("image/png", png())));
			status.setRollbackOnly();
		});
		verify(storage, times(2)).delete(anyString());
	}

	@Test
	void 읽기전용_트랜잭션에서는_저장하지_않는다() {
		TransactionTemplate readOnlyTransaction = new TransactionTemplate(transactionTemplate.getTransactionManager());
		readOnlyTransaction.setReadOnly(true);
		assertThatThrownBy(() -> readOnlyTransaction.executeWithoutResult(status -> service.store(
				List.of(upload("image/png", png()))))).isInstanceOf(BusinessException.class);
		verifyNoInteractions(storage);
	}
}
