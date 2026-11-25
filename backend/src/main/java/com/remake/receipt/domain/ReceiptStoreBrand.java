package com.remake.receipt.domain;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ReceiptStoreBrand {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern CU = Pattern.compile("(?<![A-Z0-9])CU(?![A-Z0-9])");

	// 같은 이름으로 시작하는 브랜드는 고유한 이름부터 확인한다.
	private static final List<BrandName> BRAND_NAMES = List.of(
			new BrandName("이마트에브리데이", ReceiptStoreCode.EMART_EVERYDAY),
			new BrandName("트레이더스", ReceiptStoreCode.TRADERS),
			new BrandName("이마트24", ReceiptStoreCode.EMART24),
			new BrandName("이마트", ReceiptStoreCode.EMART),
			new BrandName("홈플러스익스프레스", ReceiptStoreCode.HOMEPLUS_EXPRESS),
			new BrandName("홈플러스", ReceiptStoreCode.HOMEPLUS),
			new BrandName("롯데마트", ReceiptStoreCode.LOTTE_MART),
			new BrandName("롯데슈퍼", ReceiptStoreCode.LOTTE_SUPER),
			new BrandName("하나로마트", ReceiptStoreCode.HANARO),
			new BrandName("코스트코", ReceiptStoreCode.COSTCO),
			new BrandName("COSTCO", ReceiptStoreCode.COSTCO),
			new BrandName("세븐일레븐", ReceiptStoreCode.SEVEN_ELEVEN),
			new BrandName("7-ELEVEN", ReceiptStoreCode.SEVEN_ELEVEN),
			new BrandName("GS더프레시", ReceiptStoreCode.GS_FRESH),
			new BrandName("GS25", ReceiptStoreCode.GS25));

	private ReceiptStoreBrand() {
	}

	public static ReceiptStoreCode match(String storeName) {
		if (storeName == null) {
			return null;
		}
		String upperName = storeName.toUpperCase(Locale.ROOT);
		String name = WHITESPACE.matcher(upperName).replaceAll("");
		for (BrandName brand : BRAND_NAMES) {
			if (name.contains(brand.name())) {
				return brand.code();
			}
		}
		return CU.matcher(upperName).find() ? ReceiptStoreCode.CU : null;
	}

	private record BrandName(String name, ReceiptStoreCode code) {
	}
}
