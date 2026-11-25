package com.remake.receipt.adapter.out.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

import tools.jackson.databind.json.JsonMapper;

class GeminiReceiptTextAdapterTest {

	private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
	private final HttpClient http = mock(HttpClient.class);

	@Test
	@SuppressWarnings("unchecked")
	void FlashLite에_이미지를_보내고_최종_판독_텍스트만_가져온다() {
		HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(200);
		when(response.body()).thenReturn("""
				{"candidates":[{"finishReason":"STOP","content":{"parts":[
				  {"thought":true,"text":"ignored"},
				  {"text":"구매일: 2026-11-24"}
				]}}]}
				""");
		when(http.sendAsync(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
				.thenReturn(CompletableFuture.completedFuture(response));
		var adapter = new GeminiReceiptTextAdapter("test-key", JsonMapper.builder().build(), http);

		assertThat(adapter.read(JPEG, Duration.ofSeconds(2))).isEqualTo("구매일: 2026-11-24");

		var request = ArgumentCaptor.forClass(HttpRequest.class);
		verify(http).sendAsync(request.capture(), any());
		assertThat(request.getValue().uri().getPath()).endsWith("/gemini-3.5-flash-lite:generateContent");
		assertThat(request.getValue().headers().firstValue("x-goog-api-key")).contains("test-key");
		assertThat(request.getValue().timeout()).contains(Duration.ofSeconds(2));
	}

	@Test
	void 키가_없으면_외부_호출을_하지_않는다() {
		var adapter = new GeminiReceiptTextAdapter("", JsonMapper.builder().build(), http);

		assertThatThrownBy(() -> adapter.read(JPEG, Duration.ofSeconds(2)))
				.isInstanceOfSatisfying(BusinessException.class,
						error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.OCR_UPSTREAM_ERROR));
		verifyNoInteractions(http);
	}
	@Test
	void Gemini_제한_시간이_지나면_OCR_TIMEOUT으로_응답한다() {
		when(http.sendAsync(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
				.thenReturn(CompletableFuture.failedFuture(new HttpTimeoutException("timeout")));
		var adapter = new GeminiReceiptTextAdapter("test-key", JsonMapper.builder().build(), http);

		assertThatThrownBy(() -> adapter.read(JPEG, Duration.ofSeconds(2)))
				.isInstanceOfSatisfying(BusinessException.class,
						error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.OCR_TIMEOUT));
	}

	@Test
	@SuppressWarnings("unchecked")
	void Gemini_요청_한도에_걸리면_OCR_UPSTREAM_ERROR로_응답한다() {
		HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(429);
		when(http.sendAsync(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
				.thenReturn(CompletableFuture.completedFuture(response));
		var adapter = new GeminiReceiptTextAdapter("test-key", JsonMapper.builder().build(), http);

		assertThatThrownBy(() -> adapter.read(JPEG, Duration.ofSeconds(2)))
				.isInstanceOfSatisfying(BusinessException.class,
						error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.OCR_UPSTREAM_ERROR));
	}
}
