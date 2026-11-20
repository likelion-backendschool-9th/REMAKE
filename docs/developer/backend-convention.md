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
│           └── ai / ocr / oauth / filestorage / push / mail  외부 서비스 호출 (필요한 기능만)
│
└── global                      모든 기능이 같이 쓰는 것
    ├── error                   ErrorCode, BusinessException, GlobalExceptionHandler
    ├── config                  Swagger, JPA, CORS, Clock, 외부 서비스 공통 설정
    ├── security                JWT 필터, Security 설정
    └── common                  BaseCreatedEntity, BaseTimeEntity, 중복 요청 방지 필터 같은 공통 클래스
```

기능 폴더와 테이블(ERD v3):

| 기능 폴더 | 테이블 |
|---|---|
| `user` | users, user_excluded_ingredients, account_deletion_requests |
| `auth` | refresh_tokens |
| `ingredient` | ingredient_categories, ingredient_catalog, ingredients, ingredient_logs |
| `receipt` | receipts, receipt_items |
| `recipe` | recipes, recipe_ingredients, recipe_steps, recipe_catalog_items, saved_recipes, recipe_cook_logs |
| `mealplan` | meal_plans, meal_plan_slots, meal_plan_slot_ingredients |
| `cart` | cart_items |
| `share` | shared_recipes, shared_recipe_photos, shared_recipe_reports |
| `quota` | usage_logs, generation_quota_grants |
| `notification` | notifications, push_subscriptions |
| `event` | events, event_daily_stats |
| `admin` | admin_audit_logs. 어드민 화면의 기능(신고 처리, 횟수 조정, 계정 제한)은 각 기능의 UseCase를 부릅니다 |
| `global/common` | idempotency_keys |

지킬 것:

- 의존은 `adapter → application → domain` 한 방향입니다. `domain`에는 Spring, JPA 어노테이션을 쓰지 않습니다.
- Controller는 `UseCase` 인터페이스만 부릅니다.
- Service는 자기 기능의 `Port` 인터페이스와 다른 기능의 `UseCase` 인터페이스만 부릅니다.
- 공통 클래스는 JPA Entity만 상속합니다. domain 객체는 상속하지 않습니다.
  - 값이 수정되는 테이블(users, ingredients, receipts, receipt_items, meal_plan_slots, cart_items, shared_recipes)은 `BaseTimeEntity`(`created_at` + `updated_at`)를 상속합니다.
  - 나머지 테이블은 `BaseCreatedEntity`(`created_at`)를 상속합니다.
  - 연결 테이블, 기준 데이터, 일별 집계, `events`는 상속하지 않습니다.

### 다른 기능의 데이터 쓰기

- 다른 기능의 데이터가 필요하면 그 기능의 `UseCase`(port/in)를 부릅니다. 다른 기능의 `Port`(port/out)나 adapter, Repository는 쓰지 않습니다.
- 다른 기능의 데이터는 domain 객체와 JPA Entity 모두 `Long ingredientId`처럼 ID만 가집니다. 다른 기능의 Entity와 JPA 연관관계(`@ManyToOne` 등)를 맺지 않습니다. DB의 외래 키는 그대로 둡니다.
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
| 웹 푸시 발송 | `adapter/out/push` | `notification/adapter/out/push` |
| 메일 발송 (계정 삭제 인증번호) | `adapter/out/mail` | `user/adapter/out/mail` |

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
- 자원은 복수형 케밥케이스로 씁니다(`saved-recipes`, `cart-items`). 동작은 자원 뒤에 동사로 붙입니다(`/recipes/generate`, `/receipts/{id}/confirm`).
- 운영 서버는 `/api/`로 시작하는 요청만 백엔드로 보내고 나머지는 프론트로 보냅니다. 이 규칙을 어기면 로컬에서는 동작하지만 운영에서는 404가 납니다.
- `server.servlet.context-path`나 전역 경로 설정(`addPathPrefix`)으로 앞부분을 붙이지 않습니다. `context-path`는 헬스체크 주소(`/actuator/health/readiness`)까지 바꿔서 배포가 실패하고, 전역 설정은 Controller에 `/api`를 또 쓰면 `/api/api/...`가 됩니다.
- Security 설정의 `requestMatchers`에도 전체 주소를 씁니다.
  - `/api/v1/public/**`, `/actuator/health/**`: 인증 없이 허용. 헬스체크를 막으면 배포가 실패합니다.
  - `/api/v1/admin/**`: ADMIN만. 권한이 없으면 403.
  - 나머지 `/api/v1/**`: 로그인 필요.
- 로컬 개발에서는 프론트(`http://localhost:3000`)가 백엔드(`http://localhost:8080`)를 직접 부르므로 CORS를 열어 둡니다. 운영은 같은 도메인이라 필요 없습니다.
- `/api/` 밖에 둬야 하는 주소(WebSocket, 파일 다운로드 등)가 필요하면 만들기 전에 배포 담당자에게 알립니다. 프록시 규칙을 추가해야 합니다.

## 응답과 에러

성공하면 Response DTO를 감싸지 않고 그대로 돌려줍니다. 성공과 실패는 HTTP 상태 코드로 구분합니다. 생성은 201, 본문 없는 삭제는 204, 접수만 하고 나중에 처리(이벤트 수집)는 202, 나머지 성공은 200입니다.

목록은 아래 모양으로 페이징합니다(`?page=0&size=20`).

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "hasNext": false }
```

실패하면 모든 API가 아래 모양으로 돌려줍니다. `fieldErrors`는 입력값 검증 실패일 때만 넣습니다.

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
- 값이 없으면 필드를 빼지 않고 `null`을 줍니다.
- 도메인 객체나 JPA Entity를 그대로 응답으로 내보내지 않고 Response DTO로 바꿔서 줍니다.
- 프론트는 OpenAPI 문서로 API 클라이언트를 만듭니다(`pnpm gen:api`). Controller의 반환 타입을 DTO로 분명히 쓰고, `Object`나 `Map`으로 돌려주지 않습니다.
- 에러는 Service에서 `BusinessException(ErrorCode.XXX)`로 던지기만 합니다. 응답은 `global/error`의 `GlobalExceptionHandler`가 한 곳에서 만듭니다. Controller와 Service에서 try-catch로 에러 응답을 만들지 않습니다.
- 에러 코드는 `global/error/ErrorCode` 하나에 모읍니다. 이름은 API 정의서·에러 정의서의 코드와 같게 씁니다.
- 로그인이 필요함(401)과 권한 없음(403)은 Security 필터 단계에서 막혀 `GlobalExceptionHandler`를 거치지 않습니다. `global/security`의 `AuthenticationEntryPoint`(401)와 `AccessDeniedHandler`(403)에서도 같은 모양으로 응답합니다.
- 생성·차감·확정 POST(레시피 생성, 요리했어요, 영수증 확정 등)는 `Idempotency-Key` 헤더를 받습니다. 같은 키로 다시 오면 처리하지 않고 처음 응답을 그대로 돌려줍니다(24시간, `idempotency_keys` 테이블). 처리는 기능마다 만들지 않고 `global/common`의 공통 필터 하나가 맡습니다.
