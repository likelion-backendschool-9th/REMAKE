package com.remake.ingredient.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.remake.ingredient.application.port.in.UpdateIngredientCommand;
import com.remake.ingredient.application.port.in.UpdateIngredientCommand.PriceChange;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 재료 수정 요청. 보내지 않은 키는 바꾸지 않는다.
 * price는 키를 보내지 않으면 그대로, "price": null이면 가격을 지운다. Jackson은 이 둘을 모두 null로 읽으므로
 * setter가 불렸는지(priceSpecified)를 따로 기억한다.
 */
public class IngredientUpdateRequest {

	@Size(max = 50)
	@Pattern(regexp = ".*\\S.*", message = "비워 둘 수 없습니다.")
	private String name;
	private IngredientCategoryCode categoryCode;
	private StorageLocation storage;
	private LocalDate expirationDate;
	@DecimalMin(value = "0", inclusive = false)
	@Digits(integer = 7, fraction = 3)
	@Schema(description = "사용·폐기한 적이 없는 재료만 고칠 수 있습니다. 단위는 받지 않고, 이름이나 카테고리를 바꾸면 서버가 단위를 다시 정합니다.")
	private BigDecimal amount;
	@PositiveOrZero
	@Schema(nullable = true, description = "키를 보내지 않으면 그대로, null이면 가격을 지웁니다.")
	private Long price;
	private boolean priceSpecified;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public IngredientCategoryCode getCategoryCode() {
		return categoryCode;
	}

	public void setCategoryCode(IngredientCategoryCode categoryCode) {
		this.categoryCode = categoryCode;
	}

	public StorageLocation getStorage() {
		return storage;
	}

	public void setStorage(StorageLocation storage) {
		this.storage = storage;
	}

	public LocalDate getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(LocalDate expirationDate) {
		this.expirationDate = expirationDate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Long getPrice() {
		return price;
	}

	@JsonSetter("price")
	public void setPrice(Long price) {
		this.price = price;
		this.priceSpecified = true;
	}

	public UpdateIngredientCommand toCommand(Long userId, Long ingredientId) {
		return new UpdateIngredientCommand(userId, ingredientId, name, categoryCode, expirationDate, storage,
				priceSpecified ? new PriceChange(price) : null, amount);
	}
}
