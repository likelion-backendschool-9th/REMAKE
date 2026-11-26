package com.remake.ingredient.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.ingredient.application.port.in.GetDiscardedIngredientLogsUseCase;
import com.remake.ingredient.application.port.out.LoadIngredientLogPort;
import com.remake.ingredient.domain.DiscardedIngredientLog;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class DiscardedIngredientLogService implements GetDiscardedIngredientLogsUseCase {

	private final LoadIngredientLogPort loadIngredientLogPort;

	@Override
	public List<DiscardedIngredientLog> getDiscardedLogs(Long userId, LocalDateTime from, LocalDateTime to) {
		return loadIngredientLogPort.findDiscardedBetween(userId, from, to);
	}
}
