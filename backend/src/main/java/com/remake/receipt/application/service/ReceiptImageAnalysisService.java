package com.remake.receipt.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetIngredientCatalogUseCase;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.ingredient.application.port.in.ResolveIngredientUnitUseCase;
import com.remake.receipt.application.port.in.AnalyzeReceiptImageUseCase;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.Volume;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.VolumeScope;
import com.remake.receipt.application.port.out.FindDuplicateReceiptPort;
import com.remake.receipt.application.port.out.ReadReceiptTextPort;
import com.remake.receipt.domain.OcrReceiptHeader;
import com.remake.receipt.domain.OcrReceiptItemParser;
import com.remake.receipt.domain.OcrReceiptItemParser.Item;
import com.remake.receipt.domain.ReceiptIngredientAliases;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class ReceiptImageAnalysisService implements AnalyzeReceiptImageUseCase {

	private static final Duration OCR_TIMEOUT = Duration.ofSeconds(30);
	private final ReadReceiptTextPort receiptText;
	private final EventTracker eventTracker;
	private final Clock clock;
	private final GetIngredientCatalogUseCase catalog;
	private final GetIngredientCategoriesUseCase categories;
	private final ResolveIngredientUnitUseCase ingredientUnits;
	private final FindDuplicateReceiptPort duplicateReceipts;
	private final OcrReceiptItemParser itemParser = new OcrReceiptItemParser();

	@Override
	public Result analyze(Long userId, byte[] image) {
		long started = System.nanoTime();
		Scan scan;
		try {
			scan = readReceipt(userId, image);
		}
		catch (BusinessException e) {
			eventTracker.track(EventName.RECEIPT_SCAN_FAILED, Screen.RECEIPT_SCAN,
					Map.of("error_code", e.getErrorCode().name(), "duration_ms", elapsedMillis(started)),
					EventTrackCommand.ofUser(userId));
			throw e;
		}
		eventTracker.track(EventName.RECEIPT_SCAN_SUCCEEDED, Screen.RECEIPT_SCAN,
				Map.of("item_count", scan.candidateCount(), "food_item_count", scan.result().items().size(),
						"duration_ms", elapsedMillis(started)),
				EventTrackCommand.ofUser(userId));
		return scan.result();
	}

	private Scan readReceipt(Long userId, byte[] image) {
		String text = receiptText.read(image, OCR_TIMEOUT);
		var header = OcrReceiptHeader.parse(text);
		checkPurchasedOn(header);
		var candidates = itemParser.parse(text);
		var foodItems = matchFood(candidates, header);
		if (candidates.isEmpty()) {
			throw new BusinessException(ErrorCode.RECEIPT_NOT_RECOGNIZED);
		}
		if (foodItems.isEmpty()) {
			throw new BusinessException(ErrorCode.RECEIPT_NO_FOOD_ITEMS);
		}
		var key = new FindDuplicateReceiptPort.Key(header.storeName(), header.purchasedOn(), header.purchasedTime(),
				header.receiptNumber(), foodItems.stream().map(ScannedItem::name).toList());
		if (key.hasRequiredFields() && duplicateReceipts.exists(userId, key)) {
			throw new BusinessException(ErrorCode.RECEIPT_DUPLICATE);
		}
		return new Scan(new Result(header, List.copyOf(foodItems)), candidates.size());
	}

	private void checkPurchasedOn(OcrReceiptHeader header) {
		if (header.purchasedOn() != null && header.purchasedOn().isBefore(LocalDate.now(clock).minusMonths(6))) {
			throw new BusinessException(ErrorCode.RECEIPT_TOO_OLD);
		}
	}

	private List<ScannedItem> matchFood(List<Item> items, OcrReceiptHeader header) {
		var foodItems = new ArrayList<ScannedItem>();
		for (Item item : items) {
			var match = catalog.findCatalogItemByName(item.name());
			String baseName = item.nameWithoutPackageVolume();
			if (match.isEmpty() && !baseName.equals(item.name())) {
				match = catalog.findCatalogItemByName(baseName);
			}
			String registrationName = item.name();
			if (match.isEmpty()) {
				String alias = ReceiptIngredientAliases.resolve(baseName);
				if (alias != null) {
					match = catalog.findCatalogItemByName(alias);
					registrationName = alias;
				}
			}
			if (match.isEmpty()) {
				continue;
			}
			var entry = match.get();
			var category = categories.getIngredientCategory(entry.categoryId());
			var expirationDate = header.purchasedOn() == null ? null
					: header.purchasedOn().plusDays(category.defaultShelfLifeDays());
			var unit = ingredientUnits.resolveUnit(registrationName, category.code());
			var parsedVolume = item.volume();
			var volume = parsedVolume == null ? null : new Volume(parsedVolume.amount(), parsedVolume.unit(),
					VolumeScope.valueOf(parsedVolume.scope().name()));
			var quantity = ReceiptQuantity.calculate(unit, item.purchaseQuantity(), volume);
			var amount = ReceiptQuantity.isValid(quantity.amount()) ? quantity.amount().stripTrailingZeros() : null;
			foodItems.add(new ScannedItem(item, registrationName, category.code(), category.defaultStorage(),
					expirationDate, amount, quantity.unit()));
		}
		return foodItems;
	}

	private record Scan(Result result, int candidateCount) {
	}

	private static long elapsedMillis(long started) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
	}
}
