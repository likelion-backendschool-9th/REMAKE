package com.remake.receipt.adapter.in.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.config.OpenApiConfig;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserId;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.receipt.application.port.in.AnalyzeReceiptImageUseCase;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.receipt.application.port.in.AnalyzeReceiptImageUseCase.ScannedItem;
import com.remake.receipt.domain.OcrReceiptItemParser.Volume;
import com.remake.receipt.domain.ReceiptImageFormat;
import com.remake.receipt.domain.ReceiptStoreBrand;
import com.remake.receipt.domain.ReceiptStoreCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "영수증")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequiredArgsConstructor
class ReceiptScanController {

	private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

	private final AnalyzeReceiptImageUseCase analyzeReceiptImage;
	private final EventTracker eventTracker;

	@Operation(summary = "영수증 OCR 스캔")
	@PostMapping(path = "/api/v1/user/receipts/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ScanResponse scan(@LoginUserId Long userId, @RequestPart("image") MultipartFile image) throws IOException {
		long started = System.nanoTime();
		byte[] bytes;
		try {
			bytes = validateImage(image);
		}
		catch (BusinessException e) {
			eventTracker.track(EventName.RECEIPT_SCAN_FAILED, Screen.RECEIPT_SCAN,
					Map.of("error_code", e.getErrorCode().name(), "duration_ms",
							TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)),
					EventTrackCommand.ofUser(userId));
			throw e;
		}

		// OCR 단계의 이벤트는 분석 서비스가 남긴다.
		var result = analyzeReceiptImage.analyze(userId, bytes);
		var items = result.items().stream()
				.map(ScanItem::from)
				.toList();
		var header = result.header();
		return new ScanResponse(items, header.purchasedOn(), header.storeName(), header.purchasedTime(),
				header.receiptNumber(), ReceiptStoreBrand.match(header.storeName()));
	}

	private static byte[] validateImage(MultipartFile image) throws IOException {
		if (image.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "영수증 사진을 첨부해 주세요.");
		}
		if (image.getSize() > MAX_IMAGE_BYTES) {
			throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
		}
		byte[] bytes = image.getBytes();
		// 파일 이름이나 Content-Type 대신 실제 이미지 헤더를 확인한다.
		ReceiptImageFormat.detect(bytes);
		return bytes;
	}

	record ScanResponse(List<ScanItem> items, @Schema(nullable = true) LocalDate purchasedOn,
			@Schema(nullable = true) String storeName, @Schema(nullable = true) LocalTime purchasedTime,
			@Schema(nullable = true) String receiptNumber, @Schema(nullable = true) ReceiptStoreCode storeCode) {
	}

	record ScanItem(String name, @Schema(nullable = true) BigDecimal amount, IngredientUnit unit, long price,
			@Schema(nullable = true) BigDecimal purchaseQuantity, @Schema(nullable = true) Volume volume,
			IngredientCategoryCode categoryCode, StorageLocation storage, LocalDate expirationDate,
			@Schema(nullable = true) String sourceName) {

		static ScanItem from(ScannedItem item) {
			var source = item.source();
			return new ScanItem(item.name(), item.amount(), item.unit(), source.totalPrice(),
					source.purchaseQuantity(), source.volume(), item.categoryCode(), item.storage(), item.expirationDate(),
					item.name().equals(source.name()) ? null : source.name());
		}
	}
}
