package com.remake.receipt.application.port.out;

import java.time.LocalDate;
import java.util.List;

import com.remake.receipt.application.port.in.ExpenseHistoryUseCase.Item;

public interface LoadExpenseHistoryPort {

	List<Entry> findBetween(Long userId, LocalDate from, LocalDate to);

	record Entry(LocalDate date, Item item) {
	}
}
