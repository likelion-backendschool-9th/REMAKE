package com.remake.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.auth.application.port.out.JwtTokenPort;
import com.remake.receipt.application.port.in.ExpireOldReceiptsUseCase;
import com.remake.receipt.application.port.in.RetryReceiptImageDeletionUseCase;
import com.remake.receipt.domain.ReceiptRetention;
import com.remake.share.application.port.out.LoadSharedRecipePhotoPort;
import com.remake.share.application.port.out.SharedRecipePhotoStoragePort;
import com.remake.support.IntegrationTestSupport;
import com.remake.support.MutableClock;
import com.remake.user.domain.UserRole;

class ReceiptExpirationBatchTest extends IntegrationTestSupport {

	private static final String ROOT = "/api/v1/user/receipts";
	private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0,
			0, 16, 'J', 'F', 'I', 'F', 0};

	@Autowired
	private ExpireOldReceiptsUseCase expireOldReceipts;
	@Autowired
	private RetryReceiptImageDeletionUseCase imageDeletion;
	@Autowired
	private JwtTokenPort tokens;
	@Autowired
	private SharedRecipePhotoStoragePort sharedPhotos;
	@Autowired
	private LoadSharedRecipePhotoPort sharedPhotoLoader;

	@Test
	void 구매일_6개월_경계에서_지난_영수증만_지우고_냉장고_재료는_남긴다() throws Exception {
		// 기본 시계 2026-03-04 → cutoff 2025-09-04. 당일은 보관, 하루 전은 삭제.
		LocalDate keepOn = LocalDate.of(2025, 9, 4);
		LocalDate deleteOn = LocalDate.of(2025, 9, 3);
		assertThat(ReceiptRetention.earliestAllowedPurchaseDate(LocalDate.now(clock))).isEqualTo(keepOn);

		Long owner = createUser();
		Long keepId = receipt(owner, keepOn, 1500L);
		Long deleteId = receipt(owner, deleteOn, 2500L);
		Long itemId = jdbcTemplate.queryForObject("SELECT id FROM receipt_items WHERE receipt_id = ?", Long.class,
				deleteId);
		Long ingredientId = createIngredient(owner, "VEGETABLE", "양파", LocalDate.of(2026, 3, 10));
		jdbcTemplate.update("UPDATE ingredients SET receipt_item_id = ? WHERE id = ?", itemId, ingredientId);
		String deletedKey = imageKey(deleteId);
		String keptKey = imageKey(keepId);

		assertThat(expireOldReceipts.expireOldReceipts()).isEqualTo(1);

		assertThat(receiptCount(deleteId)).isZero();
		assertThat(receiptCount(keepId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipt_items WHERE receipt_id = ?", Long.class,
				deleteId)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients WHERE id = ?", Long.class,
				ingredientId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT receipt_item_id FROM ingredients WHERE id = ?", Long.class,
				ingredientId)).isNull();
		assertThatThrownBy(() -> images.read(deletedKey)).isInstanceOf(NoSuchFileException.class);
		assertThat(images.read(keptKey)).isEqualTo(JPEG);

		// 목록은 최근 6개월만 받으므로, 보관한 9월 영수증이 보이도록 시계를 그날로 맞춘다.
		clock.setNow(ZonedDateTime.of(2025, 9, 4, 10, 0, 0, 0, MutableClock.ZONE));
		mockMvc.perform(get(ROOT).cookie(token(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.month").value("2025-09"))
				.andExpect(jsonPath("$.totalAmount").value(1500))
				.andExpect(jsonPath("$.weeks[0].receipts[0].id").value(keepId.intValue()));
		mockMvc.perform(get(ROOT + "/" + deleteId).cookie(token(owner))).andExpect(status().isNotFound());
	}

	@Test
	void 월말과_윤년_경계에서도_minusMonths_6_규칙을_따른다() throws Exception {
		clock.setNow(ZonedDateTime.of(2026, 8, 31, 3, 0, 0, 0, MutableClock.ZONE));
		LocalDate monthEndCutoff = ReceiptRetention.earliestAllowedPurchaseDate(LocalDate.now(clock));
		assertThat(monthEndCutoff).isEqualTo(LocalDate.of(2026, 2, 28));

		Long owner = createUser();
		Long keepMonthEnd = receipt(owner, LocalDate.of(2026, 2, 28), 1000L);
		Long deleteMonthEnd = receipt(owner, LocalDate.of(2026, 2, 27), 2000L);
		assertThat(expireOldReceipts.expireOldReceipts()).isEqualTo(1);
		assertThat(receiptCount(keepMonthEnd)).isEqualTo(1);
		assertThat(receiptCount(deleteMonthEnd)).isZero();

		clock.setNow(ZonedDateTime.of(2024, 8, 29, 3, 0, 0, 0, MutableClock.ZONE));
		LocalDate leapCutoff = ReceiptRetention.earliestAllowedPurchaseDate(LocalDate.now(clock));
		assertThat(leapCutoff).isEqualTo(LocalDate.of(2024, 2, 29));

		Long leapOwner = createUser();
		Long keepLeap = receipt(leapOwner, LocalDate.of(2024, 2, 29), 1000L);
		Long deleteLeap = receipt(leapOwner, LocalDate.of(2024, 2, 28), 2000L);
		assertThat(expireOldReceipts.expireOldReceipts()).isEqualTo(1);
		assertThat(receiptCount(keepLeap)).isEqualTo(1);
		assertThat(receiptCount(deleteLeap)).isZero();
	}

	@Test
	void 구매일_6개월이_지난_최초_반영_기록도_지운다() {
		Long owner = createUser();
		LocalDate keepOn = LocalDate.of(2025, 9, 4);
		LocalDate deleteOn = LocalDate.of(2025, 9, 3);
		LocalDateTime now = LocalDateTime.now(clock);
		jdbcTemplate.update("""
				INSERT INTO receipt_first_applications
				(user_id, store_name, purchased_on, receipt_number, created_at)
				VALUES (?, '동네마트', ?, 'KEEP-1', ?), (?, '동네마트', ?, 'DROP-1', ?)
				""", owner, keepOn, now, owner, deleteOn, now);

		expireOldReceipts.expireOldReceipts();

		assertThat(jdbcTemplate.queryForList("SELECT receipt_number FROM receipt_first_applications", String.class))
				.containsExactly("KEEP-1");
	}

	@Test
	void 같은_날_다시_실행해도_오류_없이_추가_삭제가_없다() throws Exception {
		Long owner = createUser();
		receipt(owner, LocalDate.of(2025, 9, 3), 1000L);
		receipt(owner, LocalDate.of(2025, 9, 4), 2000L);

		assertThat(expireOldReceipts.expireOldReceipts()).isEqualTo(1);
		assertThat(expireOldReceipts.expireOldReceipts()).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isEqualTo(1);
	}

	@Test
	void 이미지_삭제에_실패해도_영수증은_지우고_재시도로_파일을_지운다() throws Exception {
		Long owner = createUser();
		Long id = receipt(owner, LocalDate.of(2025, 9, 3), 1000L);
		String key = imageKey(id);
		doThrow(new IOException("temporary storage failure")).doCallRealMethod().when(images).delete(key);

		assertThat(expireOldReceipts.expireOldReceipts()).isEqualTo(1);
		assertThat(receiptCount(id)).isZero();
		assertThat(images.read(key)).isEqualTo(JPEG);
		assertThat(pendingImageDeletions()).isEqualTo(1);

		imageDeletion.retryPending();

		assertThatThrownBy(() -> images.read(key)).isInstanceOf(NoSuchFileException.class);
		assertThat(pendingImageDeletions()).isZero();
	}

	@Test
	void 같은_저장소의_공유_요리_사진은_남기고_영수증_이미지만_지운다() throws Exception {
		Long owner = createUser();
		Long expiredId = receipt(owner, LocalDate.of(2025, 9, 3), 1000L);
		String receiptKey = imageKey(expiredId);

		String sharedKey = "shared-recipes/" + UUID.randomUUID() + ".jpg";
		sharedPhotos.store(sharedKey, JPEG, "image/jpeg");
		assertThat(images.read(receiptKey)).isEqualTo(JPEG);
		assertThat(sharedPhotoLoader.load(sharedKey)).isPresent();

		assertThat(expireOldReceipts.expireOldReceipts()).isEqualTo(1);

		assertThat(receiptCount(expiredId)).isZero();
		assertThatThrownBy(() -> images.read(receiptKey)).isInstanceOf(NoSuchFileException.class);
		try (var shared = sharedPhotoLoader.load(sharedKey).orElseThrow()) {
			assertThat(shared.stream().readAllBytes()).isEqualTo(JPEG);
		}
	}

	@Test
	void 이미지_파일이_없어도_DB는_지우고_작업이_계속된다() throws Exception {
		Long owner = createUser();
		Long missingImage = receipt(owner, LocalDate.of(2025, 9, 2), 1000L);
		Long nextExpired = receipt(owner, LocalDate.of(2025, 9, 3), 2000L);
		String missingKey = imageKey(missingImage);
		String nextKey = imageKey(nextExpired);
		images.delete(missingKey);

		assertThat(expireOldReceipts.expireOldReceipts()).isEqualTo(2);
		assertThat(receiptCount(missingImage)).isZero();
		assertThat(receiptCount(nextExpired)).isZero();
		assertThatThrownBy(() -> images.read(nextKey)).isInstanceOf(NoSuchFileException.class);
	}

	private long pendingImageDeletions() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM receipt_image_deletions", Long.class);
	}

	private long receiptCount(Long id) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM receipts WHERE id = ?", Long.class, id);
	}

	private String imageKey(Long id) {
		return jdbcTemplate.queryForObject("SELECT image_key FROM receipts WHERE id = ?", String.class, id);
	}

	private Long receipt(Long owner, LocalDate purchasedOn, Long price) throws Exception {
		String key = images.save(new ByteArrayInputStream(JPEG));
		Long id = jdbcTemplate.queryForObject("""
				INSERT INTO receipts (user_id, purchased_on, store_name, store_code, image_key, created_at)
				VALUES (?, ?, '동네마트', 'CU', ?, ?) RETURNING id
				""", Long.class, owner, purchasedOn, key, LocalDateTime.now(clock));
		jdbcTemplate.update("""
				INSERT INTO receipt_items (receipt_id, category_id, name, storage, price, created_at)
				VALUES (?, ?, '양파', 'PANTRY', ?, ?)
				""", id, categoryId("VEGETABLE"), price, LocalDateTime.now(clock));
		return id;
	}

	private Cookie token(Long userId) {
		return new Cookie("ACCESS_TOKEN", tokens.issueAccessToken(userId, UserRole.USER));
	}
}
