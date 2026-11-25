package com.remake.notification.application.port.in;

/** 회원 탈퇴 때 부른다. 그 회원의 알림을 모두 지운다. 탈퇴 처리와 같은 트랜잭션에서 쓴다. */
public interface DeleteUserNotificationsUseCase {

	void deleteUserNotifications(Long userId);
}
