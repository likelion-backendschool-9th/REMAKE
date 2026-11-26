package com.remake.receipt.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalTime;

import com.remake.global.common.BaseCreatedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "receipt_first_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ReceiptFirstApplicationJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long userId;
	private String storeName;
	private LocalDate purchasedOn;
	private String receiptNumber;
	private LocalTime purchasedTime;

	ReceiptFirstApplicationJpaEntity(Long userId, String storeName, LocalDate purchasedOn, String receiptNumber,
			LocalTime purchasedTime) {
		this.userId = userId;
		this.storeName = storeName;
		this.purchasedOn = purchasedOn;
		this.receiptNumber = receiptNumber;
		this.purchasedTime = purchasedTime;
	}
}
