package com.remake.share;

import static com.remake.share.SharedRecipePhotoValidationTest.png;
import static com.remake.share.SharedRecipePhotoValidationTest.upload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.remake.global.config.UploadProperties;
import com.remake.global.error.BusinessException;
import com.remake.share.adapter.out.filestorage.FileSystemSharedRecipePhotoStorageAdapter;
import com.remake.share.application.port.in.StoreSharedRecipePhotoUseCase;
import com.remake.share.application.port.out.PhotoStorageKeyConflictException;
import com.remake.share.application.port.out.SharedRecipePhotoStoragePort;
import com.remake.share.application.service.SharedRecipePhotoService;
import com.remake.share.domain.SharedRecipePhotoPolicy;
import com.remake.support.IntegrationTestSupport;

import jakarta.servlet.MultipartConfigElement;

/** PostgreSQL의 실제 커밋·롤백과 실제 파일의 보존·삭제를 함께 검증한다. */
class SharedRecipePhotoTransactionTest extends IntegrationTestSupport {

	@Autowired
	private StoreSharedRecipePhotoUseCase service;

	@Autowired
	private FileSystemSharedRecipePhotoStorageAdapter storage;

	@Autowired
	private UploadProperties uploadProperties;

	@Autowired
	private SharedRecipePhotoPolicy policy;

	@Autowired
	private MultipartConfigElement multipart;

	@Test
	void 커밋되면_사진을_보존하고_임의_키를_반환한다() throws Exception {
		String key = transactionTemplate.execute(status -> service.store(upload("image/png", png())));
		assertThat(key).matches("shared-recipes/[0-9a-f-]{36}\\.png");
		try (var content = storage.load(key).orElseThrow()) {
			assertThat(content.stream().readAllBytes()).containsExactly(png());
		}
	}

	@Test
	void DB_제약_실패로_롤백되면_실제_파일을_삭제한다() {
		AtomicReference<String> key = new AtomicReference<>();
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			key.set(service.store(upload("image/png", png())));
			Long userId = createUser();
			jdbcTemplate.update("UPDATE users SET provider = NULL WHERE id = ?", userId);
		})).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThat(key.get()).isNotNull();
		assertThat(path(key.get())).doesNotExist();
	}

	@Test
	void rollbackOnly일때도_파일을_정리한다() {
		String key = transactionTemplate.execute(status -> {
			String saved = service.store(upload("image/png", png()));
			status.setRollbackOnly();
			return saved;
		});
		assertThat(path(key)).doesNotExist();
	}

	@Test
	void 파일을_쓴_뒤_실패해도_부분_파일을_정리한다() {
		FailingStorage failing = new FailingStorage(0);
		var failingService = new SharedRecipePhotoService(failing, policy);
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
				failingService.store(upload("image/png", png()))))
				.isInstanceOf(BusinessException.class).hasCause(failing.writeFailure);
		assertThat(path(failing.key)).doesNotExist();
	}

	@Test
	void 즉시_삭제_실패는_원인을_보존하고_롤백때_재시도한다() {
		FailingStorage failing = new FailingStorage(1);
		var failingService = new SharedRecipePhotoService(failing, policy);
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
				failingService.store(upload("image/png", png()))))
				.isInstanceOf(BusinessException.class).hasCause(failing.writeFailure)
				.satisfies(error -> assertThat(error.getSuppressed()).containsExactly(failing.deleteFailure));
		assertThat(failing.deletes).isEqualTo(2);
		assertThat(path(failing.key)).doesNotExist();
	}

	@Test
	void 계속되는_삭제_실패도_원래_오류를_덮지_않는다() {
		FailingStorage failing = new FailingStorage(2);
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
				new SharedRecipePhotoService(failing, policy).store(upload("image/png", png()))))
				.isInstanceOf(BusinessException.class).hasCause(failing.writeFailure);
		assertThat(failing.deletes).isEqualTo(2);
		// 영구 삭제 실패는 경고 후 운영 정리가 필요하다. 공유 테스트 정리가 파일을 지운다.
		assertThat(path(failing.key)).exists();
	}

	@Test
	void 기존_키_충돌은_기존_파일을_정리하지_않는다() {
		AtomicReference<String> oldKey = new AtomicReference<>();
		SharedRecipePhotoStoragePort conflict = new SharedRecipePhotoStoragePort() {
			@Override
			public void store(String key, byte[] bytes, String contentType) {
				oldKey.set(key);
				storage.store(key, bytes, contentType);
				// 다른 요청이 같은 키를 먼저 만들었다고 가정한다.
				throw new PhotoStorageKeyConflictException();
			}

			@Override
			public void delete(String key) {
				throw new AssertionError("기존 파일은 삭제하면 안 됩니다.");
			}
		};
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
				new SharedRecipePhotoService(conflict, policy).store(upload("image/png", png()))))
				.isInstanceOf(BusinessException.class).hasCauseInstanceOf(PhotoStorageKeyConflictException.class);
		assertThat(path(oldKey.get())).exists();
	}

	@Test
	void 같은_트랜잭션의_여러_호출도_롤백시_함께_정리한다() {
		transactionTemplate.executeWithoutResult(status -> {
			service.store(upload("image/png", png()));
			service.store(upload("image/png", png()));
			status.setRollbackOnly();
		});
		assertThat(Path.of(uploadProperties.rootDirectory()).resolve("shared-recipes")).isEmptyDirectory();
	}

	@Test
	void 읽기전용_트랜잭션에서는_저장하지_않는다() {
		TransactionTemplate readOnly = new TransactionTemplate(transactionTemplate.getTransactionManager());
		readOnly.setReadOnly(true);
		assertThatThrownBy(() -> readOnly.executeWithoutResult(status -> service.store(upload("image/png", png()))))
				.isInstanceOf(BusinessException.class);
		assertThat(Path.of(uploadProperties.rootDirectory()).resolve("shared-recipes")).isEmptyDirectory();
	}

	@Test
	void 공유사진_5MiB_경계는_허용하고_Spring은_영수증을_위해_10MB이다() {
		byte[] bytes = new byte[SharedRecipePhotoPolicy.MAX_FILE_BYTES];
		System.arraycopy(png(), 0, bytes, 0, png().length);
		String key = transactionTemplate.execute(status -> service.store(upload("image/png", bytes)));
		assertThat(path(key)).hasSize(bytes.length);
		assertThat(multipart.getMaxFileSize()).isEqualTo(10L * 1024 * 1024);
		assertThat(multipart.getMaxRequestSize()).isEqualTo(30L * 1024 * 1024);
	}

	private Path path(String key) {
		return Path.of(uploadProperties.rootDirectory()).resolve(key);
	}

	/** 실제 파일 저장 후 디스크 오류를 주입한다. 저장·삭제 자체는 실제 어댑터가 수행한다. */
	private class FailingStorage implements SharedRecipePhotoStoragePort {

		final RuntimeException writeFailure = new IllegalStateException("test write failure");
		final RuntimeException deleteFailure = new IllegalStateException("test delete failure");
		final int failures;
		String key;
		int deletes;

		FailingStorage(int failures) {
			this.failures = failures;
		}

		@Override
		public void store(String key, byte[] bytes, String contentType) {
			this.key = key;
			storage.store(key, bytes, contentType);
			throw writeFailure;
		}

		@Override
		public void delete(String key) {
			if (++deletes <= failures) {
				throw deleteFailure;
			}
			storage.delete(key);
		}
	}
}
