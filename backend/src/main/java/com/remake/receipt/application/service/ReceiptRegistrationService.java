package com.remake.receipt.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.remake.cart.application.port.in.CartDeductionCommand;
import com.remake.cart.application.port.in.DeductCartItemsUseCase;
import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.ingredient.application.port.in.RegisterIngredientCommand;
import com.remake.ingredient.application.port.in.RegisterIngredientUseCase;
import com.remake.ingredient.application.port.in.ResolveIngredientUnitUseCase;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase;
import com.remake.receipt.application.port.out.ReceiptImageStoragePort;
import com.remake.receipt.application.port.out.SaveReceiptPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
class ReceiptRegistrationService implements RegisterReceiptUseCase {

	private final SaveReceiptPort saveReceiptPort;
	private final ReceiptImageStoragePort imageStorage;
	private final GetIngredientCategoriesUseCase categories;
	private final RegisterIngredientUseCase ingredients;
	private final ResolveIngredientUnitUseCase ingredientUnits;
	private final DeductCartItemsUseCase deductCartItems;
	private final EventTracker eventTracker;
	private final Clock clock;

	@Override
	@Transactional
	public Result register(Long userId, InputStream image, Command command) {
		// 구매일이 정확히 6개월 전인 영수증은 등록할 수 있다.
		if (command.purchasedOn().isBefore(LocalDate.now(clock).minusMonths(6))) {
			throw new BusinessException(ErrorCode.RECEIPT_TOO_OLD);
		}
		var items = new ArrayList<Item>(command.items().size());
		for (int index = 0; index < command.items().size(); index++) {
			ItemRequest request = command.items().get(index);
			var unit = ingredientUnits.resolveUnit(request.name(), request.categoryCode());
			items.add(ReceiptQuantity.resolve(request, unit, index));
		}
		if (saveReceiptPort.lockOwnerAndCheckDuplicate(userId, command)) {
			throw new BusinessException(ErrorCode.RECEIPT_DUPLICATE);
		}
		String imageKey = saveImage(image);
		Long receiptId = saveReceiptPort.saveReceipt(userId, command.purchasedOn(), command.purchasedTime(),
				command.receiptNumber(), command.storeName(), command.storeCode(), imageKey);
		var registrations = new ArrayList<RegisterIngredientCommand>();
		var deductions = new ArrayList<CartDeductionCommand>();
		for (int index = 0; index < items.size(); index++) {
			Item item = items.get(index);
			for (CartDeduction deduction : command.items().get(index).cartDeductions()) {
				deductions.add(new CartDeductionCommand(deduction.cartItemId(), item.name(), deduction.amount(),
						item.unit()));
			}
			Long categoryId = categories.getIngredientCategoryByCode(item.categoryCode()).id();
			Long itemId = saveReceiptPort.saveItem(receiptId, categoryId, item);
			// 냉장고 재료는 저장된 품목 ID를 참조하므로 품목을 먼저 저장한다.
			registrations.add(new RegisterIngredientCommand(userId, item.name(), item.categoryCode(), item.storage(),
					item.expirationDate(), command.purchasedOn(), item.amount(),
					item.unit(), item.price(), itemId));
		}
		ingredients.registerIngredients(registrations);
		int cartRemovedCount = deductions.isEmpty() ? 0 : deductCartItems.deduct(userId, deductions);
		trackSavedReceipt(userId, receiptId, command, registrations.size(), cartRemovedCount);
		return new Result(receiptId, registrations.size(), cartRemovedCount);
	}

	private void trackSavedReceipt(Long userId, Long receiptId, Command command, int registeredCount, int cartRemovedCount) {
		BigInteger foodTotal = command.items().stream()
				.map(ItemRequest::price)
				.filter(Objects::nonNull)
				.map(BigInteger::valueOf)
				.reduce(BigInteger.ZERO, BigInteger::add);
		// EventTracker가 커밋 뒤 적재한다. OCR 편집 횟수와 화면 체류 시간은 최종 품목만으로 알 수 없다.
		eventTracker.track(EventName.RECEIPT_SAVED, Screen.RECEIPT_RESULT,
				Map.of("receipt_id", receiptId, "registered_count", registeredCount, "food_total_amount", foodTotal,
						"cart_removed_count", cartRemovedCount),
				EventTrackCommand.ofUser(userId));
	}

	private String saveImage(InputStream image) {
		String imageKey;
		try {
			imageKey = imageStorage.save(image);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Cannot store receipt image", e);
		}
		// DB 롤백은 파일을 되돌리지 않으므로 커밋에 실패하면 사진을 따로 정리한다.
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == STATUS_ROLLED_BACK) {
					try {
						imageStorage.delete(imageKey);
					}
					catch (IOException e) {
						log.warn("롤백된 영수증 사진 정리 실패 key={}", imageKey, e);
					}
				}
			}
		});
		return imageKey;
	}
}
