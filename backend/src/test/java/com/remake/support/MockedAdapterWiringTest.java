package com.remake.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * IntegrationTestSupport가 @MockitoBean으로 바꾼 외부 어댑터는 테스트에서 진짜 생성자가 실행되지 않는다.
 * 그래서 어댑터가 받는 Bean(예: RestClient.Builder)이 운영 앱에 없어도 다른 테스트는 모두 통과한다.
 * 이 테스트는 가짜로 바꾼 어댑터마다 생성자 인자를 앱에서 찾을 수 있는지 확인한다.
 * 계기: #132 머지 뒤 spring-boot-starter-restclient가 없어 서버에서 앱이 뜨지 않음(Backend CD 실패).
 */
class MockedAdapterWiringTest extends IntegrationTestSupport {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void 가짜로_바꾼_어댑터의_생성자_인자는_앱에_Bean으로_있다() {
		List<Class<?>> adapters = mockedAdapterTypes();
		assertThat(adapters).as("IntegrationTestSupport의 @MockitoBean 필드").isNotEmpty();

		// Spring이 생성자를 부를 때와 같은 방식으로 인자를 찾는다(@Value·@Qualifier·Optional·List 처리, Bean 여러 개면 실패).
		AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
		List<String> unresolved = new ArrayList<>();
		for (Class<?> adapter : adapters) {
			Constructor<?> constructor = singleConstructor(adapter);
			for (int i = 0; i < constructor.getParameterCount(); i++) {
				MethodParameter parameter = new MethodParameter(constructor, i);
				try {
					beanFactory.resolveDependency(new DependencyDescriptor(parameter, true), null);
				} catch (BeansException e) {
					unresolved.add(adapter.getSimpleName() + " 생성자 인자 " + ResolvableType.forMethodParameter(parameter)
							+ ": " + e.getClass().getSimpleName());
				}
			}
		}

		assertThat(unresolved).as("앱에서 찾지 못한 생성자 인자 (의존성 누락 또는 같은 타입 Bean 여러 개)").isEmpty();
	}

	private static List<Class<?>> mockedAdapterTypes() {
		return Arrays.stream(IntegrationTestSupport.class.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(MockitoBean.class))
				.<Class<?>>map(Field::getType)
				.toList();
	}

	private static Constructor<?> singleConstructor(Class<?> type) {
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		assertThat(constructors).as(type.getSimpleName() + "는 생성자 주입용 생성자 하나만 둔다").hasSize(1);
		return constructors[0];
	}
}
