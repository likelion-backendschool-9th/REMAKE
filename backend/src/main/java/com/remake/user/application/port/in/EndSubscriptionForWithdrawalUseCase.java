package com.remake.user.application.port.in;

/** 탈퇴 시 미종료 구독을 즉시 종료하고 E88을 남긴다(#258). */
public interface EndSubscriptionForWithdrawalUseCase {

	void endForWithdrawal(Long userId);
}
