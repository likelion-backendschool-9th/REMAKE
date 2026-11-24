package com.remake.share.application.port.in;

public interface StoreSharedRecipePhotoUseCase {

	/**
	 * 게시 UseCase의 활성 쓰기 트랜잭션 안에서 호출하고, 반환 키를 shared_recipes.image_key에 저장한다.
	 * 게시가 롤백되면 파일도 정리한다. 별도 트랜잭션은 만들지 않는다.
	 */
	String store(SharedRecipePhotoUpload upload);
}
