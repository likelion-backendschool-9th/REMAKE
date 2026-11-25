package com.remake.receipt.domain;

import java.time.LocalDate;

/** 구매일 기준 6개월 보관. 등록 거절과 새벽 정리 배치가 같은 경계를 쓴다. */
public final class ReceiptRetention {

	public static final int MONTHS = 6;

	private ReceiptRetention() {
	}

	/** 이날부터는 보관한다. 이보다 이른 구매일이면 너무 오래됐다. */
	public static LocalDate earliestAllowedPurchaseDate(LocalDate today) {
		return today.minusMonths(MONTHS);
	}

	public static boolean isTooOld(LocalDate purchasedOn, LocalDate today) {
		return purchasedOn.isBefore(earliestAllowedPurchaseDate(today));
	}
}
