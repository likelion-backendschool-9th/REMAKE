package com.remake.receipt.adapter.in.web;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserId;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/user/receipts")
@RequiredArgsConstructor
class ReceiptController {

	private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/heic");
	private final RegisterReceiptUseCase registerReceiptUseCase;

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

	record ReceiptCreateResponse(Long receiptId, int ingredientCount, int cartRemovedCount) {
	}
}
