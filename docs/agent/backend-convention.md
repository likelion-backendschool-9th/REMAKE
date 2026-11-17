# Backend Code Convention

> Korean version for developers: [`docs/developer/backend-convention.md`](../developer/backend-convention.md)

## Code style

- Use camelCase in Java code and JSON, snake_case in the DB.

## Folder structure

Use hexagonal architecture. Group code by feature, and give every feature folder the same shape.

```
com.remake
├── ingredient / recipe / receipt / notification / user / auth
│   ├── domain                  Core objects and rules (no Spring, no JPA)
│   ├── application
│   │   ├── port
│   │   │   ├── in              UseCase interfaces (inbound)
│   │   │   └── out             Port interfaces (outbound)
│   │   └── service             UseCase implementations; throws exceptions
│   └── adapter
│       ├── in
│       │   ├── web             Controllers, Request/Response DTOs
│       │   └── scheduler       Scheduled jobs (only where needed)
│       └── out
│           ├── persistence     JPA entities, repositories, Port implementations
│           └── ai / ocr / oauth / storage  External service calls (only where needed)
│
└── global                      Shared by all features
    ├── error                   ErrorCode, BusinessException, GlobalExceptionHandler
    ├── config                  Swagger, JPA, CORS, shared external service config
    ├── security                JWT filter, Security config
    └── common                  Shared classes such as BaseTimeEntity
```

Rules:

- Dependencies point one way: `adapter → application → domain`. Do not use Spring or JPA annotations in `domain`.
- Controllers call only `UseCase` interfaces.
- Services call only their own feature's `Port` interfaces and other features' `UseCase` interfaces.
- Only JPA entities extend `BaseTimeEntity`. Domain objects do not.

### Using another feature's data

- When you need another feature's data, call that feature's `UseCase` (port/in). Do not use another feature's `Port` (port/out), adapters, or repositories.
- Reference another feature's data by ID only, such as `Long ingredientId`, in both domain objects and JPA entities. Do not create JPA associations (`@ManyToOne`, etc.) to another feature's entities. Keep the DB foreign keys.
- Within one feature, compose objects and use JPA associations freely. Example: `Recipe` and `RecipeStep`.

### Folders created only where needed

When a feature needs a connection other than `adapter/in/web` and `adapter/out/persistence`, the feature owner creates the folder when needed, using the names below.

| Connection | Location | Example |
|---|---|---|
| Jobs that run automatically on a schedule | `adapter/in/scheduler` | Creating expiry alerts, receipt cleanup |
| AI (recipe generation, category estimation) | `adapter/out/ai` | `recipe/adapter/out/ai` |
| OCR (reading receipt text) | `adapter/out/ocr` | `receipt/adapter/out/ocr` |
| Social login | `adapter/out/oauth` | `auth/adapter/out/oauth` |
| File storage (receipt photos) | `adapter/out/storage` | `receipt/adapter/out/storage` |

- Schedulers call only `UseCase` interfaces, like controllers.
- Services never call external services directly; they call `port/out` interfaces only. External call code implements those interfaces in the adapter folders above.
- Keep external service config shared by several features (API keys, HTTP clients) in one place in `global/config`.
- If you need a connection not in the table, choose a name and add it to this table first.

### Class name examples (ingredient feature)

| Location | Name |
|---|---|
| domain | `Ingredient` |
| port/in | `CreateIngredientUseCase`, `GetIngredientsUseCase` |
| port/out | `LoadIngredientPort`, `SaveIngredientPort` |
| service | `IngredientService` |
| adapter/in/web | `IngredientController`, `IngredientCreateRequest`, `IngredientResponse` |
| adapter/out/persistence | `IngredientJpaEntity`, `IngredientJpaRepository`, `IngredientPersistenceAdapter` |

## Terms

Use the same word in code, DB, and API.

| UI term (Korean) | Code word |
|---|---|
| 회원 (member) | `user` |
| 재료 (ingredient) | `ingredient` |
| 소비기한 (expiration date) | `expirationDate` |
| 보관 위치 (storage location) | `storage` |
| 영수증 (receipt) | `receipt` |
| 레시피 (recipe) | `recipe` |
| 알림 (notification) | `notification` |

## Responses and errors

On success, return the Response DTO as-is without a wrapper. Distinguish success and failure by HTTP status code: 201 for creation, 204 for deletion without a body, 200 for other successes. On failure, every API returns this shape:

```json
{
  "code": "INGREDIENT_NOT_FOUND",
  "message": "재료를 찾을 수 없습니다."
}
```

- Format dates as `2026-12-21` and timestamps as `2026-11-15T09:00:00+09:00`.
- When a value is missing, return the field as `null`; do not omit it.
- Never return domain objects or JPA entities directly; convert them to Response DTOs.
- Services only throw `BusinessException(ErrorCode.XXX)`. `GlobalExceptionHandler` in `global/error` builds every error response in one place. Do not build error responses with try-catch in controllers or services.
- Keep all error codes in the single `global/error/ErrorCode`.
