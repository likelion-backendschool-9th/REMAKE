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
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.remake.global.config.ClockConfig;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

// 현재 시각은 주입받은 Clock으로만 읽는다(docs/agent/backend-testing.md "Production code rules for testability").
// Clock 없이 시각을 읽으면 운영에서는 정상으로 보이지만, 테스트의 시험용 시계(MutableClock)를 따르지 않아
// 날짜 경계(00:00 초기화, D-3, 월요일 집계)를 다루는 테스트가 돌리는 시각에 따라 통과하거나 실패한다.
// Spring을 띄우지 않고 컴파일된 운영 코드(src/main, @Query 값 포함)와 마이그레이션 SQL을 읽어 검사한다.
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
	// 대상은 db/migration의 SQL 파일(DEFAULT now() 등)과 Repository의 @Query·@NativeQuery 값(value, countQuery).
	private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

	private static final Pattern SQL_TIME_FUNCTION = Pattern.compile(
			"\\b(now|clock_timestamp|statement_timestamp|transaction_timestamp|timeofday)\\s*\\("
					+ "|\\b(current_timestamp|current_date|current_time|localtimestamp|localtime)\\b",
			Pattern.CASE_INSENSITIVE);

	// JPQL(HQL)에만 있는 현재 시각 표현. Hibernate 7.4 HQL 문법(HqlParser의 currentDateFunction·instantFunction·
	// localDateFunction 등)에서 SQL_TIME_FUNCTION에 없는 것이고, 띄어 쓰는 형태(current date, local datetime)도 있다.
	// instant 같은 단어는 SQL에서 컬럼 이름일 수 있어 네이티브 쿼리와 마이그레이션에는 쓰지 않는다.
	// 필드(i.instant)·파라미터(:instant)와 날짜 값 리터럴(local datetime 2026-03-04 10:00)은 걸리지 않게 뺀다.
	private static final Pattern HQL_TIME_FUNCTION = Pattern.compile(
			"(?<![.:\\w])(current_instant|instant|local_date|local_time|local_datetime|offset_datetime"
					+ "|current\\s+(date|time|timestamp)|local\\s+(date|time|datetime)|offset\\s+datetime)\\b(?!\\s*\\d)",
			Pattern.CASE_INSENSITIVE);

	private static final String QUERY = "org.springframework.data.jpa.repository.Query";
	private static final String NATIVE_QUERY = "org.springframework.data.jpa.repository.NativeQuery";

	// PostgreSQL은 'now'::timestamp, 'today'::date 같은 특수 문자열 값도 실행 시각으로 바꾼다.
	private static final Pattern SQL_TIME_STRING = Pattern.compile(
			"'\\s*(now|today|tomorrow|yesterday)\\s*'", Pattern.CASE_INSENSITIVE);

	// 문자열('...', 안의 ''는 따옴표 한 개)을 먼저 알아봐야 문자열 안의 --를 주석으로 잘못 보지 않는다.
	// 문자열 부분은 점유 반복(*+)으로 쓴다. (?:[^']|'')* 처럼 쓰면 글자마다 재귀해서 긴 문자열에서 StackOverflowError가 난다.
	private static final Pattern SQL_TOKEN = Pattern.compile(
			"(?<string>'[^']*+(?:''[^']*+)*+')|(?<comment>--[^\\n]*|/\\*.*?\\*/)", Pattern.DOTALL);

	@Test
	void 마이그레이션_SQL은_DB의_현재_시각_함수를_쓰지_않는다() throws IOException {
		// 폴더를 옮겼는데 검사가 빈 폴더를 보고 통과하는 일을 막는다.
		assertThat(MIGRATION_DIR).isDirectory();
		List<String> violations = new ArrayList<>();
		try (Stream<Path> files = Files.walk(MIGRATION_DIR)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".sql")).sorted().toList()) {
				findSqlTimeUsages(file.getFileName().toString(), Files.readString(file), List.of(SQL_TIME_FUNCTION),
						violations);
			}
		}
		assertThat(violations)
				.as("DB의 현재 시각 함수(DEFAULT now(), 'now'::timestamp 등)는 시험용 시계를 따르지 않는다. "
						+ "시각 컬럼은 기본값 없이 두고 앱이 값을 넣는다(BaseCreatedEntity·BaseTimeEntity)")
				.isEmpty();
	}

	@Test
	void Repository_쿼리는_DB의_현재_시각_함수를_쓰지_않는다() {
		List<String> violations = new ArrayList<>();
		int queries = 0;
		for (JavaClass javaClass : PRODUCTION_CLASSES) {
			for (JavaMethod method : javaClass.getMethods()) {
				for (String annotationType : List.of(QUERY, NATIVE_QUERY)) {
					Optional<? extends JavaAnnotation<?>> annotation = method.tryGetAnnotationOfType(annotationType);
					if (annotation.isEmpty()) {
						continue;
					}
					queries++;
					boolean nativeQuery = annotationType.equals(NATIVE_QUERY)
							|| Boolean.TRUE.equals(annotation.get().get("nativeQuery").orElse(false));
					List<Pattern> functions = nativeQuery
							? List.of(SQL_TIME_FUNCTION)
							: List.of(SQL_TIME_FUNCTION, HQL_TIME_FUNCTION);
					for (String property : List.of("value", "countQuery")) {
						String query = (String) annotation.get().get(property).orElse("");
						findSqlTimeUsages(javaClass.getSimpleName() + "." + method.getName() + " " + property, query,
								functions, violations);
					}
				}
			}
		}
		// 어노테이션 이름이나 클래스 읽기가 바뀌었는데 검사가 쿼리를 하나도 못 보고 통과하는 일을 막는다.
		assertThat(queries).as("운영 코드에서 @Query를 찾지 못했다").isPositive();
		// 클래스·메서드를 읽는 순서가 실행마다 달라서 정렬해 보여 준다.
		assertThat(violations.stream().sorted().toList())
				.as("쿼리 안의 현재 시각 함수(CURRENT_DATE, now(), local date 등)는 DB 시계를 읽어 시험용 시계를 따르지 않는다. "
						+ "시각은 Clock으로 구해 파라미터로 넘긴다(예: LocalDate.now(clock)을 :today로)")
				.isEmpty();
	}

	// 주석은 지우고, 문자열은 값이 'now'·'today' 같은 특수 값인지만 본다.
	// 시각 함수는 주석과 문자열을 뺀 나머지에서 찾는다(설명 문자열 안의 now()는 걸리지 않게).
	// 줄 번호가 바뀌지 않도록 지운 자리의 줄바꿈은 남긴다. 위치는 "파일:줄" 또는 "클래스.메서드 속성:줄"이다.
	private static void findSqlTimeUsages(String location, String sql, List<Pattern> functions,
			List<String> violations) {
		Matcher token = SQL_TOKEN.matcher(sql);
		StringBuilder code = new StringBuilder();
		while (token.find()) {
			String text = token.group();
			if (token.group("string") != null && SQL_TIME_STRING.matcher(text).matches()) {
				violations.add(location + ":" + lineOf(sql, token.start()) + " " + text);
			}
			token.appendReplacement(code, text.replaceAll("[^\\n]", ""));
		}
		token.appendTail(code);
		// 줄을 나누지 않고 전체에서 찾는다. 띄어 쓰는 HQL 표현(current\ndate)이 줄을 넘어가도 잡는다.
		for (Pattern pattern : functions) {
			Matcher function = pattern.matcher(code);
			while (function.find()) {
				violations.add(location + ":" + lineOf(code.toString(), function.start()) + " "
						+ function.group().trim().replaceAll("\\s+", " "));
			}
		}
	}

	private static int lineOf(String text, int offset) {
		return (int) text.substring(0, offset).chars().filter(c -> c == '\n').count() + 1;
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
		// Calendar.getInstance(...)와 날짜 값 없이 만든 GregorianCalendar(시간대·지역만 받는 것 포함)는 현재 시각으로 채워진다.
		if (owner.equals("java.util.Calendar") && name.equals("getInstance")) {
			return true;
		}
		if (owner.equals("java.util.GregorianCalendar") && name.equals("<init>")) {
			return parameters.stream().allMatch(type -> type.isEquivalentTo(TimeZone.class)
					|| type.isEquivalentTo(Locale.class));
		}
		if ((owner.equals("java.time.Clock") && SYSTEM_CLOCK_FACTORIES.contains(name))
				|| (owner.equals("java.time.InstantSource") && name.equals("system"))) {
			return !origin.isEquivalentTo(ClockConfig.class);
		}
		return false;
	}
}
