package com.remake.receipt.domain;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record OcrReceiptHeader(String storeName, LocalDate purchasedOn, LocalTime purchasedTime,
		String receiptNumber) {

	private static final Pattern STORE = Pattern.compile(
			"^(?:상\\h*호(?:명)?|판매처)\\h*[:：]\\h*(.+)$");
	private static final Pattern COMPANY = Pattern.compile(
			"^(?:주식회사|\\(주\\)|㈜)\\h*(.+)$");
	private static final Pattern STORE_TITLE = Pattern.compile(
			"^(?:[0-9]{1,2}\\h+)?(\\p{L}.*(?:마트|슈퍼|편의점)(?:\\h*[\\p{L}\\p{N}]+점)?)$");
	// 일부 영수증의 [구매] 라벨은 OCR에서 [구 0H]로 읽힌다.
	private static final Pattern PURCHASE_DATE = Pattern.compile(
			"^(?:(?:판\\h*매\\h*일|구\\h*매\\h*일|거\\h*래\\h*일)(?:시|자)?|\\[등[록독]\\]|\\[구\\h*(?:매|0H)\\])\\h*[:：]?\\h*"
					+ "([0-9]{4}|[0-9]{2})\\h*[-./]\\h*([0-9]{1,2})\\h*[-./]\\h*([0-9]{1,2})(?=\\D|$|[0-9]{1,2}:)");

	private static final Pattern PURCHASE_TIME = Pattern.compile(
			"^\\h*([0-9]{1,2}):([0-9]{2})(?::([0-9]{2}))?(?=$|[^0-9:])");
	private static final Pattern RECEIPT_NUMBER = Pattern.compile(
			"^(?:영\\h*수\\h*증|거\\h*래)\\h*번\\h*호\\h*[:：]?\\h*([0-9]+(?:-[0-9]+)*)$");

	/** 확실히 읽지 못한 값은 비워 두며, 승인번호나 사업자번호를 영수증 번호로 쓰지 않는다. */
	public static OcrReceiptHeader parse(String receiptText) {
		String storeName = null;
		LocalDate purchasedOn = null;
		LocalTime purchasedTime = null;
		var lines = receiptText.lines().map(String::strip).toList();
		for (String line : lines) {
			if (line.contains("상품명") || line.contains("품목명")) {
				break;
			}
			if (storeName == null) {
				storeName = readStoreName(line);
			}
			if (purchasedOn == null) {
				Matcher date = PURCHASE_DATE.matcher(line);
				if (date.find()) {
					purchasedOn = readPurchaseDate(date);
					if (purchasedOn != null) {
						purchasedTime = readPurchaseTime(line.substring(date.end()));
					}
				}
			}
		}
		// 영수증 번호는 품목 표 아래에도 인쇄될 수 있다.
		String receiptNumber = lines.stream()
				.map(RECEIPT_NUMBER::matcher)
				.filter(Matcher::matches)
				.map(number -> number.group(1))
				.findFirst().orElse(null);
		return new OcrReceiptHeader(storeName, purchasedOn, purchasedTime, receiptNumber);
	}

	private static String readStoreName(String line) {
		Matcher labelled = STORE.matcher(line);
		if (labelled.matches()) {
			String name = labelled.group(1).strip();
			return name.codePoints().anyMatch(Character::isLetter) ? name : null;
		}
		Matcher company = COMPANY.matcher(line);
		if (company.matches() && company.group(1).codePoints().anyMatch(
				codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HANGUL)) {
			return line;
		}
		Matcher title = STORE_TITLE.matcher(line);
		return title.matches() ? title.group(1) : null;
	}

	private static LocalDate readPurchaseDate(Matcher date) {
		int year = Integer.parseInt(date.group(1));
		if (date.group(1).length() == 2) {
			year += 2000;
		}
		try {
			return LocalDate.of(year, Integer.parseInt(date.group(2)), Integer.parseInt(date.group(3)));
		}
		catch (DateTimeException e) {
			return null;
		}
	}

	private static LocalTime readPurchaseTime(String suffix) {
		Matcher time = PURCHASE_TIME.matcher(suffix);
		if (!time.find()) {
			return null;
		}
		int second = time.group(3) == null ? 0 : Integer.parseInt(time.group(3));
		try {
			return LocalTime.of(Integer.parseInt(time.group(1)), Integer.parseInt(time.group(2)), second);
		}
		catch (DateTimeException e) {
			return null;
		}
	}
}
