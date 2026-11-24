package com.remake.receipt.adapter.out.persistence;

import java.math.BigDecimal;

import com.remake.global.common.BaseCreatedEntity;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.Item;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "receipt_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ReceiptItemJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long receiptId;
	private Long categoryId;
	private String name;
	private BigDecimal amount;
	@Enumerated(EnumType.STRING)
	private IngredientUnit unit;
	private Long price;
	@Enumerated(EnumType.STRING)
	private StorageLocation storage;

	ReceiptItemJpaEntity(Long receiptId, Long categoryId, Item item) {
		this.receiptId = receiptId;
		this.categoryId = categoryId;
		this.name = item.name();
		this.amount = item.amount();
		this.unit = item.unit();
		this.price = item.price();
		this.storage = item.storage();
	}
}
