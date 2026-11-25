package com.remake.share.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserId;
import com.remake.share.application.port.in.GetSharedRecipeUseCase;
import com.remake.share.application.port.in.PublishSharedRecipeUseCase;
import com.remake.share.application.port.in.ReportSharedRecipeUseCase;
import com.remake.share.application.port.in.SaveSharedRecipeUseCase;
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
	private final ReportSharedRecipeUseCase reportSharedRecipe;
	private final SaveSharedRecipeUseCase saveSharedRecipe;

	@Operation(summary = "공유 레시피 저장", description = "원본을 참조로 저장하고 현재 부족 재료를 장바구니에 담습니다. 이미 저장한 PUT 재시도는 추가 담기를 하지 않습니다.")
	@PutMapping("/{id}/save")
	public SaveSharedRecipeUseCase.SaveResult save(@LoginUserId Long userId, @PathVariable Long id) {
		return saveSharedRecipe.save(userId, id);
	}

	@Operation(summary = "공유 레시피 저장 취소", description = "원본의 내 저장만 취소하고 장바구니와 수정 수량을 유지합니다. 반복 취소도 성공합니다.")
	@DeleteMapping("/{id}/save")
	public SaveSharedRecipeUseCase.UnsaveResult unsave(@LoginUserId Long userId, @PathVariable Long id) {
		return saveSharedRecipe.unsave(userId, id);
	}

	@Operation(summary = "레시피 익명 공유", description = "내가 생성한 레시피를 제목·사진 1장과 공유합니다. 같은 원본은 KST 월요일 시작 주에 한 번만 공유할 수 있습니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public PublishedResponse publish(@LoginUserId Long userId,
			@Valid @RequestPart("payload") PublishSharedRecipeRequest payload,
			@RequestPart(value = "image", required = false) MultipartFile image, MultipartHttpServletRequest request) {
		if (request.getFiles("image").size() > 1) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		SharedRecipePhotoUpload photo = image == null ? null
				: new SharedRecipePhotoUpload(image.getContentType(), image.getSize(), image::getInputStream);
		return new PublishedResponse(publishSharedRecipe.publish(userId, payload.recipeId(), payload.title(), photo));
	}

	@Operation(summary = "익명 공유 상세", description = "공개 게시글의 조리 내용과 내 냉장고 재료명 기준 보유 여부를 조회합니다. 작성자 식별정보는 반환하지 않습니다.")
	@GetMapping("/{id}")
	public GetSharedRecipeUseCase.Detail detail(@LoginUserId Long userId, @PathVariable Long id) {
		return getSharedRecipe.getDetail(userId, id);
	}

	@Operation(summary = "공유 레시피 신고", description = "네 가지 사유 중 하나만 선택합니다. OTHER도 자유 입력이 없고 회원당 게시글별 한 번만 신고할 수 있습니다. 자동 숨김은 하지 않습니다.")
	@PostMapping("/{id}/reports")
	@ResponseStatus(HttpStatus.CREATED)
	public void report(@LoginUserId Long userId, @PathVariable Long id,
			@Valid @RequestBody ReportSharedRecipeRequest request) {
		reportSharedRecipe.report(userId, id, request.reason());
	}

	public record PublishedResponse(Long id) { }
}
