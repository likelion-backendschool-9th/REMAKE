package com.remake.share.application.port.out;

public interface SharedRecipePhotoStoragePort {

	/** 주어진 임의 키에 저장한다. 실패 시 일부 파일이 남아도 호출 측이 같은 키로 정리한다. */
	void store(String imageKey, byte[] content, String contentType);

	/** 키의 파일을 삭제한다. 이미 없는 파일은 성공으로 처리한다. */
	void delete(String imageKey);
}
