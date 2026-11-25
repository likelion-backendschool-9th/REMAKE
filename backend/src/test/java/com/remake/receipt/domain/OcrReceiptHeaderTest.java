package com.remake.receipt.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class OcrReceiptHeaderTest {

	@Test
	void 대괄호_구매_라벨과_OCR_오인식에서도_일시를_읽는다() {
		assertThat(OcrReceiptHeader.parse("[구 0H]2017-06-0221:13   POS"))
				.isEqualTo(new OcrReceiptHeader(null, LocalDate.of(2017, 6, 2), LocalTime.of(21, 13), null));
		assertThat(OcrReceiptHeader.parse("[구   매]2021-03-03   19:00"))
				.isEqualTo(new OcrReceiptHeader(null, LocalDate.of(2021, 3, 3), LocalTime.of(19, 0), null));
	}

	@Test
	void 마트_지점명과_등록_일시를_읽는다() {
		assertThat(OcrReceiptHeader.parse("""
				0   롯데쇼핑(주)롯데마트 계양점
				[등독] 2026-05-0119:46
				상품명 단가 수량 금액
				""")).isEqualTo(new OcrReceiptHeader("롯데쇼핑(주)롯데마트 계양점", LocalDate.of(2026, 5, 1),
				LocalTime.of(19, 46), null));
	}

	@Test
	void 법인명과_두_자리_판매일을_읽는다() {
		assertThat(OcrReceiptHeader.parse("""
				주식회사 테스트식자재마트
				판매일 26-11-16 19:27, 월요일
				""")).isEqualTo(new OcrReceiptHeader("주식회사 테스트식자재마트", LocalDate.of(2026, 11, 16),
				LocalTime.of(19, 27), null));
	}

	@Test
	void 상호와_네_자리_거래일을_읽는다() {
		assertThat(OcrReceiptHeader.parse("""
				상 호: 테스트 마트
				거래일시: 2026. 11. 16 19:27
				""")).isEqualTo(new OcrReceiptHeader("테스트 마트", LocalDate.of(2026, 11, 16),
				LocalTime.of(19, 27), null));
	}

	@Test
	void 날짜가_잘못돼도_읽힌_판매처는_유지한다() {
		assertThat(OcrReceiptHeader.parse("""
				판매처: 테스트 마트
				구매일 2026/02/30
				""")).isEqualTo(new OcrReceiptHeader("테스트 마트", null, null, null));
	}

	@Test
	void 글자가_깨진_상단에서_판매처나_구매일을_추측하지_않는다() {
		assertThat(OcrReceiptHeader.parse("""
				주식회사 SKATE
				관마일 a8 187g
				쿠폰 만료 2026-11-30
				""")).isEqualTo(new OcrReceiptHeader(null, null, null, null));
	}

	@Test
	void 품목표_아래_영수증_번호도_앞자리_0을_보존해_읽는다() {
		assertThat(OcrReceiptHeader.parse("""
				구매일시 2026-11-16 19:27:08
				품목명 수량 금액
				우유 2개 5000
				승인번호: 999999
				영수증 번호: 000123-02
				""")).isEqualTo(new OcrReceiptHeader(null, LocalDate.of(2026, 11, 16),
				LocalTime.of(19, 27, 8), "000123-02"));
	}

	@Test
	void 잘못된_시각과_승인번호는_비워둔다() {
		assertThat(OcrReceiptHeader.parse("""
				판매일 2026-11-16 19:99
				승인번호 012345
				""")).isEqualTo(new OcrReceiptHeader(null, LocalDate.of(2026, 11, 16), null, null));
	}
}
