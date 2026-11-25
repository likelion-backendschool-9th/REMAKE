# REMAKE 백엔드 코드 컨벤션

> 에이전트용 영문판: [`docs/agent/backend-convention.md`](../agent/backend-convention.md)

## 코드 스타일

- 테스트는 [백엔드 테스트 문서](backend-testing.md)대로 씁니다(테스트 종류, 통합 테스트 부모 클래스, 시험용 시계).
- Java 코드와 JSON은 camelCase, DB는 snake_case로 씁니다.

## 폴더 구조

헥사고날 아키텍처로 만듭니다. 기능별로 묶고, 기능 폴더 안은 모두 같은 모양으로 둡니다.

```
com.remake
├── user / auth / ingredient / receipt / recipe / mealplan / cart / share / quota / notification / event / admin
│   ├── domain                  핵심 객체와 규칙 (Spring, JPA 모름)
│   ├── application
│   │   ├── port
│   │   │   ├── in              UseCase 인터페이스 (들어오는 입구)
│   │   │   └── out             Port 인터페이스 (나가는 출구)
│   │   └── service             UseCase 구현, 예외를 던지는 곳
│   └── adapter
│       ├── in
│       │   ├── web             Controller, Request/Response DTO
│       │   └── scheduler       예약 작업 (필요한 기능만)
│       └── out
│           ├── persistence     JPA Entity, Repository, Port 구현
│           └── ai / ocr / oauth / filestorage / mail  외부 서비스 호출 (필요한 기능만)
│
└── global                      모든 기능이 같이 쓰는 것
    ├── error                   ErrorCode, BusinessException, GlobalExceptionHandler
    ├── config                  Swagger, JPA, CORS, Clock, JSON 시각 표기, 외부 서비스 공통 설정
    ├── security                JWT 필터, Security 설정
    └── common                  BaseCreatedEntity, BaseTimeEntity, 중복 요청 방지 필터 같은 공통 클래스
```

기능 폴더와 테이블(ERD v3):

| 기능 폴더 | 테이블 |
|---|---|
| `user` | users, user_excluded_ingredients, account_deletion_requests, subscriptions, subscription_payments |
| `auth` | refresh_tokens |
| `ingredient` | ingredient_categories, ingredient_catalog, ingredients, ingredient_logs |
| `receipt` | receipts, receipt_items |
| `recipe` | recipes, recipe_ingredients, recipe_steps, saved_recipes |
| `mealplan` | meal_plans, meal_plan_slots |
| `cart` | cart_items |
| `share` | shared_recipes, shared_recipe_reports, shared_recipe_weekly_ranks |
| `quota` | usage_logs, generation_quota_grants |
| `notification` | notifications |
| `event` | events |
| `admin` | 보류: 어드민 화면이 피그마에 없어 어드민 테이블이 없습니다. 디자인되면 어드민 기능은 각 기능의 UseCase를 부릅니다 |

지킬 것:

- 의존은 `adapter → application → domain` 한 방향입니다. `domain`에는 Spring, JPA 어노테이션을 쓰지 않습니다.
- Controller는 `UseCase` 인터페이스만 부릅니다.
- Service는 자기 기능의 `Port` 인터페이스와 다른 기능의 `UseCase` 인터페이스만 부릅니다.
- 공통 클래스는 JPA Entity만 상속합니다. domain 객체는 상속하지 않습니다.
  - 값이 수정되는 테이블(users, ingredients, meal_plan_slots, cart_items, shared_recipes)은 `BaseTimeEntity`(`created_at` + `updated_at`)를 상속합니다. 확정한 영수증은 수정하지 않으므로 `receipts`, `receipt_items`는 `BaseCreatedEntity`를 상속합니다.
  - 나머지 테이블은 `BaseCreatedEntity`(`created_at`)를 상속합니다.
  - 연결 테이블, 기준 데이터, 일별 집계, `events`는 상속하지 않습니다.

### 다른 기능의 데이터 쓰기

- 다른 기능의 데이터가 필요하면 그 기능의 `UseCase`(port/in)를 부릅니다. 다른 기능의 `Port`(port/out)나 adapter, Repository는 쓰지 않습니다.
- 다른 기능의 데이터는 domain 객체와 JPA Entity 모두 `Long ingredientId`처럼 ID만 가집니다. 다른 기능의 Entity와 JPA 연관관계(`@ManyToOne` 등)를 맺지 않습니다. DB의 외래 키는 그대로 둡니다.
- 다른 기능의 domain enum(`ingredient/domain`의 `IngredientUnit`, `StorageLocation` 같은 고정 값 목록)은 그대로 가져다 씁니다. `UseCase` 규칙은 데이터를 읽고 쓸 때의 규칙이고, 값 타입에는 해당하지 않습니다. enum은 그 값을 정하는 기능에 두고 `global`로 옮기지 않습니다.
- 같은 기능 안에서는 객체로 묶고 JPA 연관관계를 써도 됩니다. 예: `Recipe`와 `RecipeStep`.

### 필요한 기능만 만드는 폴더

`adapter/in/web`과 `adapter/out/persistence` 말고 다른 연결이 필요하면, 그 기능을 맡은 사람이 필요할 때 아래 이름으로 폴더를 만듭니다.

| 연결 대상 | 위치 | 예 |
|---|---|---|
| 정해진 시각에 저절로 도는 작업 | `adapter/in/scheduler` | 임박 알림 만들기, 영수증 정리, 금주의 레시피 보상 |
| AI (레시피·식단표 생성, 카테고리 추정) | `adapter/out/ai` | `recipe/adapter/out/ai` |
| OCR (영수증 글자 읽기) | `adapter/out/ocr` | `receipt/adapter/out/ocr` |
| 소셜 로그인 | `adapter/out/oauth` | `auth/adapter/out/oauth` |
| 파일 저장 (영수증 사진, 조리 사진) | `adapter/out/filestorage` | `receipt/adapter/out/filestorage` |
| 소셜 연결 끊기 (카카오 unlink / 네이버 delete) | `adapter/out/oauth` | `user/adapter/out/oauth` |
| 메일 발송 (카카오·구글 계정 삭제 인증번호) | `adapter/out/mail` | `user/adapter/out/mail` |

- 스케줄러는 Controller처럼 `UseCase`만 부릅니다. 두 번 돌아도 결과가 같게 만들고, 동시에 도는 중복은 DB 유니크 키로 막습니다.
- Service는 외부 서비스를 직접 부르지 않고 `port/out` 인터페이스만 부릅니다. 외부 서비스 호출 코드는 위 adapter 폴더에서 그 인터페이스를 구현합니다.
- 여러 기능이 같이 쓰는 외부 서비스 설정(API 키, HTTP 클라이언트)은 `global/config`에 하나만 둡니다.
- 표에 없는 연결이 필요하면 이름을 정해서 이 표에 먼저 추가합니다.
- 파일 저장 폴더는 `storage`가 아니라 `filestorage`입니다. `storage`는 재료의 보관 위치(냉장·냉동·실온)를 뜻하는 단어라 겹치지 않게 합니다.

### 클래스 이름 예시 (재료 기능)

| 위치 | 이름 |
|---|---|
| domain | `Ingredient` |
| port/in | `CreateIngredientUseCase`, `GetIngredientsUseCase` |
| port/out | `LoadIngredientPort`, `SaveIngredientPort` |
| service | `IngredientService` |
| adapter/in/web | `IngredientController`, `IngredientCreateRequest`, `IngredientResponse` |
| adapter/out/persistence | `IngredientJpaEntity`, `IngredientJpaRepository`, `IngredientPersistenceAdapter` |

## DB와 엔티티

테이블 구조는 [ERD·테이블 정의서](erd.md)를 따릅니다.

- **테이블은 Flyway로만 만들고 바꿉니다.** 엔티티를 추가하거나 바꾸면 같은 PR에 `backend/src/main/resources/db/migration/V<번호>__<설명>.sql`을 넣습니다. JPA는 로컬과 운영 모두 `validate`(검사만)라서 SQL을 빠뜨리면 앱이 뜨지 않습니다.
  - 번호는 dev의 마지막 번호 다음 숫자를 씁니다. 다른 PR이 먼저 머지돼서 번호가 겹치면, 머지 전에 내 파일 번호를 올립니다.
  - 이미 dev에 머지된 마이그레이션 파일은 고치지 않습니다. 바꿀 것이 있으면 새 번호 파일을 추가합니다.
  - Backend CI가 이 두 규칙을 최신 dev와 비교해 검사합니다([마이그레이션 검사](ci.md#마이그레이션-검사)). dev에 마이그레이션이 들어오면 마이그레이션을 추가한 열린 PR의 이 검사가 자동으로 다시 돌아서, 번호가 이미 쓰인 PR은 빨간색이 됩니다. 확실하지 않으면 머지 직전에 Backend CI를 직접 다시 실행합니다.
  - 배포 중에는 이전 버전 앱과 새 버전 앱이 같은 DB를 잠시 같이 씁니다. 컬럼 이름 변경이나 삭제처럼 이전 버전을 깨는 변경은 단계를 나눠서 합니다(자세한 내용은 [배포 문서](deployment.md)).
- **타입**: 수량은 `numeric(10,3)` ↔ `BigDecimal`, 금액은 `bigint` ↔ `Long`(원 단위), 시각은 `timestamp` ↔ `LocalDateTime`(한국 시간), JSON은 `jsonb`.
- **정해진 값 목록**(상태, 종류, 단위 등)은 Java enum으로 만들고 `@Enumerated(EnumType.STRING)`으로 저장합니다. 순서 번호(`ORDINAL`)로 저장하지 않습니다.
- **현재 시각은 주입받은 `Clock`으로만 읽습니다.** `LocalDateTime.now()`나 `LocalDate.now(zone)`처럼 `Clock` 없이 부르는 `now()`, 컬럼의 `DEFAULT now()`, 쿼리 안의 `now()`는 쓰지 않습니다. 생성·수정 시각은 공통 클래스가 JPA 감사로 넣습니다. 운영 코드나 마이그레이션 SQL이 이 규칙을 어기면 `ClockUsageTest`가 빌드를 실패시킵니다.

## 용어

코드, DB, API에서 같은 단어를 씁니다.

| 화면의 말 | 코드의 단어 |
|---|---|
| 회원 | `user` |
| 재료 | `ingredient` |
| 재료 사전 | `ingredientCatalog` |
| 제외 재료(알레르기) | `excludedIngredient` |
| 소비기한 | `expirationDate` |
| 보관 위치 | `storage` |
| 수량 / 단위 | `amount` / `unit` |
| 영수증 | `receipt` |
| 레시피 | `recipe` |
| 요리했어요(요리 기록) | `cook` / `cookLog` |
| 식단표 / 식단 칸 | `mealPlan` / `mealPlanSlot` |
| 장바구니 | `cart` |
| 공유 레시피 | `sharedRecipe` |
| 생성 횟수 | `quota` |
| 알림 | `notification` |

## API 주소

- 모든 Controller 주소는 `/api/v1/{호출자}/{자원}`으로 시작하고, Controller에 직접 씁니다. 예: `@RequestMapping("/api/v1/user/ingredients")`
  - `public`: 로그인 전에 부르는 API(소셜 로그인, 토큰 재발급)
  - `user`: 로그인한 사용자. 대부분의 API. 토큰 주인의 데이터만 다룹니다
  - `admin`: 어드민(role=ADMIN)
- 자원은 복수형 케밥케이스로 씁니다(`saved-recipes`, `cart-items`). 동작은 자원 뒤에 동사로 붙입니다(`/recipes/generate`, `/receipts/scan`). 경로에 `me`를 쓰지 않습니다(`/users/me`가 아니라 `/user/profile`). 전체 API 목록은 [`spec/api-summary.md`](../agent/spec/api-summary.md)(영문)에 있습니다.
- 운영 서버는 `/api/`로 시작하는 요청만 백엔드로 보내고 나머지는 프론트로 보냅니다. 이 규칙을 어기면 로컬에서는 동작하지만 운영에서는 404가 납니다.
- `server.servlet.context-path`나 전역 경로 설정(`addPathPrefix`)으로 앞부분을 붙이지 않습니다. `context-path`는 헬스체크 주소(`/actuator/health/readiness`)까지 바꿔서 배포가 실패하고, 전역 설정은 Controller에 `/api`를 또 쓰면 `/api/api/...`가 됩니다.
- Security 설정의 `requestMatchers`에도 전체 주소를 씁니다.
  - `/api/v1/public/**`, `/actuator/health/**`: 인증 없이 허용. 헬스체크를 막으면 배포가 실패합니다.
  - `/api/v1/admin/**`: ADMIN만. 권한이 없으면 403.
  - 나머지 `/api/v1/**`: 로그인 필요.
- 로컬 개발에서는 프론트(`http://localhost:3000`)가 백엔드(`http://localhost:8080`)를 직접 부르므로 CORS를 열어 둡니다(`global/config/CorsConfig`, 허용 주소는 `remake.cors.allowed-origins`). 인증 쿠키를 보내려면 `allowCredentials(true)`와 정확한 출처 지정이 필요합니다. 허용 주소는 하나씩 적고(예: `http://localhost:3000`) `*`는 쓰지 않습니다. 운영은 같은 도메인이라 필요 없습니다.
- `/api/` 밖에 둬야 하는 주소(WebSocket, 파일 다운로드 등)가 필요하면 만들기 전에 배포 담당자에게 알립니다. 프록시 규칙을 추가해야 합니다.

## 응답과 에러

성공하면 Response DTO를 감싸지 않고 그대로 돌려줍니다. `ApiResponse`, `CommonResponse`처럼 성공 응답을 감싸는 클래스(`{ data, meta }` 등)는 만들지 않습니다. 성공과 실패는 HTTP 상태 코드로 구분합니다. 생성은 201, 본문 없는 삭제는 204, 접수만 하고 나중에 처리(이벤트 수집)는 202, 나머지 성공은 200입니다.

계속 늘어나는 목록은 아래 모양으로 페이징합니다(`?page=0&size=20`, `PageResponse.from(page, XxxResponse::from)`). 커서 방식은 쓰지 않습니다. 개수가 정해진 기준 데이터(재료 카테고리 등)는 페이징하지 않고 배열로 줍니다.

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "hasNext": false }
```

실패하면 모든 API가 아래 모양으로 돌려줍니다. `fieldErrors`(`{ field, message }` 항목)는 입력값 검증 실패(`INVALID_INPUT`)일 때만 넣습니다. `details`는 화면에 값이 더 필요할 때만 넣습니다. 예를 들어 `GENERATION_LIMIT_EXCEEDED`의 남은 횟수와 초기화 시각(`"details": { "remaining": 0, "resetsAt": "2026-11-21T00:00:00+09:00" }`)입니다. `fieldErrors`에 값이 없으면 필드를 빼지 않고 `null`로 돌려주고, `details`에 값이 없으면 필드를 뺍니다(`@JsonInclude(NON_NULL)`). `details`는 `new BusinessException(ErrorCode.XXX, Map.of(...))`나 `new BusinessException(ErrorCode.XXX, message, Map.of(...))`로 넘기면 `GlobalExceptionHandler`가 응답에 그대로 담습니다. 에러 코드 목록은 [`spec/errors.md`](../agent/spec/errors.md)(영문)에 있습니다.

```json
{
  "code": "INGREDIENT_NOT_FOUND",
  "message": "재료를 찾을 수 없습니다.",
  "status": 404,
  "traceId": "a1b2c3d4",
  "fieldErrors": [{ "field": "amount", "message": "0보다 커야 합니다." }]
}
```

- 날짜는 `2026-12-21`, 시각은 `2026-11-15T09:00:00+09:00` 형식으로 줍니다. 금액은 원 단위 정수입니다.
  - DTO에는 `LocalDate`, `LocalDateTime`을 그대로 씁니다. `+09:00`은 `global/config/JacksonConfig`가 붙이므로 DTO에서 `OffsetDateTime`이나 문자열로 바꾸지 않습니다. 초 아래는 버립니다.
  - 요청으로 받은 시각은 다른 시간대(`Z` 등)면 한국 시간으로 바꾸고, 시간대가 없으면 한국 시간으로 봅니다.
- 필드 이름은 camelCase, enum 값은 이름 그대로(대문자 스네이크, `FRIDGE`, `SHARED_COPY`) 줍니다. DTO에도 enum 타입을 그대로 씁니다.
- 값이 없으면 필드를 빼지 않고 `null`을 줍니다.
- 도메인 객체나 JPA Entity를 그대로 응답으로 내보내지 않고 Response DTO로 바꿔서 줍니다.
- 프론트는 OpenAPI 문서로 API 클라이언트를 만듭니다(`pnpm gen:api`). Controller의 반환 타입을 DTO로 분명히 쓰고, `Object`나 `Map`으로 돌려주지 않습니다. 문서는 로컬 백엔드의 `/v3/api-docs`, 화면은 `/swagger-ui.html`입니다. 로그인이 필요한 API는 Swagger에 `@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)`로 쿠키 인증(OpenAPI `apiKey`, `in: cookie`, 이름 `ACCESS_TOKEN`)을 표시합니다(#42).
- 에러는 Service에서 `BusinessException(ErrorCode.XXX)`로 던지기만 합니다. 응답은 `global/error`의 `GlobalExceptionHandler`가 한 곳에서 만듭니다. Controller와 Service에서 try-catch로 에러 응답을 만들지 않습니다.
- 에러 코드는 `global/error/ErrorCode` 하나에 모읍니다. 이름은 API 정의서·에러 정의서의 코드와 같게 씁니다.
- 로그인이 필요함(401)과 권한 없음(403)은 Security 필터 단계에서 막혀 `GlobalExceptionHandler`를 거치지 않습니다. `global/security`의 `AuthenticationEntryPoint`(401)와 `AccessDeniedHandler`(403)에서도 같은 모양으로 응답합니다.
- `Idempotency-Key` 헤더와 중복 요청 키 테이블은 없습니다. 등록·차감 같은 버튼은 프론트가 요청이 끝날 때까지 비활성으로 막습니다.
- 인증 방식은 [#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42)에서 A안으로 확정했습니다. 액세스(30분)·리프레시(14일) JWT를 모두 HttpOnly 쿠키(`Secure; SameSite=Lax`)로 둡니다. 토큰은 `Authorization` 헤더가 아니라 쿠키에서 읽습니다. 로그인·재발급 응답에는 `accessToken`을 넣지 않습니다.

### 예시 API

새 API는 재료 카테고리 목록(`GET /api/v1/user/ingredient-categories`)을 따라 만듭니다. 마이그레이션부터 테스트까지 한 벌이 다 있습니다.

| 위치 | 파일 |
|---|---|
| 마이그레이션 | `db/migration/V1__create_ingredient_categories.sql` |
| domain | `IngredientCategory`, `IngredientCategoryCode`, `StorageLocation`, `IngredientUnit` |
| port/in | `GetIngredientCategoriesUseCase` |
| port/out | `LoadIngredientCategoryPort` |
| service | `IngredientCategoryService` (없으면 `BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND)`) |
| adapter/in/web | `IngredientCategoryController`, `IngredientCategoryResponse` |
| adapter/out/persistence | `IngredientCategoryJpaEntity`, `IngredientCategoryJpaRepository`, `IngredientCategoryPersistenceAdapter` |
| 테스트 | `src/test/.../ingredient/IngredientCategoryApiTest` |

## 데이터 접근과 요청 처리

- 모든 조회는 토큰 주인의 데이터로 한정합니다. 다른 사용자의 리소스는 403이 아니라 404(`*_NOT_FOUND`)로 응답해서 있는지 없는지 알리지 않습니다.
- 요청마다 `traceId`를 만들어 로그(MDC)에 넣고 에러 응답에도 담습니다. 스택 트레이스, SQL, 외부 서비스 에러 문구는 응답에 넣지 않습니다.
- 시간 제한: 일반 요청 10초, OCR 30초, AI 레시피 생성 60초(식단표 생성은 202로 바로 응답하고 서버 작업으로 만들며, 작업 시간 제한은 미결, #144). 외부 서비스 시간 초과는 `OCR_TIMEOUT`·`AI_TIMEOUT`, 외부 서비스 오류는 `OCR_UPSTREAM_ERROR`·`AI_UPSTREAM_ERROR`로 바꿉니다.
- 비즈니스 규칙(횟수 제한, 단위, 소비기한 상태, 영수증 보관)은 [`spec/domain-rules.md`](../agent/spec/domain-rules.md)(영문)에, 테이블 단위 규칙(낙관적 락, 삭제, 보관)은 [ERD](erd.md)에 있습니다.

## AI 생성

- LLM은 기능마다 자기 `port/out` 인터페이스로 부르고, 구현은 그 기능의 `adapter/out/ai`에 둡니다. 다른 기능의 AI 포트를 같이 쓰지 않습니다. 입력은 요청의 ID·enum과 DB 데이터(냉장고 재료, 제외 재료 코드)뿐입니다. **사용자가 입력한 글을 프롬프트에 넣지 않습니다.**
- 응답은 구조화된 JSON으로 받아 검증합니다: 스키마, 단위는 14개 코드 중 하나, 조리 단계가 비어 있지 않음. 검증에 실패하면 `AI_INVALID_RESPONSE`이고, 저장하지 않으며 횟수도 차감하지 않습니다.
- "레시피 재료 보충" 설정이 ON이면 냉장고에 없는 재료도 쓸 수 있습니다. OFF면 냉장고 재료로만 만들고, 양념을 뺀 재료가 2개 미만이면(소비기한 지난 재료 제외) AI를 부르지 않고 `NO_INGREDIENTS`입니다. 기준 2개는 레시피 영역 담당이 정한 값이고(정책에는 "너무 적으면"만 있음) `GenerationIngredients.MIN_MAIN_INGREDIENTS`에 있습니다. AI에 보내는 제외(알레르기) 재료는 설정의 제외 재료(`GetUserGenerationPreferencesUseCase`, 지금 값. 빠른 생성·커스텀·다시 받기 모두 항상)에 커스텀에서 고른 칩을 더한 것입니다. 요청과 도메인 타입은 `AllergenCode`(`user/domain`, ERD 부록 B)이고, 모르는 코드는 `400 INVALID_INPUT`입니다.
- 식단표: 칸마다 생성 때 레시피 전체를 만듭니다(칸 상세를 나중에 만들지 않음). 같은 재료 수량을 여러 끼니에 겹쳐 쓰지 않게 계산합니다(각 칸 레시피 재료 기준, 칸별 예정 재료 테이블 없음). 재료로 만들 수 있는 일수까지만 생성하고(BASIC 최대 5일, PREMIUM 7일), 그다음 부족한 재료를 장바구니에 담습니다.
- 성공한 생성만 횟수를 차감합니다. 한도는 [`spec/domain-rules.md`](../agent/spec/domain-rules.md)(영문)에 있습니다. 횟수 기능(`quota`)은 남은 횟수를 저장하지 않고 `usage_logs`에서 오늘(레시피)·이번 달 1일(식단표) 이후 행을 세어 계산합니다. 차감은 결과를 저장하는 트랜잭션 안에서 `ConsumeRecipeQuotaUseCase`(레시피)나 `ConsumeMealPlanQuotaUseCase`(식단표)로 하고, 회원 단위 advisory lock을 걸어 동시에 요청해도 한도를 넘지 않습니다. `remake.quota.enforced=false`면 한도 검사만 끕니다(기록은 남음). 이 값은 `application-local.yml`에만 있고(`QUOTA_ENFORCED`, 로컬 기본 `false`) 개발자가 자기 Gemini 키로 반복 테스트하기 위한 것입니다. 운영과 테스트는 기본값 `true`입니다. 개발자는 각자 자기 무료 키를 `backend/.env`에 넣습니다.
- LLM은 Google Gemini(무료 티어)입니다. `GenerateRecipeAiPort`(`recipe/application/port/out`)를 `GeminiRecipeAdapter`(`recipe/adapter/out/ai`)가 구현합니다. 식단표는 한 번에 여러 칸을 채우고 다른 기능의 `Port`는 쓸 수 없어서 자기 포트 `GenerateMealPlanAiPort`(`mealplan/application/port/out`)를 둡니다. 이 포트는 `GeminiMealPlanAdapter`(`mealplan/adapter/out/ai`)가 구현하고, 한 번에 모든 칸을 받고 서버 작업으로 돌기 때문에 응답 대기 90초인 자기 `RestClient`를 씁니다. 두 어댑터 모두 서버에 기록을 남기지 않는 `generateContent` API에 `responseMimeType: application/json`과 `responseSchema`를 지정해 부르고, 규칙은 시스템 지시에, 데이터(옵션 코드, 냉장고 재료)는 사용자 메시지에 JSON으로 넣습니다. 냉장고 재료 이름은 DB 값이지만 사용자가 입력한 글자라서, 지시가 아니라 데이터로 다루라고 시스템 지시에 적습니다.
- 설정: `remake.gemini.api-key`(`GEMINI_API_KEY`), `remake.gemini.model`(`GEMINI_MODEL`, 기본 `gemini-3.5-flash-lite`). 키는 로컬은 `backend/.env`, 운영은 서버 `.env`에 넣고 `compose.prod.yml`이 백엔드에 넘깁니다. 운영 키는 운영 전용 Google Cloud 프로젝트의 키라 로컬에서 쓰지 않습니다. 키가 비어 있으면 `AI_UPSTREAM_ERROR`가 납니다. 키를 요청 본문이나 로그에 넣지 않습니다.
- 어댑터 호출 한 번은 25초 제한이고 재시도하지 않습니다. 재시도는 Service가 1회 하므로(`AI_INVALID_RESPONSE` 뒤에도) 두 번 불러도 API 제한 60초 안에 끝납니다. 식단표: 어댑터 호출 한 번은 90초 제한이고(`GeminiMealPlanClientConfig.READ_TIMEOUT`, 실측 뒤 조정), `MealPlanGenerationJobService`가 `GeneratedMealPlanValidator`로 칸을 검사하고 같은 AI 오류 세 가지에서 1.5초 뒤 1회 재시도합니다. 공급자 과부하(Gemini 503, `MealPlanAiBusyException`, 코드는 그대로 `AI_UPSTREAM_ERROR`)는 3·5·7·9초, 그다음은 10초씩 쉬며 최대 9번 더 시도합니다(총 10번, 쉬는 시간 합 74초, `remake.meal-plan.ai-busy-retry-delays`). 서버 작업이라 사용자가 기다리지 않고, 11-24 실측에서 503이 잦았기 때문입니다. 거절도 AI Studio 사용량에 요청으로 세어지므로, 레시피 생성과 같이 쓰는 무료 분당 한도를 아끼려고 몰아서 보내지 않고 간격을 둡니다. "1회 재시도" 정책과 달라 PO 확인이 필요한 제안입니다. 요청한 칸이 빠지거나 겹치거나 레시피 한도를 어기면 응답 전체를 `AI_INVALID_RESPONSE`로 봅니다(칸 하나만 버리면 그날부터 식단이 잘림). 에러 변환: 시간 초과 → `AI_TIMEOUT`, HTTP 오류(429·503 포함)·연결 실패·안전 필터 거절·키 없음 → `AI_UPSTREAM_ERROR`, JSON 아님·후보 없음·`MAX_TOKENS` → `AI_INVALID_RESPONSE`. 공급자 쪽은 상태 코드만 로그에 남기고 에러 문구는 남기지 않습니다.
- `GeneratedRecipeValidator`가 AI 응답을 `Recipe`로 바꿉니다: 인분은 요청 값을 쓰고, 14개 코드에 없는 단위의 재료는 그 줄만 빼고(남은 줄이 없으면 `AI_INVALID_RESPONSE`), 모르는 냉장고 ID는 "냉장고에 없는 재료"로 보고, `TO_TASTE`는 양을 비우고, 양은 소수 셋째 자리로 반올림합니다. 재료 보충이 OFF면 냉장고에 없는 재료는 모두 `AI_INVALID_RESPONSE`입니다. 기본 양념(소금, 식용유)과 `PINCH`·`TO_TASTE` 줄도 예외가 아닙니다. 양념도 다른 재료처럼 사용자가 등록해 관리하는 품목입니다(#158). 예외는 물 하나입니다(`GeneratedRecipeValidator.WATER`, 이름이 정확히 "물", 레시피 영역 담당 결정). 물은 냉장고에 등록하는 품목이 아니라서 냉장고에 없어도 쓰고, 부족한 재료로 세지 않습니다(이벤트 `shortage_count`, 장바구니). `PINCH`·`TO_TASTE`는 "요리 완료 때 차감하지 않는다"는 뜻일 뿐입니다.
- 무료 티어 한도는 Google Cloud 프로젝트 단위로 분당 15회, 하루 500회입니다(미국 태평양 시간 자정에 초기화). 레시피 한 건은 약 800토큰, 5~10초입니다. 식단표 생성은 칸마다 모델을 부르면 안 되고, 식단표 하나를 한 번(또는 몇 번)에 만들어야 분당 15회 안에 들어옵니다.
- 레시피를 보여 주는 목록 한 줄(저장 목록 33, 식단표 40·42, 공유 49·50)은 `IngredientSummary.of`(`recipe/domain`)로 `ingredientSummary`·`extraIngredientCount`를 만듭니다. 레시피 재료 순서대로 앞 3개 이름을 `" · "`로 잇고 나머지 개수를 줍니다(화면은 "+N", 0이면 생략). 물·`PINCH`·`TO_TASTE` 줄도 그대로 셉니다(레시피 영역 담당 결정, 피그마 p4.1·p4.1.4). 다른 기능은 레시피 한 줄을 `GetRecipeListItemsUseCase`(레시피 id 목록으로 제목·요약·즐겨찾기)로 읽습니다. 공유 영역은 남의 공개 레시피 요약을 `GetRecipeIngredientSummariesUseCase`(레시피 id 목록 → id별 `IngredientSummary`)로 한 번에 읽습니다. 볼 수 있는지 확인하지 않으므로 부르는 쪽이 숨기지 않은(VISIBLE) 공유 글의 원본 레시피 id만 넘깁니다.
- 저장 목록 `AVAILABLE`(바로 가능): 물을 뺀 모든 재료가 내 냉장고에 같은 이름으로, 소비기한 안에 있는 레시피입니다. 양은 단위 환산(#63) 전까지 비교하지 않습니다. 저장한 공유 레시피는 다른 사람 냉장고의 재료 id를 가리키므로 이름으로 비교합니다.
- 단위 환산은 `UnitConversion`(`recipe/domain`, #63) 한 곳에서만 합니다. 무게↔무게, 부피↔부피(큰술 15ml·작은술 5ml·컵 200ml), 개수 단위는 같은 단위끼리만, `PINCH`·`TO_TASTE`는 바꾸지 않고, 결과는 소수 셋째 자리에서 반올림합니다. 식단표 배정(`StockUnits`)과 장바구니 → 냉장고(`CartFridgeQuantity`)도 이 클래스를 씁니다. 환산 표를 따로 만들지 않습니다.
- 요리 미리보기(31)는 레시피 재료마다 같은 이름이고 소비기한이 지나지 않은 내 냉장고 재료 중 소비기한이 가장 빠른 것과 짝짓습니다. 물·`PINCH`·`TO_TASTE`·양 없는 줄은 뺍니다. 양은 냉장고 단위로 바꾸고 남은 양을 넘지 않게 하며, 종류가 다르면(1개 ↔ 300g, 영수증 `PACK` 재고 ↔ g, #145) `null`로 두어 사용자가 정합니다. 요리 완료(32)는 `ConsumeIngredientUseCase`(via `COOK`, E25 발행)로 모두 함께 성공하거나 실패하게 빼고, 오늘 식단 칸은 `MarkMealSlotCookedUseCase`로 표시합니다.

## 예약 작업 (한국 시간)

| 작업 | 시각 | 하는 일 |
| --- | --- | --- |
| 소비기한 알림 | 매일 09:00 | `EXPIRY_SOON`(3일 이하, 재료마다), `EXPIRED`(하루 한 건) 생성(앱 안 알림만, 기기 푸시 없음). 알림을 끈 사용자는 제외. `ingredient_expired`, `notification_sent` 발행 |
| 주간 랭킹·보상 | 월요일 0시 | 지난주 TOP 100을 `shared_recipe_weekly_ranks`에 확정하고, 상위 레시피 3개의 작성자에게 레시피 생성 +10/+6/+3회(한 달 유효)를 한 트랜잭션에서 지급. 규칙은 [ERD](erd.md) 22번 테이블. `weekly_reward_granted` 발행 |
| 영수증 정리 | 매일 새벽 | 구매일 기준 6개월 지난 영수증을 이미지·품목과 함께 삭제 |
| 구독 자동 결제 | 매일 새벽 | 다음 결제일이 오늘인 ACTIVE 구독을 billingKey로 결제하고 한 달 연장, CANCELED 구독은 끝냄(`users.plan` BASIC). 실패 처리는 미결(사용자에게 질문) |
| 이벤트 보관 | 매일 새벽 | 180일 지난 원본 이벤트 삭제. 일별 집계 테이블 없음 |

- 하루·한 달 횟수 초기화 작업은 없습니다. 남은 횟수는 `usage_logs`로 계산합니다. 그 밖의 정리 작업은 ERD의 구현 규칙에 있습니다.

## 이벤트 발행 (`EventTracker`)

이벤트 계약은 [`spec/events.md`](../agent/spec/events.md)(영문)에 있습니다. API별 발행 이벤트는 [`spec/api-summary.md`](../agent/spec/api-summary.md)(영문)의 "Events" 열에 있습니다.

- 트래커는 `event` 기능이 하나만 가집니다. 다른 기능은 `event` 기능의 UseCase로만 부릅니다(제안: `eventTracker.track(EventName.RECEIPT_SAVED, Screen.RECEIPT_RESULT, props)`, 최종 시그니처는 이벤트 담당자가 정함).
- API가 끝나는 지점에서 성공·실패·거절 모두 정의서대로 발행합니다. 사용자 행동 하나는 CLIENT나 SERVER 중 한쪽에서 한 번만 기록합니다.
- 트랜잭션 커밋 뒤에 비동기로 저장합니다. 트래킹은 요청을 막거나 실패시키지 않습니다. 오류는 삼키고 로그만 남깁니다.
- 이벤트 이름과 속성은 이벤트 정의서와 같은 enum이나 상수 클래스로 관리합니다. 이벤트 이름을 문자열로 직접 쓰지 않습니다.
- 트래커가 개인정보를 거릅니다: 그 이벤트의 허용 속성 목록에 없는 키는 버리고, 25개를 넘거나 중첩된 객체는 거부합니다. 속성은 문자·숫자·참거짓만 평평하게 둡니다.
- 공통 값을 채웁니다: `user_id`(내부 ID), `session_id`, `source`(CLIENT/SERVER), `screen`, `platform`, `app_version`, `is_seed`, `occurred_at`.
- `POST /api/v1/user/events`는 클라이언트 이벤트를 최대 50개 받고, 모르는 이름은 버리고, 202 `{ accepted, rejected }`로 응답합니다. 50개를 넘으면 `EVENT_BATCH_TOO_LARGE`.

## 꼭 써야 하는 테스트

테스트 작성 방법: [백엔드 테스트](backend-testing.md).

- 비즈니스 규칙 단위 테스트: 횟수 계산(성공만 차감, 같은 회원 동시 요청), 단위 환산(같은 종류끼리만; 큰술 15ml, 작은술 5ml, 컵 200ml; 꼬집·약간은 차감 안 함), 소비기한 상태, 식단표 재료 배정과 부족 계산, 랭킹과 `rankChange`, 중복 영수증, 영수증 보관.
- OCR·LLM 어댑터는 가짜로 바꾸고 시간 초과·잘못된 응답 경우를 테스트합니다.
- 어드민 API(어드민 화면이 나올 때까지 보류): 어드민이 아니면 403인지 테스트합니다.
