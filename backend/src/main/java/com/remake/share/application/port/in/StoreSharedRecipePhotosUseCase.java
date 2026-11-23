package com.remake.share.application.port.in;

import java.util.List;

import com.remake.share.domain.SharedRecipePhoto;

public interface StoreSharedRecipePhotosUseCase {

	/**
	 * 게시 UseCase의 활성 트랜잭션 안에서 호출한다. 반환된 키·순서를 같은 트랜잭션으로 DB에 저장한다.
	 * 게시가 롤백되면 업로드한 파일을 정리한다. 사진을 보관하는 별도 트랜잭션은 만들지 않는다.
	 */
	List<SharedRecipePhoto> store(List<SharedRecipePhotoUpload> uploads);
}
