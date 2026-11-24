package com.remake.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.remake.auth.application.port.out.JwtTokenPort;
import com.remake.global.config.AuthProperties;
import com.remake.global.config.UploadProperties;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.share.application.port.in.PublishSharedRecipeUseCase;
import com.remake.share.application.port.in.SharedRecipePhotoUpload;
import com.remake.support.IntegrationTestSupport;
import com.remake.user.domain.UserRole;
import jakarta.servlet.http.Cookie;

class SharedRecipePhotoApiTest extends IntegrationTestSupport {
	private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1};
	@Autowired private PublishSharedRecipeUseCase publish;
	@Autowired private UploadProperties uploads;
	@Autowired private JwtTokenPort tokens;
	@Autowired private AuthProperties auth;

	private Long post(Long user) {
		return publish.publish(user, createRecipe(user), "익명 사진", new SharedRecipePhotoUpload(
				"image/jpeg", JPEG.length, () -> new ByteArrayInputStream(JPEG)));
	}
	private String url(Long id) { return "/api/v1/user/shared-recipe-photos/" + id; }

	@Test
	void 실제_액세스쿠키로_다른회원도_공개사진을_조회하고_캐시하지않는다() throws Exception {
		Long id = post(createUser());
		String access = tokens.issueAccessToken(createUser(), UserRole.USER);
		mockMvc.perform(get(url(id)).cookie(new Cookie(auth.accessCookieName(), access)))
				.andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"))
				.andExpect(content().bytes(JPEG)).andExpect(header().string("Content-Length", "4"))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"));
	}
	@Test
	void 비로그인과_만료쿠키는_이미지에도_401이다() throws Exception {
		Long user = createUser(); Long id = post(user);
		mockMvc.perform(get(url(id))).andExpect(status().isUnauthorized());
		String access = tokens.issueAccessToken(user, UserRole.USER);
		clock.advance(java.time.Duration.ofHours(1));
		mockMvc.perform(get(url(id)).cookie(new Cookie(auth.accessCookieName(), access)))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
	}
	@Test
	void 숨김과_없는글은_소유자도_404이다() throws Exception {
		Long user = createUser(); Long id = post(user);
		jdbcTemplate.update("UPDATE shared_recipes SET status = 'HIDDEN' WHERE id = ?", id);
		for (Long target : new Long[]{id, 999999L}) {
			mockMvc.perform(get(url(target)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
					.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHARED_RECIPE_NOT_FOUND"));
		}
	}
	@Test
	void 파일이_누락되면_내부경로없는_404다() throws Exception {
		Long user = createUser(); Long id = post(user);
		String key = jdbcTemplate.queryForObject("SELECT image_key FROM shared_recipes WHERE id = ?", String.class, id);
		Files.delete(Path.of(uploads.rootDirectory()).resolve(key));
		String body = mockMvc.perform(get(url(id)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHARED_RECIPE_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();
		assertThat(body).doesNotContain(key, uploads.rootDirectory());
	}
	@Test
	void 원본작성자_탈퇴후에도_공개사진은_유지한다() throws Exception {
		Long author = createUser(); Long id = post(author);
		jdbcTemplate.update("UPDATE users SET deleted_at = ? WHERE id = ?",
				java.time.LocalDateTime.now(clock), author);
		mockMvc.perform(get(url(id)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, createUser()))
				.andExpect(status().isOk()).andExpect(content().bytes(JPEG));
	}
}
