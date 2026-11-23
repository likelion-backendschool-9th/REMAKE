package com.remake.share.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.share.application.port.in.SharedRecipePhotoUpload;
import com.remake.share.application.port.in.StoreSharedRecipePhotosUseCase;
import com.remake.share.application.port.out.SharedRecipePhotoStoragePort;
import com.remake.share.domain.SharedRecipePhoto;
import com.remake.share.domain.SharedRecipePhotoFormat;
import com.remake.share.domain.SharedRecipePhotoPolicy;

import lombok.extern.slf4j.Slf4j;

/**
 * 저장소 결정 전의 핵심 로직. 운영 Bean은 실제 저장소 어댑터를 연결하는 변경에서 등록한다.
 * #86 게시 UseCase가 먼저 트랜잭션을 시작해야 하며, 이 클래스가 독립적으로 커밋하지 않는다.
 */
@Slf4j
public class SharedRecipePhotoService implements StoreSharedRecipePhotosUseCase {

	private final SharedRecipePhotoStoragePort storage;
	private final SharedRecipePhotoPolicy policy;

	public SharedRecipePhotoService(SharedRecipePhotoStoragePort storage, SharedRecipePhotoPolicy policy) {
		this.storage = storage;
		this.policy = policy;
	}

	@Override
	public List<SharedRecipePhoto> store(List<SharedRecipePhotoUpload> uploads) {
		List<PreparedPhoto> photos = prepare(uploads);
		if (!TransactionSynchronizationManager.isActualTransactionActive()
				|| !TransactionSynchronizationManager.isSynchronizationActive()
				|| TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}

		List<String> pendingKeys = new ArrayList<>();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					cleanUp(pendingKeys, null);
				}
			}
		});

		List<SharedRecipePhoto> stored = new ArrayList<>();
		try {
			for (PreparedPhoto photo : photos) {
				String key = "shared-recipes/" + UUID.randomUUID() + "." + photo.format().extension();
				// 저장소가 파일을 쓴 뒤 실패하는 경우에도 현재 파일까지 정리한다.
				pendingKeys.add(key);
				storage.store(key, photo.bytes(), photo.format().contentType());
				stored.add(new SharedRecipePhoto(key, stored.size()));
			}
			return List.copyOf(stored);
		} catch (RuntimeException failure) {
			BusinessException error = new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
			error.initCause(failure);
			cleanUp(pendingKeys, error);
			throw error;
		}
	}

	private List<PreparedPhoto> prepare(List<SharedRecipePhotoUpload> uploads) {
		policy.validateCount(uploads == null ? 0 : uploads.size());
		List<PreparedPhoto> prepared = new ArrayList<>();
		for (SharedRecipePhotoUpload upload : uploads) {
			if (upload == null || upload.content() == null) {
				throw new BusinessException(ErrorCode.PHOTO_REQUIRED);
			}
			policy.validateSize(upload.size());
			SharedRecipePhotoFormat format = SharedRecipePhotoFormat.fromContentType(upload.contentType());
			try (InputStream stream = upload.content().getInputStream()) {
				// 신고된 크기를 신뢰하지 않고 실제 스트림도 상한 + 1바이트까지만 읽는다.
				byte[] bytes = stream.readNBytes(policy.maxFileBytes() + 1);
				policy.validateSize(bytes.length);
				if (bytes.length != upload.size()) {
					throw new BusinessException(ErrorCode.INVALID_INPUT);
				}
				format.validateHeader(bytes);
				prepared.add(new PreparedPhoto(format, bytes));
			} catch (IOException failure) {
				BusinessException error = new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
				error.initCause(failure);
				throw error;
			}
		}
		return prepared;
	}

	private void cleanUp(List<String> pendingKeys, RuntimeException original) {
		for (String key : List.copyOf(pendingKeys)) {
			try {
				storage.delete(key);
				pendingKeys.remove(key);
			} catch (RuntimeException failure) {
				if (original != null) {
					original.addSuppressed(failure);
				}
				// 저장소 오류에는 URL·키·계정 정보가 포함될 수 있어 원문을 로그로 남기지 않는다.
				log.warn("공유 사진 정리 실패: 저장소 확인 및 잔여 파일 정리 필요");
			}
		}
	}

	private record PreparedPhoto(SharedRecipePhotoFormat format, byte[] bytes) {
	}
}
