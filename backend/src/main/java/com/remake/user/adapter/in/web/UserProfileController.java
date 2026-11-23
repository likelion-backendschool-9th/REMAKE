package com.remake.user.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.user.application.port.in.GetProfileUseCase;
import com.remake.user.application.port.in.UpdateProfileUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "프로필")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

	private final GetProfileUseCase getProfileUseCase;
	private final UpdateProfileUseCase updateProfileUseCase;

	@Operation(summary = "프로필 조회", description = "설정 화면에 필요한 값을 한 번에 돌려준다.")
	@GetMapping
	public ProfileResponse getProfile(@LoginUserId Long userId) {
		return ProfileResponse.from(getProfileUseCase.getProfile(userId));
	}

	@Operation(summary = "프로필 부분 수정", description = "가구원 수·레시피 재료 보충·알림 받기 중 보낸 값만 바꾼다.")
	@PatchMapping
	public ProfileResponse updateProfile(@LoginUserId Long userId, @Valid @RequestBody UpdateProfileRequest request) {
		return ProfileResponse.from(updateProfileUseCase.updateProfile(request.toCommand(userId)));
	}
}
