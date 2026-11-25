package com.remake.receipt.application.port.in;

/**
 * 회원 탈퇴 때 부른다. 영수증·품목을 지우고 사진은 큐 + afterCommit으로 지운다.
 */
public interface DeleteReceiptsForUserUseCase {

	void deleteReceiptsForUser(Long userId);
}
