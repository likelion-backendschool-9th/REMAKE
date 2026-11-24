package com.remake.cart.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.cart.application.port.in.DeleteCartItemsUseCase;
import com.remake.cart.application.port.in.GetCartItemsUseCase;
import com.remake.cart.application.port.in.MoveCartItemsToFridgeUseCase;
import com.remake.cart.application.port.in.UpdateCartItemUseCase;
import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@Tag(name = "장바구니")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/cart-items")
@RequiredArgsConstructor
public class CartItemController {
	private final GetCartItemsUseCase getCartItemsUseCase;
	private final UpdateCartItemUseCase updateCartItemUseCase;
	private final DeleteCartItemsUseCase deleteCartItemsUseCase;
	private final MoveCartItemsToFridgeUseCase moveCartItemsToFridgeUseCase;

	@Operation(summary = "장바구니 목록", description = "본인 항목을 담긴 id 순의 items 배열로 반환합니다. 다른 레시피의 같은 재료는 별도 행입니다.")
	@GetMapping
	public CartItemsResponse list(@LoginUserId Long userId) {
		return new CartItemsResponse(getCartItemsUseCase.getItems(userId).stream().map(CartItemResponse::from).toList());
	}

	@Operation(summary = "장바구니 수량 변경", description = "수량만 변경합니다. 0 초과, 정수 7자리·소수 3자리까지 허용합니다.")
	@PatchMapping("/{id}")
	public CartAmountResponse update(@LoginUserId Long userId, @PathVariable Long id,
			@Valid @RequestBody CartAmountRequest request) {
		var item = updateCartItemUseCase.updateAmount(userId, id, request.amount());
		return new CartAmountResponse(item.id(), item.amount(), item.unit());
	}

	@Operation(summary = "선택 장바구니 항목 삭제", description = "비어 있지 않은 중복 없는 본인 ids만 허용합니다. 하나라도 없으면 전체를 유지합니다.")
	@PostMapping("/delete")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@LoginUserId Long userId, @Valid @RequestBody CartSelectionRequest request) {
		deleteCartItemsUseCase.deleteItems(userId, request.ids());
	}

	@Operation(summary = "선택 항목 냉장고 이동", description = "카테고리 기본 보관 위치·소비기한, 가격 없이 등록합니다. 수량·단위를 보존하고 큰술·작은술·컵만 ml로 환산합니다. 등록·장바구니 제거는 함께 반영합니다.")
	@PostMapping("/move-to-fridge")
	public CartMoveResponse move(@LoginUserId Long userId, @Valid @RequestBody CartSelectionRequest request) {
		return new CartMoveResponse(moveCartItemsToFridgeUseCase.move(userId, request.ids()));
	}
}
