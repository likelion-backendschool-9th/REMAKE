package com.remake.share.adapter.in.web;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.remake.global.config.OpenApiConfig;
import com.remake.share.application.port.in.GetSharedRecipePhotoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "공유 레시피")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/shared-recipe-photos")
@RequiredArgsConstructor
public class SharedRecipePhotoController {
	private final GetSharedRecipePhotoUseCase getSharedRecipePhoto;

	@Operation(summary = "공유 사진 조회", description = "id는 공유 게시글 ID입니다. 로그인한 사용자에게 공개 게시글의 사진만 제공합니다. 숨겨진 글과 누락된 파일은 404입니다.")
	@GetMapping("/{id}")
	public ResponseEntity<InputStreamResource> image(@PathVariable Long id) {
		var photo = getSharedRecipePhoto.getPhoto(id);
		// ResourceHttpMessageConverter가 응답 전송 후 스트림을 닫는다.
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(photo.contentType()))
				.contentLength(photo.contentLength()).cacheControl(CacheControl.noStore())
				.header("X-Content-Type-Options", "nosniff")
				.body(new InputStreamResource(photo.stream()));
	}
}
