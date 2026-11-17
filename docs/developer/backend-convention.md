# REMAKE 백엔드 코드 컨벤션

> 에이전트용 영문판: [`docs/agent/backend-convention.md`](../agent/backend-convention.md)

## 코드 스타일

- Java 코드와 JSON은 camelCase, DB는 snake_case로 씁니다.

## 폴더 구조

헥사고날 아키텍처로 만듭니다. 기능별로 묶고, 기능 폴더 안은 모두 같은 모양으로 둡니다.

```
com.remake
├── ingredient / recipe / receipt / notification / user / auth
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
│           └── ai / ocr / oauth / storage  외부 서비스 호출 (필요한 기능만)
│
└── global                      모든 기능이 같이 쓰는 것
    ├── error                   ErrorCode, BusinessException, GlobalExceptionHandler
    ├── config                  Swagger, JPA, CORS, 외부 서비스 공통 설정
    ├── security                JWT 필터, Security 설정
    └── common                  BaseTimeEntity 같은 공통 클래스
```

지킬 것:

- 의존은 `adapter → application → domain` 한 방향입니다. `domain`에는 Spring, JPA 어노테이션을 쓰지 않습니다.
- Controller는 `UseCase` 인터페이스만 부릅니다.
- Service는 자기 기능의 `Port` 인터페이스와 다른 기능의 `UseCase` 인터페이스만 부릅니다.
- `BaseTimeEntity`는 JPA Entity만 상속합니다. domain 객체는 상속하지 않습니다.

### 다른 기능의 데이터 쓰기

- 다른 기능의 데이터가 필요하면 그 기능의 `UseCase`(port/in)를 부릅니다. 다른 기능의 `Port`(port/out)나 adapter, Repository는 쓰지 않습니다.
- 다른 기능의 데이터는 domain 객체와 JPA Entity 모두 `Long ingredientId`처럼 ID만 가집니다. 다른 기능의 Entity와 JPA 연관관계(`@ManyToOne` 등)를 맺지 않습니다. DB의 외래 키는 그대로 둡니다.
- 같은 기능 안에서는 객체로 묶고 JPA 연관관계를 써도 됩니다. 예: `Recipe`와 `RecipeStep`.

### 필요한 기능만 만드는 폴더

`adapter/in/web`과 `adapter/out/persistence` 말고 다른 연결이 필요하면, 그 기능을 맡은 사람이 필요할 때 아래 이름으로 폴더를 만듭니다.

| 연결 대상 | 위치 | 예 |
|---|---|---|
| 정해진 시각에 저절로 도는 작업 | `adapter/in/scheduler` | 임박 알림 만들기, 영수증 정리 작업 |
| AI (레시피 생성, 카테고리 추정) | `adapter/out/ai` | `recipe/adapter/out/ai` |
| OCR (영수증 글자 읽기) | `adapter/out/ocr` | `receipt/adapter/out/ocr` |
| 소셜 로그인 | `adapter/out/oauth` | `auth/adapter/out/oauth` |
| 파일 저장 (영수증 사진) | `adapter/out/storage` | `receipt/adapter/out/storage` |

- 스케줄러는 Controller처럼 `UseCase`만 부릅니다.
- Service는 외부 서비스를 직접 부르지 않고 `port/out` 인터페이스만 부릅니다. 외부 서비스 호출 코드는 위 adapter 폴더에서 그 인터페이스를 구현합니다.
- 여러 기능이 같이 쓰는 외부 서비스 설정(API 키, HTTP 클라이언트)은 `global/config`에 하나만 둡니다.
- 표에 없는 연결이 필요하면 이름을 정해서 이 표에 먼저 추가합니다.

### 클래스 이름 예시 (재료 기능)

| 위치 | 이름 |
|---|---|
| domain | `Ingredient` |
| port/in | `CreateIngredientUseCase`, `GetIngredientsUseCase` |
| port/out | `LoadIngredientPort`, `SaveIngredientPort` |
| service | `IngredientService` |
| adapter/in/web | `IngredientController`, `IngredientCreateRequest`, `IngredientResponse` |
| adapter/out/persistence | `IngredientJpaEntity`, `IngredientJpaRepository`, `IngredientPersistenceAdapter` |

## 용어

코드, DB, API에서 같은 단어를 씁니다.

| 화면의 말 | 코드의 단어 |
|---|---|
| 회원 | `user` |
| 재료 | `ingredient` |
| 소비기한 | `expirationDate` |
| 보관 위치 | `storage` |
| 영수증 | `receipt` |
| 레시피 | `recipe` |
| 알림 | `notification` |

## 응답과 에러

성공하면 Response DTO를 감싸지 않고 그대로 돌려줍니다. 성공과 실패는 HTTP 상태 코드로 구분합니다. 생성은 201, 본문 없는 삭제는 204, 나머지 성공은 200입니다. 실패하면 모든 API가 아래 모양으로 돌려줍니다.

```json
{
  "code": "INGREDIENT_NOT_FOUND",
  "message": "재료를 찾을 수 없습니다."
}
```

- 날짜는 `2026-12-21`, 시각은 `2026-11-15T09:00:00+09:00` 형식으로 줍니다.
- 값이 없으면 필드를 빼지 않고 `null`을 줍니다.
- 도메인 객체나 JPA Entity를 그대로 응답으로 내보내지 않고 Response DTO로 바꿔서 줍니다.
- 에러는 Service에서 `BusinessException(ErrorCode.XXX)`로 던지기만 합니다. 응답은 `global/error`의 `GlobalExceptionHandler`가 한 곳에서 만듭니다. Controller와 Service에서 try-catch로 에러 응답을 만들지 않습니다.
- 에러 코드는 `global/error/ErrorCode` 하나에 모읍니다.
