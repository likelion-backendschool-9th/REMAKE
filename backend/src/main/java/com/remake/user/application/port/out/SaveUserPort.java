package com.remake.user.application.port.out;

import com.remake.user.domain.User;

public interface SaveUserPort {

	User save(User user);
}
