package com.remake.quota.application.port.out;

import java.time.LocalDateTime;

import com.remake.quota.domain.UsageType;

/** usage_logs 읽기·쓰기. 성공한 생성만 한 행씩 남긴다. */
public interface UsageLogPort {

	/** from(포함) 이후에 남긴 type 행 수 */
	int countSince(Long userId, UsageType type, LocalDateTime from);

	/** from(포함) 이후에 기본 횟수로 남긴 type 행 수(grant_id IS NULL). 보상 횟수로 쓴 행은 세지 않는다. */
	int countBaseSince(Long userId, UsageType type, LocalDateTime from);

	/** 성공한 생성 한 건을 남긴다. 시각은 JPA 감사(Clock)가 넣는다. 보상 횟수를 썼으면 grantId, 아니면 null */
	void save(Long userId, UsageType type, Long grantId);

	/** 현재 트랜잭션이 끝날 때까지 회원 단위로 잠근다. 같은 회원의 차감이 동시에 실행되지 않게 한다. */
	void lockUser(Long userId);
}
