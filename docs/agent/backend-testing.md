# Backend Testing

> Korean version for developers: [`docs/developer/backend-testing.md`](../developer/backend-testing.md)

Backend integration tests share one Spring application so that CI stays fast. Spring reuses a started application only for test classes with the same configuration. A test class with its own configuration starts another application and another PostgreSQL container. In a previous project, per-class configuration made backend CI grow from 50 seconds to 8 minutes. Backend CI warns when the application starts more than once (see the [CI document](ci.md)).

Support code:

| File | Role |
| --- | --- |
| `backend/src/test/java/com/remake/support/IntegrationTestSupport.java` | Base class of every integration test. Starts the application once with the `test` profile, a PostgreSQL container, MockMvc, and the test clock, and cleans up after each test |
| `backend/src/test/java/com/remake/support/TestDatabase.java` | Checks the tables right after startup. Truncates every table except the reference tables after each test |
| `backend/src/test/java/com/remake/support/MutableClock.java` | Test clock. Default 2026-03-04 10:00 Asia/Seoul. It does not move by itself. Change it with `clock.setNow(...)` or move it forward with `clock.advance(...)`; it resets after each test |
| `backend/src/test/resources/application-test.yml` | Settings of the `test` profile |
| `backend/src/test/java/com/remake/RemakeApplicationTests.java` | Checks that the application starts (migrations, entity validation, bean wiring) |
| `backend/src/test/java/com/remake/ScheduledSettingsTest.java` | Checks every `@Scheduled` setting (cron, zone, delay and rate values, `${...}` placeholders, `#{...}` SpEL expressions) with Spring's own checker, resolving values the same way Spring does at startup, without running the jobs. Tests turn scheduling off, so Spring does not check them at startup |
| `backend/src/test/java/com/remake/ClockUsageTest.java` | Fails when production code reads the current time without the injected `Clock`: `now()` on `java.time` types except `now(Clock)` (so `now(ZoneId)` also fails), `System.currentTimeMillis()`, `new Date()`, `Calendar.getInstance()`, `new GregorianCalendar()`, a system `Clock` outside `ClockConfig`, and Hibernate `@CreationTimestamp`, `@UpdateTimestamp`, `@CurrentTimestamp`. Also fails on DB time functions (`now()`, `CURRENT_DATE`, `CURRENT_TIMESTAMP`, ...) and special time strings (`'now'`, `'today'`, `'tomorrow'`, `'yesterday'`) in migration SQL, ignoring SQL comments. Reads compiled classes with ArchUnit and does not start Spring. Does not check `@Query` strings |
| `backend/src/test/java/com/remake/support/TestDatabaseTest.java` | Checks that the `TestDatabase` checks work, using temporary tables: changing reference data fails, user data is truncated, a user data table with rows at startup fails, and truncating fails without `CASCADE` when a reference table references user data. If a change to `TestDatabase` makes these tests fail, do not change the tests; stop and ask the user |

## Running the tests

- Run `./gradlew test` or `./gradlew build` in `backend/`. Docker must be running: `IntegrationTestSupport` starts PostgreSQL in a container. Tests do not need the local DB (`docker compose up`).
- If Docker is not running, every integration test fails. In the console, the last `Caused by` line of the first failure is `java.lang.IllegalStateException at DockerClientProviderStrategy.java:...`. The test report (`backend/build/reports/tests/test/index.html`) shows `Could not find a valid Docker environment` for that test and `ApplicationContext failure threshold (1) exceeded` for the others. Start Docker (Docker Desktop on Windows and macOS) and run the tests again.
- If Docker is not available, do not change the test setup to work around it. Tell the user and write it under `## 테스트` in the PR.

## Choose the test type

Choose the lightest type that can actually verify what you test. Do not replace what needs Spring or the DB (queries, saving, transactions, request format) with mocks.

| What you test | Where | How | Spring application |
| --- | --- | --- | --- |
| Domain rules (days until expiration, unit conversion, quantity deduction) | `domain` | Plain JUnit | None |
| Service flow and branches (exception when the daily limit is exceeded) | `application/service` | JUnit + Mockito. Mock the feature's own `port/out` and other features' `UseCase` with `@Mock` | None |
| External API adapter request and response mapping | `adapter/out/ai`, `ocr`, `oauth` | No Spring. `MockRestServiceServer.bindTo(RestClient.Builder)` | None |
| Persistence adapter queries and entity mapping | `adapter/out/persistence` | Extend `IntegrationTestSupport`. Call the adapter inside `transactionTemplate` | Shared |
| API requests and responses, validation errors, whole flows | `adapter/in/web` | Extend `IntegrationTestSupport`. Call through `mockMvc` | Shared |
| Flows whose result depends on commit or rollback (a `FAILED` usage log must remain when AI fails) | Service and adapters | Extend `IntegrationTestSupport`. Make the parent's external adapter mock throw, then check the DB | Shared |
| Scheduled jobs | `adapter/in/scheduler` | Extend `IntegrationTestSupport`. Call the `UseCase` the job calls. Call it twice and check that the result does not change | Shared |

- Do not use `@WebMvcTest`, `@DataJpaTest`, or `@RestClientTest`. Each starts a separate application. `@DataJpaTest` also does not load adapters (`@Component`) and rolls back by default.
- Write persistence adapter tests only for hand-written queries or complex mapping. API tests already cover simple saving and loading.
- If a persistence adapter fails with `LazyInitializationException`, call it inside `transactionTemplate`, as production calls it inside a service transaction. Do not change the mapping to `EAGER`.
- MockMvc does not go through Tomcat. Upload size limits and the `/error` response are not tested through it. Check configuration values in the shared application (for example the `MultipartConfigElement` bean). Upload limits have two values: per file (`spring.servlet.multipart.max-file-size`) and per request (`spring.servlet.multipart.max-request-size`).
- For an external adapter that does not use `RestClient` (for example a vendor SDK), stop and ask the user how to test it.

## Integration test rules

1. Extend `IntegrationTestSupport`. On the test class, use only JUnit annotations; on its fields, use only `@Autowired`. Any other Spring test configuration on the class or its fields starts another application. Examples: `@SpringBootTest`, `@TestPropertySource`, `@ActiveProfiles`, `@Import`, `@ContextConfiguration`, `@DynamicPropertySource`, `@DirtiesContext`, `@ImportAutoConfiguration`, `@AutoConfigure...` (including `@AutoConfigureMockMvc(addFilters = false)`), `@MockitoBean`, `@MockitoSpyBean`, `@TestBean`, `@WebMvcTest`, `@DataJpaTest`, `@RestClientTest`, and a nested `@TestConfiguration` class. Mocks and spies go in `IntegrationTestSupport`.
2. If a test needs more setup, add it to `IntegrationTestSupport` or `application-test.yml` only when it is safe for all tests. Otherwise, stop and ask the user before creating a new base class. A new base class (for example one that starts a real HTTP server with `@SpringBootTest(webEnvironment = RANDOM_PORT)` and extends `IntegrationTestSupport`) starts one more application and container. Raise `--max-spring-boots` in `.github/workflows/backend-ci.yml` in the same PR and explain why in the PR description. If only one test class needs its own configuration, ask the user first, then add it to that class and explain why in a comment and in the PR description. Backend CI shows a warning for it.
3. Do not add `@Transactional` to integration tests. Data is committed as in production, so tests catch bugs that appear only on commit: a missing `@Transactional` in a service, constraint violations raised at commit, and `@TransactionalEventListener` code. `TestDatabase` truncates the tables after each test.
4. Create test data in each test or in `@BeforeEach`. Data created in `@BeforeAll` is deleted after the first test. Do not hardcode IDs: use the IDs of the rows you created, and find reference data by a unique key (code or name).
5. Do not change reference data in tests. The test fails.
6. Use the parent's `clock` for time: `clock.setNow(ZonedDateTime.of(..., MutableClock.ZONE))`. The clock does not move by itself, so rows saved in one test get the same `createdAt` and `updatedAt`. When a test checks order by time or a changed update time, move the clock between saves: `clock.advance(Duration.ofMinutes(1))`. Otherwise there is nothing to sort by and rows come back in the order PostgreSQL stored them: a newest-first test fails, and an oldest-first test passes even without `ORDER BY`.
7. Use the PostgreSQL container that `IntegrationTestSupport` starts. Do not start containers or set DB properties in test classes.
8. Do not enable JUnit parallel execution. Every test truncates the tables, so parallel tests delete each other's data.
9. Do not change `static` values in tests. Every test in the run shares them, and nothing resets them.

When a test needs a mock, try in this order: a unit test, then a spy (`@MockitoSpyBean`) in `IntegrationTestSupport` that behaves like the real bean unless a test stubs it, then the parent's `clock`, then rule 2.

Example (illustrative; the endpoint is an example):

```java
class IngredientApiTest extends IntegrationTestSupport {

	@Test
	void 자정이_지나면_남은_날이_줄어든다() throws Exception {
		// Register an ingredient that expires on 2026-03-07 (3 days left at the default time).
		clock.setNow(ZonedDateTime.of(2026, 3, 5, 0, 30, 0, 0, MutableClock.ZONE));
		mockMvc.perform(get("/api/v1/user/ingredients"))
				.andExpect(jsonPath("$[0].daysLeft").value(2));
	}
}
```

## External service adapters

- In the PR that adds an external service adapter (`adapter/out/ai`, `ocr`, `oauth`, and `filestorage` when files go to external storage), replace it in `IntegrationTestSupport` with `@MockitoBean` on the adapter class. Do not mock a port interface: when an adapter implements several ports, the application fails to start.
- A mocked adapter's real constructor never runs in tests, so a missing dependency (for example `RestClient.Builder` without `spring-boot-starter-restclient`) passes every test and fails only on the server. `MockedAdapterWiringTest` resolves every constructor parameter of each `@MockitoBean` adapter in `IntegrationTestSupport` the way Spring does when it calls the constructor (`@Value`, `@Qualifier`, `Optional`, and `List` included; several beans of one type fail). Give each adapter a single constructor. If this test fails, fix the app, not the test: add the missing dependency to `build.gradle`, or add `@Qualifier` when several beans match.
- Each test stubs the responses it needs. Unstubbed calls return default values: `null` for objects, empty for collections and `Optional`, 0 for numbers. Stubs reset after each test.
- Add fake keys and an unreachable address (`http://localhost:1`) for the adapter's properties to `application-test.yml`. Tests do not read `backend/.env` (only the `local` profile reads it), but they still read environment variables from the shell or IDE. Without the fake key, a real key from those variables can be used.
- If files are saved to a server folder, do not mock `filestorage`. Set the folder to a test folder in `application-test.yml` and clear it in `IntegrationTestSupport.cleanUp`.
- An adapter test (no Spring) builds a client from `RestClient.builder()` bound with `MockRestServiceServer.bindTo(builder)` and passes it to the adapter's constructor. Do not set a request factory (for example for timeouts) on that builder after binding: it replaces the mock server, and requests go to the real network.
- To give an adapter timeouts, build its `RestClient` in a `@Configuration` bean (request factory and base URL there) and have the adapter's constructor take the built `RestClient` with `@Qualifier`. The adapter test then passes a `RestClient` built from the mock-bound builder. Example: `GeminiClientConfig` and `GeminiRecipeAdapter`.
- A mocked adapter's real constructor never runs in integration tests. Add a small wiring test that gets each constructor argument from the application context and builds the adapter (example: `GeminiRecipeAdapterWiringTest`).

## Reference data

- Reference data (ingredient categories, ingredient catalog; allergy items are part of the ingredient catalog) is inserted only by Flyway migrations. Its tables are listed in `IntegrationTestSupport.REFERENCE_TABLES`, and `TestDatabase` never truncates them.
- When a migration adds a reference table, add the table to the list in the same PR. If you forget, the application fails at startup with `앱이 뜬 직후 행이 있는 테이블: [...]`.
- Do not add `CASCADE` to the truncate. If it fails with `cannot truncate a table referenced in a foreign key constraint`, a reference table references user data. Fix the schema.

## When you add or change

| When you add or change | Do this |
| --- | --- |
| An external service adapter | Mock it in `IntegrationTestSupport` and add fake properties to `application-test.yml` (above) |
| A reference data migration | Add the table to `REFERENCE_TABLES`. Add a helper that finds IDs by a unique key (for example `categoryId("MEAT")`) |
| The `users` table | Add `createUser()` to `IntegrationTestSupport`. Tests of other features create users with it |
| Spring Security | Add a helper to `IntegrationTestSupport` that sends requests with a login token |
| A cache (`@Cacheable`) or Redis | Clear it in `IntegrationTestSupport.cleanUp`. For Redis, start its container in `IntegrationTestSupport` |
| `@Async` or jobs that are accepted now and processed later | Tests wait for completion (Awaitility). `cleanUp` waits for running jobs before truncating. Do not replace the executor with `SyncTaskExecutor`: the job would join the caller's transaction and behave differently from production |
| The PostgreSQL version in `compose.prod.yml` | Change the container image in `IntegrationTestSupport` (`postgres:17`) to the same version |

## Production code rules for testability

| Rule | Why |
| --- | --- |
| Read the current time only from the injected `Clock` (`LocalDate.now(clock)`). `domain` receives the time or a `Clock` as an argument | Tests change the time through the `Clock` bean. Most policies depend on Korean time (00:00 reset, D-3, Monday ranking, 09:00 alerts). `ClockUsageTest` fails when this rule is broken |
| Timestamps set by JPA auditing (`@CreatedDate`, `@LastModifiedDate`) and date validation (`@Future`, `@Past`) use the `Clock` through `global/config/ClockConfig.java` and `global/config/JpaConfig.java`. Put `@EnableJpaAuditing` only on `JpaConfig`. Pass the `Clock` to the JWT library. Do not use `@CreationTimestamp`, a column `DEFAULT now()`, or `now()`/`CURRENT_DATE` in queries; pass the time as a parameter | Those use the real clock and do not follow the test clock. `ClockUsageTest` catches `@CreationTimestamp` and time functions in migration SQL, but not in `@Query` strings. A second `@EnableJpaAuditing` stops the application from starting |
| Declare the time fields of `BaseCreatedEntity` and `BaseTimeEntity` as `LocalDateTime`. Build the `+09:00` format of responses in the Response DTO | JPA auditing does not support `OffsetDateTime`: with it, every save fails |
| Do not insert fixed rows into user data tables (rows that exist even on an empty DB: constant `INSERT`s in migrations, startup code). Backfill migrations (`INSERT ... SELECT`) are fine. Mark withdrawn users with `users.deleted_at`. The deployment owner makes the first admin with SQL on the server | `TestDatabase` stops the tests at startup when a user data table has rows |
| Put `@EnableScheduling` only on `global/config/SchedulingConfig.java` | Tests turn scheduling off with `remake.scheduling.enabled: false`. Otherwise jobs run during tests and overlap with the cleanup |
| Keep `@Scheduled` values (cron, zone, delays) in code or `application.yml`. Do not set or override them only in `application-prod.yml` | `ScheduledSettingsTest` checks the values that the `test` profile reads. Values only in the `prod` profile are not checked |
| Write `zone` in cron schedules: `@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")` | Without `zone`, a cron follows the scheduler's default time zone, which comes from the server (JVM) settings. The production containers set `TZ: Asia/Seoul`, so it is correct now, but a change in the server settings would move every job without `zone`. `ScheduledSettingsTest` also checks the `zone` you write |
| A scheduled job must give the same result when it runs twice. Prevent concurrent duplicates with DB unique keys and skip duplicates | During a deployment both versions run and can start the same job at the same time ([deployment document](deployment.md)) |
| An external service adapter receives its `RestClient` (or `RestClient.Builder`) through its constructor and does not create it | The adapter test passes a client bound to `MockRestServiceServer` |
