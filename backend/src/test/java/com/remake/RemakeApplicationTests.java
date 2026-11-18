package com.remake;

import org.junit.jupiter.api.Test;

import com.remake.support.IntegrationTestSupport;

// 앱이 뜨는지(마이그레이션, 엔티티 검사, Bean 연결) 확인한다. 다른 통합 테스트와 같은 앱을 쓰므로 기동이 늘지 않는다.
class RemakeApplicationTests extends IntegrationTestSupport {

	@Test
	void contextLoads() {
	}
}
