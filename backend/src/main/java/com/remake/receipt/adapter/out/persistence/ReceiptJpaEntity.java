package com.remake.receipt.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalTime;

import com.remake.global.common.BaseCreatedEntity;
import com.remake.receipt.domain.ReceiptStoreCode;

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
@Table(name = "receipts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ReceiptJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long userId;
	private LocalDate purchasedOn;
	private LocalTime purchasedTime;
	private String receiptNumber;
	private String storeName;
	@Enumerated(EnumType.STRING)
	private ReceiptStoreCode storeCode;
	private String imageKey;

	ReceiptJpaEntity(Long userId, LocalDate purchasedOn, LocalTime purchasedTime, String receiptNumber,
			String storeName, ReceiptStoreCode storeCode, String imageKey) {
		this.userId = userId;
		this.purchasedOn = purchasedOn;
		this.purchasedTime = purchasedTime;
		this.receiptNumber = receiptNumber;
		this.storeName = storeName;
		this.storeCode = storeCode;
		this.imageKey = imageKey;
	}
}
