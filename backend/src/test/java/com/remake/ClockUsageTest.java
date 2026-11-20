package com.remake;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.remake.global.config.ClockConfig;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

// 현재 시각은 주입받은 Clock으로만 읽는다(docs/agent/backend-testing.md "Production code rules for testability").
// Clock 없이 시각을 읽으면 운영에서는 정상으로 보이지만, 테스트의 시험용 시계(MutableClock)를 따르지 않아
// 날짜 경계(00:00 초기화, D-3, 월요일 집계)를 다루는 테스트가 돌리는 시각에 따라 통과하거나 실패한다.
// Spring을 띄우지 않고 컴파일된 운영 코드(src/main)와 마이그레이션 SQL을 읽어 검사한다.
class ClockUsageTest {

	private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("com.remake");

	// now(Clock)만 허용하고 나머지 now(), now(ZoneId)는 모두 실제 시계를 읽는다.
	private static final Set<String> TIME_TYPES = Set.of(
			"java.time.LocalDateTime", "java.time.LocalDate", "java.time.LocalTime", "java.time.Instant",
			"java.time.ZonedDateTime", "java.time.OffsetDateTime", "java.time.OffsetTime",
			"java.time.Year", "java.time.YearMonth", "java.time.MonthDay");

	// 실제 시계로 Clock을 만드는 메서드. ClockConfig만 쓸 수 있다.
	private static final Set<String> SYSTEM_CLOCK_FACTORIES = Set.of(
			"system", "systemUTC", "systemDefaultZone", "tickMillis", "tickSeconds", "tickMinutes");

	private static final String HOW_TO_FIX = "현재 시각은 주입받은 Clock으로 읽어야 테스트의 시험용 시계를 따른다. "
			+ "LocalDate.now(clock)처럼 Clock을 넘긴다. 걸린 시간 재기는 System.nanoTime()을 쓴다";

	@Test
	void 운영_코드는_Clock_없이_현재_시각을_읽지_않는다() {
		classes().should(notReadTheSystemClock())
				.because(HOW_TO_FIX)
				.check(PRODUCTION_CLASSES);
	}

	@Test
	void 운영_코드는_Hibernate의_시각_자동_입력을_쓰지_않는다() {
		String[] annotations = {"org.hibernate.annotations.CreationTimestamp",
				"org.hibernate.annotations.UpdateTimestamp", "org.hibernate.annotations.CurrentTimestamp"};
		String because = "실제 시계를 써서 시험용 시계를 따르지 않는다. "
				+ "생성·수정 시각은 BaseCreatedEntity·BaseTimeEntity를 상속해 JPA 감사(ClockConfig의 Clock)로 넣는다";
		for (String annotation : annotations) {
			noFields().should().beAnnotatedWith(annotation).because(because).allowEmptyShould(true)
					.check(PRODUCTION_CLASSES);
			noMethods().should().beAnnotatedWith(annotation).because(because).allowEmptyShould(true)
					.check(PRODUCTION_CLASSES);
		}
	}

	// SQL의 시각 함수는 DB 서버의 실제 시계를 읽는다. 시각은 앱이 Clock으로 구해 값으로 넣는다.
	// 대상은 db/migration의 SQL 파일(DEFAULT now() 등). Repository의 @Query 문자열은 아직 검사하지 않는다.
	private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

	private static final Pattern SQL_TIME_FUNCTION = Pattern.compile(
			"\\b(now|clock_timestamp|statement_timestamp|transaction_timestamp|timeofday)\\s*\\("
					+ "|\\b(current_timestamp|current_date|current_time|localtimestamp|localtime)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern SQL_COMMENT = Pattern.compile("--[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);

	@Test
	void 마이그레이션_SQL은_DB의_현재_시각_함수를_쓰지_않는다() throws IOException {
		// 폴더를 옮겼는데 검사가 빈 폴더를 보고 통과하는 일을 막는다.
		assertThat(MIGRATION_DIR).isDirectory();
		List<String> violations = new ArrayList<>();
		try (Stream<Path> files = Files.walk(MIGRATION_DIR)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".sql")).sorted().toList()) {
				String[] lines = withoutComments(Files.readString(file)).split("\n", -1);
				for (int i = 0; i < lines.length; i++) {
					Matcher matcher = SQL_TIME_FUNCTION.matcher(lines[i]);
					if (matcher.find()) {
						violations.add(file.getFileName() + ":" + (i + 1) + " " + matcher.group().trim());
					}
				}
			}
		}
		assertThat(violations)
				.as("DB의 현재 시각 함수(DEFAULT now() 등)는 시험용 시계를 따르지 않는다. "
						+ "시각 컬럼은 기본값 없이 두고 앱이 값을 넣는다(BaseCreatedEntity·BaseTimeEntity)")
				.isEmpty();
	}

	// 주석 안의 글자는 검사하지 않는다. 줄 번호가 바뀌지 않도록 주석 안의 줄바꿈은 남긴다.
	private static String withoutComments(String sql) {
		Matcher matcher = SQL_COMMENT.matcher(sql);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String newlines = matcher.group().replaceAll("[^\\n]", "");
			matcher.appendReplacement(result, newlines);
		}
		matcher.appendTail(result);
		return result.toString();
	}

	// 메서드 호출, 생성자 호출, 메서드 참조(LocalDateTime::now)를 모두 본다.
	private static ArchCondition<JavaClass> notReadTheSystemClock() {
		return new ArchCondition<>("not read the current time without the injected Clock") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				for (JavaAccess<?> access : javaClass.getAccessesFromSelf()) {
					if (access.getTarget() instanceof CodeUnitAccessTarget target
							&& readsSystemClock(javaClass, target)) {
						events.add(SimpleConditionEvent.violated(access, access.getDescription()));
					}
				}
			}
		};
	}

	private static boolean readsSystemClock(JavaClass origin, CodeUnitAccessTarget target) {
		String owner = target.getOwner().getName();
		String name = target.getName();
		List<JavaClass> parameters = target.getRawParameterTypes();
		if (TIME_TYPES.contains(owner) && name.equals("now")) {
			return !(parameters.size() == 1 && parameters.get(0).isEquivalentTo(Clock.class));
		}
		if (owner.equals("java.lang.System") && name.equals("currentTimeMillis")) {
			return true;
		}
		if (owner.equals("java.util.Date") && name.equals("<init>") && parameters.isEmpty()) {
			return true;
		}
		if ((owner.equals("java.time.Clock") && SYSTEM_CLOCK_FACTORIES.contains(name))
				|| (owner.equals("java.time.InstantSource") && name.equals("system"))) {
			return !origin.isEquivalentTo(ClockConfig.class);
		}
		return false;
	}
}
