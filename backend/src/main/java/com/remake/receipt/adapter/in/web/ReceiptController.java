package com.remake.receipt.adapter.in.web;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserId;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase;
import com.remake.receipt.application.port.in.ReceiptHistoryUseCase;
import com.remake.receipt.domain.ReceiptStoreCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "영수증")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping(ReceiptController.ROOT)
@RequiredArgsConstructor
class ReceiptController {

	static final String ROOT = "/api/v1/user/receipts";
	private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/heic");
	private final RegisterReceiptUseCase registerReceiptUseCase;
	private final ReceiptHistoryUseCase history;

	@Operation(summary = "영수증 등록")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	ReceiptCreateResponse register(@LoginUserId Long userId, @RequestPart("image") MultipartFile image,
			@Valid @RequestPart("payload") ReceiptCreateRequest request) throws IOException {
		if (image.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "영수증 사진을 첨부해 주세요.");
		}
		String contentType = image.getContentType();
		if (contentType == null || !IMAGE_TYPES.contains(contentType)) {
			throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE, "JPG, PNG 또는 HEIC 사진을 첨부해 주세요.");
		}
		try (var input = image.getInputStream()) {
			var result = registerReceiptUseCase.register(userId, input, request.toCommand());
			return new ReceiptCreateResponse(result.receiptId(), result.registeredCount(), result.cartRemovedCount());
		}
	}

	@Operation(summary = "영수증 목록")
	@GetMapping
	ReceiptHistoryUseCase.MonthlyReceipts list(@LoginUserId Long userId, @RequestParam(required = false) YearMonth month) {
		return history.list(userId, month);
	}

	@Operation(summary = "영수증 상세")
	@GetMapping("/{id}")
	ReceiptDetailResponse detail(@LoginUserId Long userId, @PathVariable Long id) {
		var detail = history.detail(userId, id);
		var receipt = detail.receipt();
		return new ReceiptDetailResponse(receipt.id(), receipt.storeCode(), receipt.storeName(), receipt.purchasedOn(),
				ROOT + "/" + id + "/image", detail.items(), receipt.totalAmount());
	}

	@Operation(summary = "영수증 사진 조회")
	@GetMapping("/{id}/image")
	ResponseEntity<byte[]> image(@LoginUserId Long userId, @PathVariable Long id) throws IOException {
		byte[] content = history.image(userId, id);
		String detected = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(content));
		MediaType mediaType = detected != null && IMAGE_TYPES.contains(detected)
				? MediaType.parseMediaType(detected) : MediaType.APPLICATION_OCTET_STREAM;
		if (content.length >= 12 && content[4] == 'f' && content[5] == 't' && content[6] == 'y' && content[7] == 'p'
				&& Set.of("heic", "heix", "hevc", "hevx").contains(
						new String(content, 8, 4, java.nio.charset.StandardCharsets.US_ASCII))) {
			mediaType = MediaType.parseMediaType("image/heic");
		}
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
				.contentType(mediaType).body(content);
	}

	@Operation(summary = "영수증 삭제")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@LoginUserId Long userId, @PathVariable Long id) {
		history.delete(userId, id);
	}

	record ReceiptDetailResponse(Long id, ReceiptStoreCode storeCode, String storeName, LocalDate purchasedOn,
			String imageUrl, List<ReceiptHistoryUseCase.Item> items, long totalAmount) {
	}

	record ReceiptCreateResponse(Long receiptId, int ingredientCount, int cartRemovedCount) {
	}
}
