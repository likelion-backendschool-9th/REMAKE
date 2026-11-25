package com.remake.receipt.application.port.in;

/** 매일 새벽. 구매일 기준 6개월을 넘긴 영수증·품목·이미지를 정리한다. */
public interface ExpireOldReceiptsUseCase {

	/** @return 삭제한 영수증 수 */
	int expireOldReceipts();
}
