package com.remake.user.application.port.out;

/**
 * 탈퇴 시 회원 소유 업무 데이터를 지운다. 레시피·공유글·이벤트·결제 기록은 지우지 않는다.
 * 구독 테이블이 생기기 전에는 구독 정리는 no-op이다.
 */
public interface PurgeUserOwnedDataPort {

	void purge(Long userId);
}
