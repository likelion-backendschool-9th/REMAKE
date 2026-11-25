package com.remake.receipt.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.remake.ingredient.domain.IngredientUnit;

class OcrReceiptItemParserTest {

	private final OcrReceiptItemParser parser = new OcrReceiptItemParser();

	@Test
	void 상품명_용량과_구매_전체량과_미확인_수량을_구분한다() {
		var items = parser.parse("""
				상품명 단가 수량 금액
				삼겹살500g 6,000 2 12,000
				사과 0.480kg 3,000
				닭가슴살 480g 5,000 10,000
				우유 2×500ml 4,000 1 4,000
				""");
		assertThat(items).extracting(OcrReceiptItemParser.Item::purchaseQuantity)
				.containsExactly(new BigDecimal("2"), null, null, BigDecimal.ONE);
		assertThat(items).extracting(OcrReceiptItemParser.Item::volume).containsExactly(
				volume("500", IngredientUnit.G, OcrReceiptItemParser.VolumeScope.PER_PACK),
				volume("0.480", IngredientUnit.KG, OcrReceiptItemParser.VolumeScope.TOTAL),
				volume("480", IngredientUnit.G, OcrReceiptItemParser.VolumeScope.PER_PACK),
				null);
	}

	private static OcrReceiptItemParser.Volume volume(String amount, IngredientUnit unit,
			OcrReceiptItemParser.VolumeScope scope) {
		return new OcrReceiptItemParser.Volume(new BigDecimal(amount), unit, scope);
	}

	@Test
	void 두_줄에서_포장_표기와_구매_수량을_구분한다() {
		assertThat(parser.parse("""
				상품명        단가   수량   금액
				009 느타리버섯 1팩
				22000217     495    2      990*#
				012 쇠고기죽 287.5g
				8801047161530 1,990 3      5,970*
				합 계: 6,960
				봉투 1 100
				""")).containsExactly(
				new OcrReceiptItemParser.Item("느타리버섯",
						new OcrQuantity(new BigDecimal("2"), IngredientUnit.PACK), 990, null),
				new OcrReceiptItemParser.Item("쇠고기죽 287.5g",
						new OcrQuantity(new BigDecimal("3"), IngredientUnit.EA), 5970,
						volume("287.5", IngredientUnit.G, OcrReceiptItemParser.VolumeScope.PER_PACK)));
	}

	@Test
	void 한_줄의_품목과_수량과_금액을_읽는다() {
		assertThat(parser.parse("""
				품목명    수량    금액
				우유      2개     5,000원
				사과      0.5kg   3,000
				""")).containsExactly(
				new OcrReceiptItemParser.Item("우유",
						new OcrQuantity(new BigDecimal("2"), IngredientUnit.EA), 5000, null),
				new OcrReceiptItemParser.Item("사과",
						new OcrQuantity(new BigDecimal("0.5"), IngredientUnit.KG), 3000,
						volume("0.5", IngredientUnit.KG, OcrReceiptItemParser.VolumeScope.TOTAL)));
	}

	@Test
	void 단가와_합계가_같고_수량만_빠진_줄은_한_개로_읽는다() {
		assertThat(parser.parse("""
				상품명 단가 수량 금액
				삼겹살 19,285 19,285
				검증되지 않은 품목 900 1,800
				004새송이버섯 1봉
				22000224 1,490 1,490#
				""")).containsExactly(
				new OcrReceiptItemParser.Item("삼겹살", new OcrQuantity(BigDecimal.ONE, IngredientUnit.EA), 19285, null),
				new OcrReceiptItemParser.Item("새송이버섯", new OcrQuantity(BigDecimal.ONE, IngredientUnit.PACK), 1490, null));
	}

	@Test
	void 손상된_상품코드와_금액도_세_숫자가_일치할_때만_복원한다() {
		assertThat(parser.parse("""
				003 카스캔 355ml 묶음(6)
				88010: 3011116  10,800    ㅣ   10,800
				008 동원)양반야채죽 287.50
				8601047161646   1,990    |    ] ,990*
				16 아삭이고추
				구분          2.980     1    2,980 #
				017 깨진 품목
				구분          2.495     2    5,990 #
				""")).extracting(OcrReceiptItemParser.Item::totalPrice).containsExactly(10800L, 1990L, 2980L);
	}

	@Test
	void 포장_표기의_세로획을_읽어_실제_구매_단위를_복원한다() {
		assertThat(parser.parse("""
				상품명 단가 수량 금액
				009 느타리버섯 |팩
				22000217 495 2 990
				""")).containsExactly(new OcrReceiptItemParser.Item(
				"느타리버섯", new OcrQuantity(new BigDecimal("2"), IngredientUnit.PACK), 990, null));
	}

	@Test
	void 제목_없이도_검증된_두_줄_품목이_두_개_이상이면_읽는다() {
		assertThat(parser.parse("""
				002 동태(냉동)
				204225 6,900 ] 6,900 #
				003 금액이 깨진 품목
				8801047161530 1,999 9 60706
				004 새송이버섯 1봉

				22000224 1,490 | 1,490 #
				""")).extracting(OcrReceiptItemParser.Item::totalPrice).containsExactly(6900L, 1490L);
	}

	@Test
	void 제목_없이_두_줄_품목이_하나뿐이면_표로_보지_않는다() {
		assertThat(parser.parse("""
				002 동태(냉동)
				잡문자가 끼어든 줄
				204225 6,900 ] 6,900 #
				004 새송이버섯 1봉
				22000224 1,490 | 1,490 #
				""")).isEmpty();
	}

	@Test
	void 제목_없이도_검증된_한_줄_품목이_연속되면_읽는다() {
		assertThat(parser.parse("""
				테스트 마트
				크러운 첨크레키 2번 6,090 1 6,090
				waa AY 돼지 삼겹살 19,285 1 19,285
				[MEN] 14164 -12,980
				금액이 깨진 품목 990 1 1,990
				*짐송이버섯 17,265 1 17,265
				오리온 눈을감자 990 1 990
				합계 123,650
				추가 안내 500 1 500
				""")).extracting(OcrReceiptItemParser.Item::totalPrice)
				.containsExactly(6090L, 19285L, 17265L, 990L);
	}

	@Test
	void 쉼표_주변_공백과_마침표가_섞여도_품목_금액을_검증한다() {
		assertThat(parser.parse("""
				상품명 단가 수량 금액
				서울우유 모리키미 A 2,700 2 5, 400
				미 스 정과 9,990 ㅣ 9 ,990
				재사용봉투 870 2 1.740
				금액이 깨진 품목 870 2 1.750
				""")).extracting(OcrReceiptItemParser.Item::totalPrice)
				.containsExactly(5400L, 9990L, 1740L);
	}

	@Test
	void 제목_없이_서로_떨어진_한_줄_후보는_품목_표로_보지_않는다() {
		assertThat(parser.parse("""
				단독 후보 990 1 990
				구분 문구
				다른 후보 500 1 500
				""")).isEmpty();
	}

	@Test
	void 단가와_품목_합계가_일치할_때_세로획을_수량_한_개로_복원한다() {
		assertThat(parser.parse("""
				상품명    단가    수량    금액
				002 동태(냉동)
				204225 6,900 ] 6,900 #
				004 새송이버섯 1봉
				22000224 1,490 | 1,490 #
				""")).containsExactly(
				new OcrReceiptItemParser.Item("동태(냉동)",
						new OcrQuantity(BigDecimal.ONE, IngredientUnit.EA), 6900, null),
				new OcrReceiptItemParser.Item("새송이버섯",
						new OcrQuantity(BigDecimal.ONE, IngredientUnit.PACK), 1490, null));
	}

	@Test
	void 숫자로_읽혔어도_단가와_수량으로_계산한_금액이_다르면_건너뛴다() {
		assertThat(parser.parse("""
				상품명    단가    수량    금액
				012 동원)양반진쇠고기죽 287,50
				8801047161530 1,999 9 60706
				013 오뚜기)참치죽 285g
				8801045303024 1,980 1 1,980*
				""")).containsExactly(
				new OcrReceiptItemParser.Item("오뚜기)참치죽 285g",
						new OcrQuantity(BigDecimal.ONE, IngredientUnit.EA), 1980,
						volume("285", IngredientUnit.G, OcrReceiptItemParser.VolumeScope.PER_PACK)));
	}

	@Test
	void 금액_끝의_영문자가_붙어도_단가와_수량이_맞을_때만_읽는다() {
		assertThat(parser.parse("""
				상품명    단가    수량    금액
				009 느타리버섯 jay
				22000217 495 2 990s
				010 초코파이
				8801117534912 4,700 1 4,900S
				""")).containsExactly(
				new OcrReceiptItemParser.Item("느타리버섯 jay",
						new OcrQuantity(new BigDecimal("2"), IngredientUnit.EA), 990, null));
	}

	@Test
	void 숫자_줄이_깨진_품목에_다음_품목의_금액을_붙이지_않는다() {
		assertThat(parser.parse("""
				상품명    단가    수량    금액
				004 새송이버섯 1봉
				22000224 1,490 | 10,800
				009 느타리버섯 1팩
				22000217 495 2 990
				""")).extracting(OcrReceiptItemParser.Item::name).containsExactly("느타리버섯");
	}

	@Test
	void 상품코드_표에서_깨진_코드와_순번이_없는_행사_품목을_읽는다() {
		assertThat(parser.parse("""
				삼품(코드)   단가 수량 금액
				003 냉면 1팩
				8801043044978   3,980   3.980
				004 금액이 깨진 품목
				8803712331064   4,480   2   8,880
				P도시락김 80g   [6,480]
				880-114303516   4,980   4,980
				008 반찬
				*236287   10,000   1   10,000
				009 참깨
				244175   6,000   2   12,000   ER
				총 구 매 액: 30,960
				010 다음 줄
				244176 2,000 1 2,000
				""")).extracting(OcrReceiptItemParser.Item::totalPrice)
				.containsExactly(3980L, 4980L, 10000L, 12000L);
	}
}
