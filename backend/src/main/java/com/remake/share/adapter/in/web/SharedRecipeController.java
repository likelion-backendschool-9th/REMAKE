package com.remake.share.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

	public record PublishedResponse(Long id) { }
}
