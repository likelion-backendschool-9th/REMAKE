# 백엔드 테스트

> 에이전트용 영문판: [`docs/agent/backend-testing.md`](../agent/backend-testing.md)

백엔드 통합 테스트는 CI가 느려지지 않도록 Spring 앱 하나를 같이 쓴다. Spring은 설정이 같은 테스트 클래스끼리만 이미 띄운 앱을 재사용한다. 설정이 다른 테스트 클래스가 하나 생기면 앱과 PostgreSQL 컨테이너가 하나씩 더 뜬다. 지난 프로젝트에서는 클래스마다 설정이 달라 백엔드 CI가 50초에서 8분까지 늘었다. Backend CI는 앱이 두 번 이상 뜨면 경고한다([CI 문서](ci.md)).

지원 코드:

| 파일 | 하는 일 |
| --- | --- |
| `backend/src/test/java/com/remake/support/IntegrationTestSupport.java` | 모든 통합 테스트의 부모 클래스. `test` 프로필로 앱을 한 번 띄우고 PostgreSQL 컨테이너, MockMvc, 시험용 시계를 준비하며, 테스트가 끝날 때마다 정리한다 |
| `backend/src/test/java/com/remake/support/TestDatabase.java` | 앱이 뜬 직후 테이블을 검사한다. 테스트가 끝날 때마다 기준 데이터 테이블을 뺀 모든 테이블을 비운다 |
| `backend/src/test/java/com/remake/support/MutableClock.java` | 시험용 시계. 기본은 한국 시간 2026-03-04 10:00. 스스로 움직이지 않는다. `clock.setNow(...)`로 바꾸거나 `clock.advance(...)`로 앞으로 옮기고, 테스트가 끝나면 기본값으로 돌아간다 |
| `backend/src/test/resources/application-test.yml` | `test` 프로필 설정 |
| `backend/src/test/java/com/remake/RemakeApplicationTests.java` | 앱이 뜨는지(마이그레이션, 엔티티 검사, Bean 연결) 확인한다 |
| `backend/src/test/java/com/remake/ScheduledSettingsTest.java` | 모든 `@Scheduled` 설정(cron, 시간대, 지연·간격 값, `${...}` 설정 값 참조, `#{...}` SpEL 식)을 Spring의 검사기로 검사한다. 값은 앱이 뜰 때 Spring이 푸는 방식 그대로 푼다. 작업은 실행하지 않는다. 테스트에서는 스케줄러를 꺼 두므로 앱이 뜰 때 Spring이 검사하지 않는다 |
| `backend/src/test/java/com/remake/ClockUsageTest.java` | 운영 코드가 주입받은 `Clock` 없이 현재 시각을 읽으면 실패한다. `java.time` 타입의 `now()` 중 `now(Clock)`이 아닌 것(그래서 `now(ZoneId)`도 실패), `System.currentTimeMillis()`, `new Date()`, `Calendar.getInstance()`, `new GregorianCalendar()`, `ClockConfig` 밖에서 실제 시계로 만든 `Clock`, Hibernate의 `@CreationTimestamp`·`@UpdateTimestamp`·`@CurrentTimestamp`가 대상이다. 마이그레이션 SQL의 DB 시각 함수(`now()`, `CURRENT_DATE`, `CURRENT_TIMESTAMP` 등)와 특수 시각 문자열(`'now'`, `'today'`, `'tomorrow'`, `'yesterday'`)도 실패한다. SQL 주석은 검사하지 않는다. ArchUnit으로 컴파일된 클래스를 읽고 Spring은 띄우지 않는다. `@Query` 문자열은 검사하지 않는다 |
| `backend/src/test/java/com/remake/support/TestDatabaseTest.java` | 임시 테이블로 `TestDatabase`의 검사가 동작하는지 확인한다. 기준 데이터를 바꾸면 실패, 사용자 데이터는 비움, 앱이 뜬 직후 사용자 데이터 테이블에 행이 있으면 실패, 기준 데이터 테이블이 사용자 데이터를 참조하면 `CASCADE` 없이 비우기가 실패. `TestDatabase`를 고쳤더니 이 테스트가 실패하면 테스트를 고치지 말고 먼저 상의한다(에이전트는 멈추고 사용자에게 묻는다) |

## 테스트 실행

- `backend/`에서 `./gradlew test`나 `./gradlew build`를 실행한다. `IntegrationTestSupport`가 PostgreSQL을 컨테이너로 띄우므로 Docker가 켜져 있어야 한다. 테스트에는 로컬 DB(`docker compose up`)가 필요 없다.
- Docker가 꺼져 있으면 통합 테스트가 모두 실패한다. 콘솔에서는 첫 실패의 마지막 `Caused by` 줄이 `java.lang.IllegalStateException at DockerClientProviderStrategy.java:...`이다. 테스트 리포트(`backend/build/reports/tests/test/index.html`)에는 그 테스트에 `Could not find a valid Docker environment`, 나머지 테스트에 `ApplicationContext failure threshold (1) exceeded`가 나온다. Docker(Windows와 macOS는 Docker Desktop)를 켜고 다시 실행한다.
- Docker를 쓸 수 없으면 테스트 설정을 바꿔 피해 가지 않는다. 돌리지 못했다고 알리고(에이전트는 사용자에게) PR의 `## 테스트`에 적는다.

## 테스트 종류 고르기

확인하려는 것을 실제로 확인할 수 있는 종류 중 가장 가벼운 것을 고른다. 쿼리, 저장, 트랜잭션, 요청 형식처럼 Spring이나 DB가 있어야 확인되는 것을 가짜 객체로 대신하지 않는다.

| 테스트하려는 것 | 위치 | 쓰는 방식 | Spring 앱 |
| --- | --- | --- | --- |
| 도메인 규칙 (소비기한까지 남은 날, 단위 환산, 수량 차감) | `domain` | 순수 JUnit | 안 뜸 |
| 서비스의 흐름과 분기 (하루 횟수를 넘으면 예외) | `application/service` | JUnit + Mockito. 자기 기능의 `port/out`과 다른 기능의 `UseCase`를 `@Mock`으로 | 안 뜸 |
| 외부 API 어댑터의 요청·응답 변환 | `adapter/out/ai`, `ocr`, `oauth` | Spring 없이 `MockRestServiceServer.bindTo(RestClient.Builder)` | 안 뜸 |
| 저장 어댑터의 쿼리와 엔티티 변환 | `adapter/out/persistence` | `IntegrationTestSupport` 상속. 어댑터를 `transactionTemplate` 안에서 호출 | 공유 |
| API 요청·응답, 검증 오류, 전체 흐름 | `adapter/in/web` | `IntegrationTestSupport` 상속. `mockMvc`로 호출 | 공유 |
| 커밋·롤백에 따라 결과가 달라지는 흐름 (AI가 실패해도 `FAILED` 사용 기록은 남아야 함) | 서비스와 어댑터 | `IntegrationTestSupport` 상속. 부모의 외부 어댑터 가짜가 예외를 던지게 한 뒤 DB를 확인 | 공유 |
| 스케줄 작업 | `adapter/in/scheduler` | `IntegrationTestSupport` 상속. 작업이 부르는 `UseCase`를 직접 호출한다. 두 번 불러도 결과가 바뀌지 않는지 확인 | 공유 |

- `@WebMvcTest`, `@DataJpaTest`, `@RestClientTest`는 쓰지 않는다. 셋 다 앱을 따로 하나 더 띄운다. `@DataJpaTest`는 어댑터(`@Component`)도 불러오지 않고, 기본으로 롤백한다.
- 저장 어댑터 테스트는 직접 쓴 쿼리나 복잡한 변환이 있을 때만 쓴다. 단순한 저장과 조회는 API 테스트가 이미 거쳐 간다.
- 저장 어댑터가 `LazyInitializationException`으로 실패하면 `transactionTemplate` 안에서 호출한다. 운영에서도 서비스의 트랜잭션 안에서 불리기 때문이다. 매핑을 `EAGER`로 바꾸지 않는다.
- MockMvc는 Tomcat을 거치지 않는다. 그래서 업로드 크기 제한과 `/error` 응답은 MockMvc로 확인되지 않는다. 설정 값은 공유 앱에서 확인한다(예: `MultipartConfigElement` Bean). 업로드 한도는 파일당(`spring.servlet.multipart.max-file-size`)과 요청 전체(`spring.servlet.multipart.max-request-size`) 두 가지다.
- `RestClient`를 쓰지 않는 외부 어댑터(예: 업체 SDK)는 테스트 방법을 먼저 상의한다(에이전트는 멈추고 사용자에게 묻는다).

## 통합 테스트 규칙

1. `IntegrationTestSupport`를 상속한다. 테스트 클래스 선언에는 JUnit 어노테이션만, 필드에는 `@Autowired`만 쓴다. 클래스나 필드에 그 밖의 Spring 테스트 설정을 붙이면 무엇이든 앱이 하나 더 뜬다. 예: `@SpringBootTest`, `@TestPropertySource`, `@ActiveProfiles`, `@Import`, `@ContextConfiguration`, `@DynamicPropertySource`, `@DirtiesContext`, `@ImportAutoConfiguration`, `@AutoConfigure...`(`@AutoConfigureMockMvc(addFilters = false)` 포함), `@MockitoBean`, `@MockitoSpyBean`, `@TestBean`, `@WebMvcTest`, `@DataJpaTest`, `@RestClientTest`, 그리고 테스트 클래스 안에 넣은 `@TestConfiguration` 클래스. 가짜 객체와 스파이는 `IntegrationTestSupport`에 둔다.
2. 설정이 더 필요하면, 모든 테스트에 적용해도 괜찮을 때만 `IntegrationTestSupport`나 `application-test.yml`에 추가한다. 그렇지 않으면 새 부모 클래스를 만들기 전에 먼저 상의한다(에이전트는 멈추고 사용자에게 묻는다). 새 부모 클래스(예: `IntegrationTestSupport`를 상속하고 `@SpringBootTest(webEnvironment = RANDOM_PORT)`로 실제 HTTP 서버를 띄우는 클래스)를 만들면 앱과 컨테이너가 하나씩 더 뜬다. 같은 PR에서 `.github/workflows/backend-ci.yml`의 `--max-spring-boots`를 올리고, PR 본문에 이유를 적는다. 한 테스트 클래스만 따로 설정이 꼭 필요하면 먼저 상의한 뒤(에이전트는 사용자에게 묻는다) 그 클래스에 붙이고, 이유를 주석과 PR 본문에 적는다. 이때 Backend CI에 경고가 뜬다.
3. 통합 테스트에는 `@Transactional`을 붙이지 않는다. 데이터가 운영처럼 실제로 커밋되므로, 커밋할 때만 드러나는 버그를 테스트가 잡는다. 서비스에서 빠뜨린 `@Transactional`, 커밋할 때 나는 제약 위반, `@TransactionalEventListener` 코드가 그렇다. 테스트가 끝날 때마다 `TestDatabase`가 테이블을 비운다.
4. 테스트 데이터는 각 테스트 안이나 `@BeforeEach`에서 만든다. `@BeforeAll`에서 만든 데이터는 첫 테스트가 끝나면 지워진다. id를 숫자로 적지 않는다. 만든 행의 id를 쓰고, 기준 데이터는 code나 name 같은 유일 키로 찾는다.
5. 테스트에서 기준 데이터를 바꾸지 않는다. 바꾸면 그 테스트가 실패한다.
6. 시간은 부모의 `clock`으로 정한다: `clock.setNow(ZonedDateTime.of(..., MutableClock.ZONE))`. 시계는 스스로 움직이지 않으므로, 한 테스트에서 저장한 행은 `createdAt`과 `updatedAt`이 모두 같다. 시간 순서나 바뀐 수정 시각을 확인하는 테스트는 저장 사이에 시계를 옮긴다: `clock.advance(Duration.ofMinutes(1))`. 옮기지 않으면 정렬할 기준이 없어 PostgreSQL이 저장한 순서대로 나온다. 그래서 최신순 테스트는 실패하고, 오래된순 테스트는 `ORDER BY`가 없어도 통과한다.
7. `IntegrationTestSupport`가 띄우는 PostgreSQL 컨테이너를 쓴다. 테스트 클래스에서 컨테이너를 띄우거나 DB 설정을 넣지 않는다.
8. JUnit 병렬 실행을 켜지 않는다. 테스트마다 테이블을 비우므로, 동시에 돌면 서로의 데이터를 지운다.
9. 테스트에서 `static` 값을 바꾸지 않는다. 모든 테스트가 같이 쓰고, 아무것도 되돌리지 않는다.

가짜 객체가 필요하면 이 순서로 시도한다. 단위 테스트 → `IntegrationTestSupport`에 둔 스파이(`@MockitoSpyBean`. 테스트가 동작을 정하지 않으면 진짜처럼 동작) → 부모의 `clock` → 규칙 2.

예 (설명용, 주소는 예시):

```java
class IngredientApiTest extends IntegrationTestSupport {

	@Test
	void 자정이_지나면_남은_날이_줄어든다() throws Exception {
		// 소비기한이 2026-03-07인 재료를 등록한다 (기본 시각 기준 3일 남음).
		clock.setNow(ZonedDateTime.of(2026, 3, 5, 0, 30, 0, 0, MutableClock.ZONE));
		mockMvc.perform(get("/api/v1/user/ingredients"))
				.andExpect(jsonPath("$[0].daysLeft").value(2));
	}
}
```

## 외부 서비스 어댑터

- 외부 서비스 어댑터(`adapter/out/ai`, `ocr`, `oauth`, 그리고 파일을 외부 저장소에 둘 때의 `filestorage`)를 추가하는 PR에서, `IntegrationTestSupport`에 그 어댑터 클래스를 `@MockitoBean`으로 바꿔 둔다. 포트 인터페이스를 가짜로 바꾸지 않는다. 어댑터가 포트를 여러 개 구현하면 앱이 뜨지 않는다.
- 가짜로 바꾼 어댑터는 테스트에서 진짜 생성자가 실행되지 않는다. 그래서 의존성이 빠져도(예: `spring-boot-starter-restclient` 없이 `RestClient.Builder`를 받음) 모든 테스트가 통과하고 서버에서만 앱이 뜨지 않는다. `MockedAdapterWiringTest`가 `IntegrationTestSupport`의 `@MockitoBean` 어댑터마다 생성자 인자를 Spring이 생성자를 부를 때와 같은 방식으로 찾아 본다(`@Value`, `@Qualifier`, `Optional`, `List`도 처리하고, 같은 타입의 Bean이 여러 개면 실패한다). 어댑터의 생성자는 하나만 둔다. 이 테스트가 실패하면 테스트가 아니라 앱을 고친다. 빠진 의존성을 `build.gradle`에 추가하거나, 같은 타입의 Bean이 여러 개면 `@Qualifier`를 붙인다.
- 외부 호출을 거치는 테스트는 필요한 응답을 각자 정한다. 정하지 않은 호출은 기본값을 돌려준다. 객체는 `null`, 컬렉션과 `Optional`은 빈 값, 숫자는 0이다. 정한 응답은 테스트가 끝나면 초기화된다.
- 어댑터가 쓰는 설정의 가짜 키와 접속되지 않는 주소(`http://localhost:1`)를 `application-test.yml`에 넣는다. 테스트는 `backend/.env`를 읽지 않지만(`local` 프로필만 읽는다), 셸이나 IDE의 환경 변수는 읽는다. 가짜 키가 없으면 그 환경 변수에 있는 진짜 키가 쓰일 수 있다.
- 파일을 서버 폴더에 저장한다면 `filestorage`를 가짜로 바꾸지 않는다. `application-test.yml`에서 저장 폴더를 테스트용 폴더로 바꾸고, `IntegrationTestSupport.cleanUp`에서 비운다.
- 어댑터 테스트(Spring 없음)는 `RestClient.builder()`에 `MockRestServiceServer.bindTo(builder)`로 가짜 서버를 붙여 만든 클라이언트를 어댑터 생성자에 넘긴다. 붙인 뒤에는 그 빌더에 요청 팩토리(예: 타임아웃용)를 설정하지 않는다. 가짜 서버가 대체되어 요청이 실제 네트워크로 나간다.

## 기준 데이터

- 기준 데이터(재료 카테고리, 재료 사전. 알레르기 항목은 재료 사전에 포함)는 Flyway 마이그레이션으로만 넣는다. 그 테이블은 `IntegrationTestSupport.REFERENCE_TABLES`에 적혀 있고, `TestDatabase`는 이 테이블을 비우지 않는다.
- 마이그레이션으로 기준 데이터 테이블을 추가하면 같은 PR에서 이 목록에도 추가한다. 빠뜨리면 앱이 뜰 때 `앱이 뜬 직후 행이 있는 테이블: [...]`로 실패한다.
- 테이블을 비울 때 `CASCADE`를 붙이지 않는다. `cannot truncate a table referenced in a foreign key constraint`로 실패하면 기준 데이터 테이블이 사용자 데이터를 참조하고 있는 것이다. 스키마를 고친다.

## 새로 추가하거나 바꿀 때

| 추가하거나 바꾸는 것 | 할 일 |
| --- | --- |
| 외부 서비스 어댑터 | `IntegrationTestSupport`에서 가짜로 바꾸고 `application-test.yml`에 가짜 설정을 넣는다(위) |
| 기준 데이터 마이그레이션 | 테이블을 `REFERENCE_TABLES`에 추가한다. 유일 키로 id를 찾는 도우미를 추가한다(예: `categoryId("MEAT")`) |
| `users` 테이블 | `IntegrationTestSupport`에 `createUser()`를 추가한다. 다른 기능의 테스트도 이 도우미로 사용자를 만든다 |
| Spring Security | 로그인 토큰을 붙여 요청하는 도우미를 `IntegrationTestSupport`에 추가한다 |
| 캐시(`@Cacheable`)나 Redis | `IntegrationTestSupport.cleanUp`에서 비운다. Redis는 컨테이너를 `IntegrationTestSupport`에서 띄운다 |
| `@Async`나 지금 접수하고 나중에 처리하는 작업 | 테스트는 작업이 끝날 때까지 기다린다(Awaitility). `cleanUp`은 실행 중인 작업이 끝난 뒤 비운다. 실행기를 `SyncTaskExecutor`로 바꾸지 않는다. 작업이 호출한 쪽 트랜잭션에 섞여 운영과 다르게 동작한다 |
| `compose.prod.yml`의 PostgreSQL 버전 | `IntegrationTestSupport`의 컨테이너 이미지(`postgres:17`)도 같은 버전으로 바꾼다 |

## 테스트를 위해 운영 코드가 지킬 것

| 규칙 | 이유 |
| --- | --- |
| 현재 시각은 주입받은 `Clock`으로만 읽는다(`LocalDate.now(clock)`). `domain`은 시각이나 `Clock`을 인자로 받는다 | 테스트는 `Clock` Bean으로 시각을 바꾼다. 정책 대부분이 한국 시간 기준이다(00:00 초기화, D-3, 월요일 집계, 09:00 알림). 이 규칙을 어기면 `ClockUsageTest`가 실패한다 |
| JPA 감사(`@CreatedDate`, `@LastModifiedDate`)가 넣는 시각과 날짜 검증(`@Future`, `@Past`)은 `global/config/ClockConfig.java`와 `global/config/JpaConfig.java`를 통해 `Clock`을 쓴다. `@EnableJpaAuditing`은 `JpaConfig`에만 둔다. JWT 라이브러리에도 `Clock`을 넘긴다. `@CreationTimestamp`, 컬럼의 `DEFAULT now()`, 쿼리 안의 `now()`·`CURRENT_DATE`는 쓰지 않고 시각을 파라미터로 넘긴다 | 이것들은 실제 시계를 써서 시험용 시계를 따르지 않는다. `ClockUsageTest`가 `@CreationTimestamp`와 마이그레이션 SQL의 시각 함수는 잡지만 `@Query` 문자열 안은 잡지 못한다. `@EnableJpaAuditing`이 두 곳에 있으면 앱이 뜨지 않는다 |
| `BaseCreatedEntity`와 `BaseTimeEntity`의 시각 필드는 `LocalDateTime`으로 둔다. 응답의 `+09:00` 형식은 Response DTO에서 만든다 | JPA 감사는 `OffsetDateTime`을 지원하지 않아, 쓰면 저장할 때마다 실패한다 |
| 사용자 데이터 테이블에 고정 행(빈 DB에서도 생기는 행: 마이그레이션의 상수 `INSERT`, 앱 시작 코드)을 넣지 않는다. 기존 데이터로 채우는 마이그레이션(`INSERT ... SELECT`)은 괜찮다. 탈퇴한 사용자는 `users.deleted_at`으로 표시한다. 첫 관리자는 배포 담당이 서버에서 SQL로 만든다 | 사용자 데이터 테이블에 행이 있으면 `TestDatabase`가 앱이 뜰 때 테스트를 멈춘다 |
| `@EnableScheduling`은 `global/config/SchedulingConfig.java`에만 둔다 | 테스트는 `remake.scheduling.enabled: false`로 스케줄러를 끈다. 그러지 않으면 테스트 중에 작업이 돌아 정리와 겹친다 |
| `@Scheduled` 값(cron, 시간대, 지연 시간)은 코드나 `application.yml`에 둔다. `application-prod.yml`에서만 정하거나 바꾸지 않는다 | `ScheduledSettingsTest`는 `test` 프로필이 읽는 값을 검사한다. `prod` 프로필에만 있는 값은 검사되지 않는다 |
| cron에는 시간대를 적는다: `@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")` | 시간대를 적지 않으면 cron은 스케줄러의 기본 시간대, 즉 서버(JVM) 설정을 따른다. 운영 컨테이너는 `TZ: Asia/Seoul`이라 지금은 맞지만, 서버 설정이 바뀌면 시간대를 적지 않은 작업의 시각이 모두 바뀐다. 적어 둔 시간대는 `ScheduledSettingsTest`가 검사한다 |
| 스케줄 작업은 두 번 돌아도 결과가 같아야 한다. 동시에 도는 중복은 DB 유니크 키로 막고, 중복이면 건너뛴다 | 배포 중에는 두 버전이 함께 떠서 같은 작업을 같은 시각에 시작할 수 있다([배포 문서](deployment.md)) |
| 외부 서비스 어댑터는 `RestClient`(또는 `RestClient.Builder`)를 생성자로 받고, 안에서 만들지 않는다 | 어댑터 테스트가 `MockRestServiceServer`를 붙인 클라이언트를 넘긴다 |
