package com.remake.user.application.port.in;

import java.util.Optional;

import com.remake.user.domain.User;

/** 회원 단건 조회. 다른 기능은 LoadUserPort가 아니라 이 UseCase만 쓴다. */
public interface GetUserUseCase {

	Optional<User> getUser(Long id);
}
