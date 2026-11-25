package com.remake.receipt.domain;

import java.util.Map;

public final class ReceiptIngredientAliases {

	private static final Map<String, String> NAMES = Map.ofEntries(
			Map.entry("오뚜기맛있는밥", "즉석밥"),
			Map.entry("미국산돼지삼겹살", "삼겹살"),
			Map.entry("풀무원다논그릭무가", "그릭요거트"),
			Map.entry("참송이버섯", "버섯"),
			Map.entry("크라운참크래커2번", "과자"),
			Map.entry("오리온치킨팝닭강정", "과자"),
			Map.entry("오리온썬핫스파이시", "과자"),
			Map.entry("오리온눈을감자", "과자"),
			Map.entry("삿포로(캔)", "맥주"),
			Map.entry("아사히수퍼드라이생", "맥주"),
			Map.entry("생생닭곰탕OKCOOK", "즉석국"),
			Map.entry("볶은참깨", "참깨"));

	private ReceiptIngredientAliases() {
	}

	public static String resolve(String name) {
		// 글자 사이의 OCR 공백을 무시하되 등록한 상품명 전체가 같을 때만 연결한다.
		String compactName = name.replaceAll("\\s+", "");
		// P는 영수증의 행사 표기일 수 있다. 이후에도 전체 상품명이 일치해야 한다.
		if (compactName.startsWith("P")) {
			compactName = compactName.substring(1);
		}
		return NAMES.get(compactName);
	}
}
