package com.remake.receipt.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ReceiptStoreBrandTest {

	@ParameterizedTest
	@CsvSource({
			"이마트 에브리데이 테스트점, EMART_EVERYDAY",
			"이마트 트레이더스 테스트점, TRADERS",
			"이마트 24 테스트점, EMART24",
			"이마트 테스트점, EMART",
			"홈플러스 익스프레스 테스트점, HOMEPLUS_EXPRESS",
			"홈플러스 테스트점, HOMEPLUS",
			"롯데쇼핑(주)롯데마트 계양점, LOTTE_MART",
			"롯데슈퍼 테스트점, LOTTE_SUPER",
			"농협 하나로마트 테스트점, HANARO",
			"Costco 테스트점, COSTCO",
			"세븐일레븐 테스트점, SEVEN_ELEVEN",
			"GS 더프레시 테스트점, GS_FRESH",
			"gs25 테스트점, GS25",
			"CU테스트점, CU"
	})
	void 상호에서_브랜드를_구분한다(String name, ReceiptStoreCode code) {
		assertThat(ReceiptStoreBrand.match(name)).isEqualTo(code);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"동네마트", "CUP STORE"})
	void 브랜드가_확인되지_않으면_비워_둔다(String name) {
		assertThat(ReceiptStoreBrand.match(name)).isNull();
	}
}
