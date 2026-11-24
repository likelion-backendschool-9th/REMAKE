package com.remake.share.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserId;
import com.remake.share.application.port.in.GetSharedRecipeUseCase;
import com.remake.share.application.port.in.PublishSharedRecipeUseCase;
import com.remake.share.application.port.in.SharedRecipePhotoUpload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@Tag(name = "공유 레시피")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/shared-recipes")
@RequiredArgsConstructor
public class SharedRecipeController {
	private final PublishSharedRecipeUseCase publishSharedRecipe;
	private final GetSharedRecipeUseCase getSharedRecipe;

	@Operation(summary = "레시피 익명 공유", description = "내가 생성한 레시피를 제목·사진 1장과 공유합니다. 같은 원본은 KST 월요일 시작 주에 한 번만 공유할 수 있습니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public PublishedResponse publish(@LoginUserId Long userId,
			@Valid @RequestPart("payload") PublishSharedRecipeRequest payload,
			@RequestPart(value = "image", required = false) List<MultipartFile> images) {
		if (images != null && images.size() > 1) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		MultipartFile image = images == null || images.isEmpty() ? null : images.getFirst();
		SharedRecipePhotoUpload photo = image == null ? null
				: new SharedRecipePhotoUpload(image.getContentType(), image.getSize(), image::getInputStream);
		return new PublishedResponse(publishSharedRecipe.publish(userId, payload.recipeId(), payload.title(), photo));
	}

	@Operation(summary = "익명 공유 상세", description = "공개 게시글의 조리 내용과 내 냉장고 재료명 기준 보유 여부를 조회합니다. 작성자 식별정보는 반환하지 않습니다.")
	@GetMapping("/{id}")
	public GetSharedRecipeUseCase.Detail detail(@LoginUserId Long userId, @PathVariable Long id) {
		return getSharedRecipe.getDetail(userId, id);
	}

	public record PublishedResponse(Long id) { }
}
