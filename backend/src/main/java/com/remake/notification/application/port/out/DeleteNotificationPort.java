package com.remake.notification.application.port.out;

import java.time.LocalDateTime;

public interface DeleteNotificationPort {

	/** created_at이 before보다 이전인 알림을 지우고 건수를 돌려준다. */
	int deleteCreatedBefore(LocalDateTime before);
}
