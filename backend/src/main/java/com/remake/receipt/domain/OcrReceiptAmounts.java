package com.remake.receipt.domain;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class OcrReceiptAmounts {

	public static final String MONEY = "(?:[0-9]{1,3}(?:\\h*[,\\.]\\h*[0-9]{3})+|[0-9]+)";
	public static final String QUANTITY = "[0-9]+(?:\\.[0-9]{1,3})?";
	public static final String QUANTITY_WITH_OCR_VARIANTS = "(?:" + QUANTITY + "|[|\\]ㅣ])";
	static final String MONEY_WITH_LEADING_ONE_VARIANT = "(?:" + MONEY + "|[|\\]ㅣ]\\h*[,\\.]\\h*[0-9]{3})";

	private static final Pattern GROUPING = Pattern.compile("[,.\\h]");

	private OcrReceiptAmounts() {
	}

	public static String moneyDigits(String value) {
		String digits = GROUPING.matcher(value).replaceAll("");
		return Character.isDigit(digits.charAt(0)) ? digits : "1" + digits.substring(1);
	}

	public static String verifiedQuantity(String rawUnitPrice, String quantity, String rawTotal) {
		boolean needsRepair = quantity == null || !Character.isDigit(quantity.charAt(0));
		String amount = needsRepair ? "1" : quantity;
		var parsedAmount = new BigDecimal(amount);
		var unitPrice = new BigDecimal(moneyDigits(rawUnitPrice));
		var total = new BigDecimal(moneyDigits(rawTotal));
		// 금액 표기가 깨져도 단가와 수량으로 품목 합계를 검증한다.
		if (parsedAmount.signum() <= 0 || (needsRepair && unitPrice.signum() == 0)) {
			return null;
		}
		return unitPrice.multiply(parsedAmount).compareTo(total) == 0 ? amount : null;
	}
}
