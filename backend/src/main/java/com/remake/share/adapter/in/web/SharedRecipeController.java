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
import org.springframework.web.bind.annotation.RequestParam;
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
import com.remake.share.application.port.in.GetMySharedRecipesUseCase;
import com.remake.share.application.port.in.DeleteSharedRecipeUseCase;
import com.remake.global.common.PageResponse;
import com.remake.share.application.port.in.GetSharedRecipeRankingUseCase;
import com.remake.share.application.port.in.PublishSharedRecipeUseCase;
import com.remake.share.application.port.in.ReportSharedRecipeUseCase;
import com.remake.share.application.port.in.SaveSharedRecipeUseCase;
import com.remake.share.application.port.in.SharedRecipePhotoUpload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
	private final GetMySharedRecipesUseCase mySharedRecipes;
	private final DeleteSharedRecipeUseCase deleteSharedRecipe;
	private final PublishSharedRecipeUseCase publishSharedRecipe;
	private final GetSharedRecipeUseCase getSharedRecipe;
	private final ReportSharedRecipeUseCase reportSharedRecipe;
	private final SaveSharedRecipeUseCase saveSharedRecipe;
	private final GetSharedRecipeRankingUseCase rankings;

	@Operation(summary = "내 공유 글 목록", description = "본인의 공개·숨김 글을 최신 공유 시각·ID 내림차순으로 반환합니다. 삭제 글은 제외하고 숨김 글의 imageUrl은 null입니다.")
	@GetMapping("/mine")
	public PageResponse<GetMySharedRecipesUseCase.Item> mine(@LoginUserId Long userId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return mySharedRecipes.getMine(userId, page, size);
	}

	@Operation(summary = "내 공유 글 삭제", description = "공유 제목·사진을 제거하고 원본·타인 저장·장바구니·최소 이력을 보존합니다. 본인 삭제 글 재호출도 204, 타인·없는 글은 404입니다.")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@LoginUserId Long userId, @PathVariable Long id) {
		deleteSharedRecipe.delete(userId, id);
	}

	@Operation(summary = "이번 주 공유 레시피 TOP 3", description = "KST 월요일부터 현재까지 유효 저장이 1건 이상인 공개 글을 최대 3개 반환합니다. 순위 변동은 반환하지 않습니다.")
	@GetMapping("/weekly-top")
	public GetSharedRecipeRankingUseCase.WeeklyTop weeklyTop() {
		return rankings.weeklyTop();
	}

	@Operation(summary = "공유 레시피 랭킹", description = "popular는 이번 주 유효 저장이 1건 이상인 공개 글의 TOP 100 안에서 제목을 검색하며 전체 순위를 유지합니다. latest는 저장 수와 관계없이 전체 공개 글의 공유 시각순이며 rank·rankChange는 null입니다. rankChange는 지난주 순위 - 현재 순위입니다.")
	@GetMapping("/ranking")
	public GetSharedRecipeRankingUseCase.Ranking ranking(
			@Parameter(schema = @Schema(allowableValues = {"popular", "latest"}, defaultValue = "popular"))
			@RequestParam(defaultValue = "popular") String sort,
			@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return rankings.ranking(sort, q, page, size);
	}

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

	@Operation(summary = "익명 공유 상세", description = "공개 게시글과 본인의 숨김 글을 조회합니다. canDelete는 작성자 메뉴 표시용이며 작성자 식별정보는 반환하지 않습니다. 숨김 글의 imageUrl은 null입니다.")
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
