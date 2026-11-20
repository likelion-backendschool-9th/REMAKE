package com.remake.global.common;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청마다 추적 ID를 만들어 로그(MDC)와 응답 헤더(X-Trace-Id)에 넣는다. 에러 응답의 traceId도 이 값이다.
 * 사용자가 에러를 알려 주면 이 ID로 서버 로그를 찾는다.
 * Security 필터보다 먼저 실행되도록 가장 앞 순서에 둔다(401/403 응답에도 traceId가 들어가게).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

	public static final String MDC_KEY = "traceId";
	public static final String HEADER = "X-Trace-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		// 밖에서 보낸 값은 믿지 않고 서버가 새로 만든다.
		String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		MDC.put(MDC_KEY, traceId);
		response.setHeader(HEADER, traceId);
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_KEY);
		}
	}
}
