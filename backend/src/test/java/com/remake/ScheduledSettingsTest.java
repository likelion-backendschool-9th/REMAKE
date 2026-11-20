package com.remake;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import com.remake.support.IntegrationTestSupport;

// 테스트에서는 스케줄러를 꺼 두므로 앱이 뜰 때 Spring이 @Scheduled 설정을 검사하지 않는다.
// 설정에 오타가 있으면 운영에서 앱이 뜨지 못하므로, Spring의 검사기로 같은 검사를 한다. 작업은 실행하지 않는다.
class ScheduledSettingsTest extends IntegrationTestSupport {

	@Autowired
	ConfigurableApplicationContext context;

	@Test
	void 모든_Scheduled_설정이_올바르다() {
		ScheduledAnnotationBeanPostProcessor checker = new ScheduledAnnotationBeanPostProcessor();
		// 앱이 뜰 때 Spring이 쓰는 것과 같은 방식으로 값을 푼다(${...} 설정 값과 #{...} SpEL).
		checker.setEmbeddedValueResolver(new EmbeddedValueResolver(context.getBeanFactory()));
		List<String> errors = new ArrayList<>();
		try {
			for (String name : context.getBeanDefinitionNames()) {
				Class<?> type = context.getType(name);
				if (type == null || !type.getName().startsWith("com.remake.")) {
					continue;
				}
				try {
					// 스케줄러를 연결하지 않았으므로 작업을 등록만 하고 실행하지 않는다.
					checker.postProcessAfterInitialization(context.getBean(name), name);
				} catch (RuntimeException e) {
					errors.add(name + ": " + e.getMessage());
				}
			}
		} finally {
			checker.destroy();
		}
		assertThat(errors).isEmpty();
	}
}
