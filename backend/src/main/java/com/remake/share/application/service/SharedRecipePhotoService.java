package com.remake.share.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.share.application.port.in.SharedRecipePhotoUpload;
import com.remake.share.application.port.in.StoreSharedRecipePhotoUseCase;
import com.remake.share.application.port.out.PhotoStorageKeyConflictException;
import com.remake.share.application.port.out.SharedRecipePhotoStoragePort;
import com.remake.share.domain.SharedRecipePhotoFormat;
import com.remake.share.domain.SharedRecipePhotoPolicy;

import lombok.extern.slf4j.Slf4j;

/** 게시 UseCase의 쓰기 트랜잭션에 파일 정리를 연결한다. 독립적으로 트랜잭션을 커밋하지 않는다. */
@Slf4j
@Service
public class SharedRecipePhotoService implements StoreSharedRecipePhotoUseCase {

	private final SharedRecipePhotoStoragePort storage;
	private final SharedRecipePhotoPolicy policy;

	public SharedRecipePhotoService(SharedRecipePhotoStoragePort storage, SharedRecipePhotoPolicy policy) {
		this.storage = storage;
		this.policy = policy;
	}

	@Override
	public String store(SharedRecipePhotoUpload upload) {
		if (upload == null || upload.content() == null) {
			throw new BusinessException(ErrorCode.PHOTO_REQUIRED);
		}
		policy.validateSize(upload.size());
		SharedRecipePhotoFormat format = SharedRecipePhotoFormat.fromContentType(upload.contentType());
		byte[] bytes = read(upload);
		format.validateHeader(bytes);
		if (!TransactionSynchronizationManager.isActualTransactionActive()
				|| !TransactionSynchronizationManager.isSynchronizationActive()
				|| TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}

		String key = "shared-recipes/" + UUID.randomUUID() + "." + format.extension();
		PhotoCleanup cleanup = new PhotoCleanup(key);
		TransactionSynchronizationManager.registerSynchronization(cleanup);
		try {
			storage.store(key, bytes, format.contentType());
			return key;
		} catch (PhotoStorageKeyConflictException failure) {
			// 이전부터 존재하던 파일은 이번 요청의 정리 대상이 아니다.
			cleanup.pending = false;
			throw unavailable(failure);
		} catch (RuntimeException failure) {
			BusinessException error = unavailable(failure);
			cleanup.remove(error);
			throw error;
		}
	}

	private byte[] read(SharedRecipePhotoUpload upload) {
		try (InputStream stream = upload.content().getInputStream()) {
			// 신고된 크기를 신뢰하지 않고 실제 스트림도 상한 + 1바이트까지만 읽는다.
			byte[] bytes = stream.readNBytes(policy.maxFileBytes() + 1);
			policy.validateSize(bytes.length);
			if (bytes.length != upload.size()) {
				throw new BusinessException(ErrorCode.INVALID_INPUT);
			}
			return bytes;
		} catch (IOException failure) {
			throw unavailable(failure);
		}
	}

	private BusinessException unavailable(Exception cause) {
		BusinessException error = new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
		error.initCause(cause);
		return error;
	}

	private class PhotoCleanup implements TransactionSynchronization {

		private final String key;
		private boolean pending = true;

		PhotoCleanup(String key) {
			this.key = key;
		}

		@Override
		public void afterCompletion(int status) {
			if (status == STATUS_ROLLED_BACK) {
				remove(null);
			} else if (status == STATUS_UNKNOWN && pending) {
				// 실제 커밋됐을 수 있으므로 파일을 보존하고 DB와 저장소 대조를 요청한다.
				log.warn("공유 사진 트랜잭션 결과 불명: 파일 보존, DB 및 저장소 확인 필요");
			}
		}

		void remove(RuntimeException original) {
			if (!pending) {
				return;
			}
			try {
				storage.delete(key);
				pending = false;
			} catch (RuntimeException failure) {
				if (original != null) {
					original.addSuppressed(failure);
				}
				log.warn("공유 사진 정리 실패: 저장소 확인 및 잔여 파일 정리 필요");
			}
		}
	}
}
