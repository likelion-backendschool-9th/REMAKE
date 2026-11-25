package com.remake.receipt.application.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.NoSuchFileException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.receipt.application.port.in.ReceiptHistoryUseCase;
import com.remake.receipt.application.port.out.ReceiptHistoryPort;
import com.remake.receipt.application.port.out.ReceiptHistoryPort.StoredReceipt;
import com.remake.receipt.application.port.out.ReceiptImageDeletionPort;
import com.remake.receipt.application.port.out.ReceiptImageStoragePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ReceiptHistoryService implements ReceiptHistoryUseCase {

	private final ReceiptHistoryPort receipts;
	private final ReceiptImageStoragePort images;
	private final ReceiptImageDeletionPort pendingImages;
	private final ReceiptImageDeletionService imageDeletion;
	private final GetIngredientCategoriesUseCase categories;
	private final Clock clock;

	@Override
	public MonthlyReceipts list(Long userId, YearMonth month) {
		YearMonth current = YearMonth.now(clock);
		YearMonth earliest = current.minusMonths(5);
		YearMonth selected = month == null ? current : month;
		if (selected.isBefore(earliest) || selected.isAfter(current)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "최근 6개월의 영수증만 조회할 수 있어요.");
		}

		var byMonth = receipts.list(userId, earliest.atDay(1), current.plusMonths(1).atDay(1)).stream()
				.collect(Collectors.groupingBy(row -> YearMonth.from(row.purchasedOn())));
		List<Row> selectedRows = byMonth.getOrDefault(selected, List.of());
		long total = total(selectedRows);
		var historical = byMonth.entrySet().stream()
				.filter(entry -> !entry.getKey().equals(current))
				.map(entry -> total(entry.getValue()))
				.toList();
		Long difference = historical.isEmpty() ? null : total - BigDecimal
				.valueOf(historical.stream().mapToLong(Long::longValue).sum())
				.divide(BigDecimal.valueOf(historical.size()), 0, RoundingMode.HALF_UP).longValueExact();

		var weeks = selectedRows.stream().collect(Collectors.groupingBy(
				row -> row.purchasedOn().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
				LinkedHashMap::new, Collectors.toList()));
		return new MonthlyReceipts(selected, total, difference, weeks.entrySet().stream()
				.map(entry -> week(selected, entry.getKey(), entry.getValue())).toList());
	}

	private Week week(YearMonth month, LocalDate monday, List<Row> rows) {
		LocalDate start = monday.isBefore(month.atDay(1)) ? month.atDay(1) : monday;
		LocalDate sunday = monday.plusDays(6);
		LocalDate end = sunday.isAfter(month.atEndOfMonth()) ? month.atEndOfMonth() : sunday;
		LocalDate firstMonday = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		long number = java.time.temporal.ChronoUnit.WEEKS.between(firstMonday, monday) + 1;
		return new Week(number + "주차", start, end, total(rows), rows);
	}

	private long total(List<Row> rows) {
		return rows.stream().mapToLong(Row::totalAmount).sum();
	}

	@Override
	public Detail detail(Long userId, Long id) {
		StoredReceipt receipt = requireReceipt(userId, id);
		Map<Long, IngredientCategory> byId = categories.getIngredientCategories().stream()
				.collect(Collectors.toMap(IngredientCategory::id, category -> category));
		List<Item> items = receipts.items(id).stream().map(item -> {
			var category = byId.get(item.categoryId());
			return new Item(item.name(), category.code(), category.imageUrl(), item.storage(), item.amount(),
					item.unit(), item.price());
		}).toList();
		long total = items.stream().filter(item -> item.price() != null).mapToLong(Item::price).sum();
		return new Detail(new Row(id, receipt.storeCode(), receipt.storeName(), receipt.purchasedOn(),
				items.size(), total), items);
	}

	@Override
	public byte[] image(Long userId, Long id) {
		StoredReceipt receipt = requireReceipt(userId, id);
		try {
			return images.read(receipt.imageKey());
		} catch (NoSuchFileException failure) {
			throw new BusinessException(ErrorCode.RECEIPT_NOT_FOUND);
		} catch (IOException failure) {
			log.error("Failed to read receipt image {}", id, failure);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	@Transactional
	public void delete(Long userId, Long id) {
		StoredReceipt receipt = requireReceipt(userId, id);
		if (!receipts.delete(userId, id)) {
			throw new BusinessException(ErrorCode.RECEIPT_NOT_FOUND);
		}
		pendingImages.enqueue(receipt.imageKey());
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					imageDeletion.deleteAfterCommit(receipt.imageKey());
				} catch (RuntimeException failure) {
					log.warn("Receipt image cleanup remains queued for {}", id, failure);
				}
			}
		});
	}

	private StoredReceipt requireReceipt(Long userId, Long id) {
		return receipts.find(userId, id).orElseThrow(() -> new BusinessException(ErrorCode.RECEIPT_NOT_FOUND));
	}
}
