package com.remake.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.auth.application.port.out.JwtTokenPort;
import com.remake.receipt.application.port.in.ReceiptHistoryUseCase;
import com.remake.receipt.application.port.in.RetryReceiptImageDeletionUseCase;
import com.remake.support.IntegrationTestSupport;
import com.remake.user.domain.UserRole;

class ReceiptHistoryApiTest extends IntegrationTestSupport {

	private static final String ROOT = "/api/v1/user/receipts";
	private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0,
			0, 16, 'J', 'F', 'I', 'F', 0};
	@Autowired
	private JwtTokenPort tokens;
	@Autowired
	private RetryReceiptImageDeletionUseCase imageDeletion;
	@Autowired
	private ReceiptHistoryUseCase history;

	@Test
	void 본인_영수증의_월별_합계와_주차를_반환한다() throws Exception {
		Long owner = createUser();
		receipt(owner, "2026-03-01", "LOTTE_MART", 3000L);
		receipt(owner, "2026-03-02", null, 2000L);
		receipt(owner, "2026-02-03", "CU", 1000L);
		receipt(createUser(), "2026-03-02", "CU", 9000L);
		mockMvc.perform(get(ROOT).cookie(token(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.month").value("2026-03"))
				.andExpect(jsonPath("$.totalAmount").value(5000))
				.andExpect(jsonPath("$.averageDiffAmount").value(4000))
				.andExpect(jsonPath("$.weeks[0].label").value("2주차"))
				.andExpect(jsonPath("$.weeks[0].totalAmount").value(2000))
				.andExpect(jsonPath("$.weeks[0].receipts[0].storeCode").doesNotExist())
				.andExpect(jsonPath("$.weeks[1].startDate").value("2026-03-01"))
				.andExpect(jsonPath("$.weeks[1].endDate").value("2026-03-01"))
				.andExpect(jsonPath("$.weeks[1].receipts[0].storeCode").value("LOTTE_MART"));
		mockMvc.perform(get(ROOT).param("month", "2025-10").cookie(token(owner)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(0))
				.andExpect(jsonPath("$.weeks").isEmpty());
		mockMvc.perform(get(ROOT).param("month", "2025-09").cookie(token(owner)))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get(ROOT).param("month", "2026-04").cookie(token(owner)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 소유자만_상세와_사진을_조회하고_삭제할_수_있다() throws Exception {
		Long owner = createUser();
		Long id = receipt(owner, "2026-03-02", null, 2000L);
		Long stranger = createUser();
		String url = ROOT + "/" + id;
		mockMvc.perform(get(url).cookie(token(owner))).andExpect(status().isOk())
				.andExpect(jsonPath("$.imageUrl").value(url + "/image"))
				.andExpect(jsonPath("$.storeCode").doesNotExist())
				.andExpect(jsonPath("$.items[0].name").value("양파"))
				.andExpect(jsonPath("$.items[0].categoryCode").value("VEGETABLE"))
				.andExpect(jsonPath("$.totalAmount").value(2000));
		mockMvc.perform(get(url + "/image").cookie(token(owner))).andExpect(status().isOk())
				.andExpect(content().contentType("image/jpeg")).andExpect(content().bytes(JPEG));
		mockMvc.perform(get(url).cookie(token(stranger))).andExpect(status().isNotFound());
		mockMvc.perform(get(url + "/image").cookie(token(stranger))).andExpect(status().isNotFound());
		mockMvc.perform(delete(url).cookie(token(stranger))).andExpect(status().isNotFound());
	}

	@Test
	void 삭제를_롤백하면_사진이_남고_커밋하면_냉장고_재료만_남는다() throws Exception {
		Long owner = createUser();
		Long id = receipt(owner, "2026-03-02", "CU", 2000L);
		Long itemId = jdbcTemplate.queryForObject("SELECT id FROM receipt_items WHERE receipt_id = ?", Long.class, id);
		Long ingredientId = createIngredient(owner, "VEGETABLE", "양파", LocalDate.of(2026, 3, 10));
		jdbcTemplate.update("UPDATE ingredients SET receipt_item_id = ? WHERE id = ?", itemId, ingredientId);
		String key = jdbcTemplate.queryForObject("SELECT image_key FROM receipts WHERE id = ?", String.class, id);

		transactionTemplate.executeWithoutResult(tx -> {
			history.delete(owner, id);
			tx.setRollbackOnly();
		});
		assertThat(images.read(key)).isEqualTo(JPEG);
		assertThat(pendingImageDeletions()).isZero();

		mockMvc.perform(delete(ROOT + "/" + id).cookie(token(owner))).andExpect(status().isNoContent());
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipt_items WHERE id = ?", Long.class, itemId))
				.isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT receipt_item_id FROM ingredients WHERE id = ?",
				Long.class, ingredientId)).isNull();
		assertThatThrownBy(() -> images.read(key)).isInstanceOf(NoSuchFileException.class);
		mockMvc.perform(get(ROOT).cookie(token(owner))).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAmount").value(0));
	}

	@Test
	void 사진_삭제에_실패하면_영수증_삭제는_유지하고_사진을_다시_삭제한다() throws Exception {
		Long owner = createUser();
		Long id = receipt(owner, "2026-03-02", "CU", 2000L);
		String key = jdbcTemplate.queryForObject("SELECT image_key FROM receipts WHERE id = ?", String.class, id);
		doThrow(new IOException("temporary storage failure")).doCallRealMethod().when(images).delete(key);

		mockMvc.perform(delete(ROOT + "/" + id).cookie(token(owner))).andExpect(status().isNoContent());
		mockMvc.perform(get(ROOT + "/" + id + "/image").cookie(token(owner))).andExpect(status().isNotFound());
		assertThat(images.read(key)).isEqualTo(JPEG);
		assertThat(pendingImageDeletions()).isEqualTo(1);

		imageDeletion.retryPending();

		assertThatThrownBy(() -> images.read(key)).isInstanceOf(NoSuchFileException.class);
		assertThat(pendingImageDeletions()).isZero();
	}

	private long pendingImageDeletions() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM receipt_image_deletions", Long.class);
	}

	private Long receipt(Long owner, String purchasedOn, String storeCode, Long price) throws Exception {
		String key = images.save(new ByteArrayInputStream(JPEG));
		Long id = jdbcTemplate.queryForObject("""
				INSERT INTO receipts (user_id, purchased_on, store_name, store_code, image_key, created_at)
				VALUES (?, ?, '동네마트', ?, ?, ?) RETURNING id
				""", Long.class, owner, LocalDate.parse(purchasedOn), storeCode, key, LocalDateTime.now(clock));
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
