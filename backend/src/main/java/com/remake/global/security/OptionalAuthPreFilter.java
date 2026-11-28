package com.remake.global.security;

import jakarta.servlet.Filter;

/**
 * {@link AuthCookieFilter} 앞에 끼울 수 있는 선택 필터.
 * 운영·로컬에는 빈이 없고, 통합 테스트만 제공한다.
 */
public interface OptionalAuthPreFilter extends Filter {
}
