package com.remake.quota.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.quota.application.port.in.DeleteUsageLogsForUserUseCase;
import com.remake.quota.application.port.out.DeleteUsageLogsForUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class DeleteUsageLogsForUserService implements DeleteUsageLogsForUserUseCase {

	private final DeleteUsageLogsForUserPort deleteUsageLogsForUserPort;

	@Override
	@Transactional
	public void deleteUsageLogsForUser(Long userId) {
		deleteUsageLogsForUserPort.deleteAllByUserId(userId);
	}
}
