package com.remake.user.application.port.in;

import com.remake.user.domain.UserPlan;

/** 구독 등 다른 기능이 회원 plan만 바꿀 때 쓴다. SaveUserPort를 직접 쓰지 않는다(#344). */
public interface ChangeUserPlanUseCase {

	void changePlan(Long userId, UserPlan plan);
}
