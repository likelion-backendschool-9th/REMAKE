package com.remake.ingredient.domain;

import java.time.LocalDate;
import java.util.Collection;

/** 냉장고 요약. total은 위험·임박·여유의 합이다. */
public record FridgeSummary(int total, int danger, int soon, int ok) {

	/** 소비기한 목록으로 센다. 목록 화면과 같은 ExpiryPolicy.statusOf로 판정하므로 수치가 어긋나지 않는다. */
	public static FridgeSummary of(Collection<LocalDate> expirationDates, LocalDate today) {
		int danger = 0;
		int soon = 0;
		int ok = 0;
		for (LocalDate date : expirationDates) {
			switch (ExpiryPolicy.statusOf(ExpiryPolicy.dDay(date, today))) {
				case DANGER -> danger++;
				case SOON -> soon++;
				case OK -> ok++;
			}
		}
		return new FridgeSummary(danger + soon + ok, danger, soon, ok);
	}
}
