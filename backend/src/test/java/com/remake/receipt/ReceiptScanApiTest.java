package com.remake.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import com.remake.auth.application.port.out.JwtTokenPort;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.support.IntegrationTestSupport;
import com.remake.user.domain.UserRole;

import tools.jackson.databind.json.JsonMapper;

class ReceiptScanApiTest extends IntegrationTestSupport {

	private static final String SCAN_PATH = "/api/v1/user/receipts/scan";
	@Autowired
	private JwtTokenPort jwtTokens;

	private Long userId;

	@BeforeEach
	void createScanOwner() {
		userId = createUser();
	}

	private Cookie accessToken() {
		return new Cookie("ACCESS_TOKEN", jwtTokens.issueAccessToken(userId, UserRole.USER));
	}

	@Test
	void 확인된_상품명만_사전에_연결하고_OCR_원문을_함께_보낸다() throws Exception {
		when(receiptText.read(any(byte[].class), any(Duration.class))).thenReturn("""
				상품명 단가 수량 금액
				오뚜기 맛있는밥 8,980 1 8,980
				미국산 돼지 삼겹살 9,000 2 18,000
				P생생닭곰탕 OKCOOK500g 3,980 2 7,960
				P볶은참깨 6,000 2 12,000
				양파링 1,000 1 1,000
				삼겹살맛 과자 2,000 1 2,000
				아몬드 핸드로션 3,000 1 3,000
				풀무원다는 그릭 무가 8,980 1 8,980
				크리운 참크래커 2번 3,000 2 6,000
				오리온 지킨팝 닭강정 990 1 990
				P생생닭공탕 OKCDOK500g 3,980 2 7,960
				P붉은참개 6,000 2 12,000
				""");
		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(4))
				.andExpect(jsonPath("$.items[0].name").value("즉석밥"))
				.andExpect(jsonPath("$.items[0].sourceName").value("오뚜기 맛있는밥"))
				.andExpect(jsonPath("$.items[1].name").value("삼겹살"))
				.andExpect(jsonPath("$.items[1].unit").value("PACK"))
				.andExpect(jsonPath("$.items[1].amount").value(2))
				.andExpect(jsonPath("$.items[2].name").value("즉석국"))
				.andExpect(jsonPath("$.items[2].volume.amount").value(500))
				.andExpect(jsonPath("$.items[3].name").value("참깨"));
	}

	@Test
	void 확인된_과자와_맥주를_포장_수량으로_인식하고_비슷한_이름은_건너뛴다() throws Exception {
		when(receiptText.read(any(byte[].class), any(Duration.class))).thenReturn("""
				상품명 단가 수량 금액
				크라운 참크래커 2번 3,000 2 6,000
				삿포로 (캔) 3,000 1 3,000
				생생닭곰탕 OKCOOK500g 4,000 1 4,000
				오리온 눈을감자 증정품 봉투 100 1 100
				아사히 수퍼드라이 생 전용잔 1,000 1 1,000
				""");
		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(3))
				.andExpect(jsonPath("$.items[0].name").value("과자"))
				.andExpect(jsonPath("$.items[0].amount").value(2))
				.andExpect(jsonPath("$.items[0].unit").value("PACK"))
				.andExpect(jsonPath("$.items[1].name").value("맥주"))
				.andExpect(jsonPath("$.items[1].unit").value("EA"))
				.andExpect(jsonPath("$.items[2].name").value("즉석국"));
	}

	@Test
	void 사전에서_확인되는_식재료가_없으면_422를_반환한다() throws Exception {
		when(receiptText.read(any(byte[].class), any(Duration.class)))
				.thenReturn("품목명 수량 금액\n수세미 1개 2,000");
		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("RECEIPT_NO_FOOD_ITEMS"));
		assertFailureEvent("RECEIPT_NO_FOOD_ITEMS");
	}

	@Test
	void 사전이_G이고_포장_용량이_ML이면_구매_수량을_PACK으로_미리_보여_준다() throws Exception {
		when(receiptText.read(any(byte[].class), any(Duration.class))).thenReturn("""
				품목명 수량 금액
				고추장 500mL 1팩 5,000
				""");

		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].amount").value(1))
				.andExpect(jsonPath("$.items[0].unit").value("PACK"))
				.andExpect(jsonPath("$.items[0].purchaseQuantity").value(1))
				.andExpect(jsonPath("$.items[0].volume.unit").value("ML"));
	}

	@Test
	void 구매_수량과_팩당_용량과_전체_용량을_별도_필드로_반환한다() throws Exception {
		when(receiptText.read(any(byte[].class), any(Duration.class))).thenReturn("""
				품목명 수량 금액
				돼지고기 500g 2 12,000
				돼지고기 0.48kg 3,000
				양파 2개 6,000
				수세미 1개 2,000
				""");
		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(3))
				.andExpect(jsonPath("$.items[0].categoryCode").value("MEAT"))
				.andExpect(jsonPath("$.items[0].storage").value("FRIDGE"))
				.andExpect(jsonPath("$.items[0].amount").value(1000))
				.andExpect(jsonPath("$.items[0].purchaseQuantity").value(2))
				.andExpect(jsonPath("$.items[0].volume.amount").value(500))
				.andExpect(jsonPath("$.items[0].volume.unit").value("G"))
				.andExpect(jsonPath("$.items[0].volume.scope").value("PER_PACK"))
				.andExpect(jsonPath("$.items[1].purchaseQuantity").doesNotExist())
				.andExpect(jsonPath("$.items[1].volume.amount").value(0.48))
				.andExpect(jsonPath("$.items[1].volume.scope").value("TOTAL"))
				.andExpect(jsonPath("$.items[2].purchaseQuantity").value(2))
				.andExpect(jsonPath("$.items[2].volume").doesNotExist());
	}

	@Test
	void 로그인하지_않으면_스캔할_수_없다() throws Exception {
		mockMvc.perform(multipart(SCAN_PATH).file(new MockMultipartFile("image", new byte[]{1})))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events", Long.class)).isZero();
	}

	@Test
	void 파일명이_JPG여도_이미지_헤더가_다르면_거절한다() throws Exception {
		var image = new MockMultipartFile("image", "receipt.jpg", "image/jpeg", new byte[]{1, 2, 3});
		mockMvc.perform(multipart(SCAN_PATH).file(image).cookie(accessToken()))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
		assertFailureEvent("UNSUPPORTED_FILE_TYPE");
	}

	@Test
	void 사진_크기가_10MB를_넘으면_거절한다() throws Exception {
		var image = new MockMultipartFile("image", new byte[10 * 1024 * 1024 + 1]);
		mockMvc.perform(multipart(SCAN_PATH).file(image).cookie(accessToken()))
				.andExpect(status().is(413))
				.andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
		assertFailureEvent("FILE_TOO_LARGE");
	}

	@Test
	void 비어_있는_사진은_거절하고_실패를_기록한다() throws Exception {
		var image = new MockMultipartFile("image", new byte[0]);
		mockMvc.perform(multipart(SCAN_PATH).file(image).cookie(accessToken()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		assertFailureEvent("INVALID_INPUT");
	}

	@Test
	void JWT로_인증한_사용자는_정확히_6개월_전_영수증을_스캔할_수_있다() throws Exception {
		stubReceiptText("판매일 2025-09-04 14:25:08\n거래번호: 000123");
		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.purchasedOn").value("2025-09-04"))
				.andExpect(jsonPath("$.purchasedTime").value("14:25:08"))
				.andExpect(jsonPath("$.receiptNumber").value("000123"))
				.andExpect(jsonPath("$.storeCode").value("LOTTE_MART"))
				.andExpect(jsonPath("$.items[0].name").value("우유"));
	}

	@Test
	void 구매일이_6개월보다_오래되면_422와_실패_이벤트를_기록한다() throws Exception {
		stubReceiptText("판매일 2025-09-03");
		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.code").value("RECEIPT_TOO_OLD"));
		assertFailureEvent("RECEIPT_TOO_OLD");
	}

	@Test
	void 하루_레시피_횟수를_다_써도_스캔은_성공하고_사용_기록을_남기지_않는다() throws Exception {
		for (int i = 0; i < 2; i++) {
			jdbcTemplate.update("INSERT INTO usage_logs (user_id, type, created_at) VALUES (?, 'RECIPE', ?)", userId,
					LocalDateTime.of(2026, 3, 4, 9, i));
		}
		stubReceiptText("판매일 2025-09-04");

		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].name").value("우유"));

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'receipt_scan_succeeded' AND user_id = ?", Integer.class,
				userId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usage_logs", Integer.class)).isEqualTo(2);
	}

	@Test
	void 구매일을_읽지_못하면_품목을_반환한다() throws Exception {
		stubReceiptText("");
		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.purchasedOn").doesNotExist())
				.andExpect(jsonPath("$.purchasedTime").doesNotExist())
				.andExpect(jsonPath("$.receiptNumber").doesNotExist())
				.andExpect(jsonPath("$.storeCode").doesNotExist())
				.andExpect(jsonPath("$.items[0].name").value("우유"));
	}

	@ParameterizedTest
	@CsvSource({"OCR_TIMEOUT, 504", "OCR_UPSTREAM_ERROR, 502"})
	void OCR_실패는_재시도_없이_응답하고_이벤트를_한_건_기록한다(ErrorCode errorCode, int httpStatus)
			throws Exception {
		when(receiptText.read(any(byte[].class), eq(Duration.ofSeconds(30))))
				.thenThrow(new BusinessException(errorCode));

		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().is(httpStatus))
				.andExpect(jsonPath("$.code").value(errorCode.name()));

		verify(receiptText).read(any(byte[].class), eq(Duration.ofSeconds(30)));
		verifyNoMoreInteractions(receiptText);
		assertFailureEvent(errorCode.name());
	}

	@Test
	void 스캔한_수량으로_등록하고_등록한_영수증의_재스캔은_거절한다() throws Exception {
		when(receiptText.read(any(byte[].class), any(Duration.class))).thenReturn("""
				상호: 롯데마트 계양점
				판매일 2025-09-04 14:25:08
				거래번호: 000123
				품목명 수량 금액
				돼지고기 500g 2 12,000
				고추장 500mL 1팩 5,000
				""");
		var scan = mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].amount").value(1000))
				.andExpect(jsonPath("$.items[1].unit").value("PACK"))
				.andReturn();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isZero();

		var mapper = JsonMapper.builder().build();
		var result = mapper.readTree(scan.getResponse().getContentAsByteArray());
		var payload = mapper.createObjectNode();
		for (String field : new String[]{"storeName", "storeCode", "purchasedOn", "purchasedTime", "receiptNumber"}) {
			payload.set(field, result.get(field));
		}
		var items = payload.putArray("items");
		for (var scanned : result.get("items")) {
			var item = items.addObject();
			for (String field : new String[]{"name", "categoryCode", "storage", "price", "expirationDate",
					"purchaseQuantity", "volume"}) {
				item.set(field, scanned.get(field));
			}
		}
		mockMvc.perform(multipart("/api/v1/user/receipts").file(receiptImage()).file(
				new MockMultipartFile("payload", "", "application/json", mapper.writeValueAsBytes(payload)))
				.cookie(accessToken()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ingredientCount").value(2));

		assertThat(jdbcTemplate.queryForList("SELECT unit FROM ingredients ORDER BY id", String.class))
				.containsExactly("G", "PACK");
		assertThat(jdbcTemplate.queryForList("SELECT amount FROM ingredients ORDER BY id", BigDecimal.class))
				.satisfiesExactly(amount -> assertThat(amount).isEqualByComparingTo("1000"),
						amount -> assertThat(amount).isEqualByComparingTo("1"));
		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECEIPT_DUPLICATE"));
	}

	@ParameterizedTest
	@CsvSource(value = {
		"14:25:08 | '' | 양파",       // 번호 누락: 같은 품목 목록으로 확인
		"'' | 000123 | 양파",         // 시각 누락: 같은 구매일과 영수증 번호로 확인
		"'' | '' | 양파",             // 번호와 시각 모두 누락
		"'' | 000123 | 당근"          // 같은 번호와 날짜라면 품목 판독 차이가 있어도 중복
	}, delimiter = '|')
	void OCR이_번호나_시각을_읽지_못해도_이미_등록한_영수증을_찾는다(
			String scannedTime, String scannedNumber, String scannedVegetable) throws Exception {
		registerExistingReceipt();
		stubDuplicateCandidate(scannedTime, scannedNumber, scannedVegetable);

		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECEIPT_DUPLICATE"));
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("""
				SELECT count(*) FROM events
				WHERE event_name = 'receipt_scan_failed' AND properties->>'error_code' = 'RECEIPT_DUPLICATE'
				""", Long.class)).isEqualTo(1);
	}

	@ParameterizedTest
	@CsvSource(value = {
		"14:25:08 | '' | 당근",       // 번호가 없고 품목이 다른 영수증
		"'' | 000456 | 양파",         // 시각이 없고 번호가 다른 영수증
		"16:00:00 | 000123 | 양파"    // 번호는 같지만 구매 시각이 다른 영수증
	}, delimiter = '|')
	void 대체_비교_조건이_다른_영수증은_스캔할_수_있다(
			String scannedTime, String scannedNumber, String scannedVegetable) throws Exception {
		registerExistingReceipt();
		stubDuplicateCandidate(scannedTime, scannedNumber, scannedVegetable);

		mockMvc.perform(multipart(SCAN_PATH).file(receiptImage()).cookie(accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2));
	}

	private void registerExistingReceipt() throws Exception {
		var payload = new MockMultipartFile("payload", "", "application/json", """
				{
				  "storeName":"롯데마트 계양점", "purchasedOn":"2025-09-04",
				  "purchasedTime":"14:25:08", "receiptNumber":"000123",
				  "items":[
				    {"name":"양파", "categoryCode":"VEGETABLE", "storage":"PANTRY", "purchaseQuantity":1},
				    {"name":"우유", "categoryCode":"DAIRY_EGG", "storage":"FRIDGE", "purchaseQuantity":2}
				  ]
				}
				""".getBytes(StandardCharsets.UTF_8));
		mockMvc.perform(multipart("/api/v1/user/receipts").file(receiptImage()).file(payload).cookie(accessToken()))
				.andExpect(status().isCreated());
	}

	private void stubDuplicateCandidate(String scannedTime, String scannedNumber, String scannedVegetable) {
		when(receiptText.read(any(byte[].class), eq(Duration.ofSeconds(30)))).thenReturn("""
				상호: 롯데마트 계양점
				판매일 2025-09-04 %s
				%s품목명 수량 금액
				우유 2개 5,000
				%s 1개 1,000
				""".formatted(scannedTime, scannedNumber.isEmpty() ? "" : "거래번호: " + scannedNumber + "\n",
				scannedVegetable));
	}

	private void stubReceiptText(String purchaseDateLine) {
		String store = purchaseDateLine.isEmpty() ? "동네마트" : "롯데마트 계양점";
		when(receiptText.read(any(byte[].class), eq(Duration.ofSeconds(30))))
				.thenReturn("상호: " + store + "\n" + purchaseDateLine + "\n품목명 수량 금액\n우유 2개 5,000\n");
	}

	private MockMultipartFile receiptImage() {
		return new MockMultipartFile("image", "receipt.jpg", "image/jpeg",
				new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
	}

	private void assertFailureEvent(String errorCode) {
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForList("""
				SELECT event_name, user_id, properties->>'error_code' AS error_code,
				       jsonb_typeof(properties->'duration_ms') AS duration_type
				FROM events
				""")).singleElement().satisfies(event -> {
					assertThat(event).containsEntry("event_name", "receipt_scan_failed")
							.containsEntry("user_id", userId)
							.containsEntry("error_code", errorCode)
							.containsEntry("duration_type", "number");
				});
	}
}
