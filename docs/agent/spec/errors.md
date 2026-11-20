# Error contract

Source: API exception and error definition (Korean, kept in Notion / Google Sheets; not in the repo). Codes are fixed UPPER_SNAKE_CASE; never rename. Clients branch on `code`; `message` is Korean user-facing text.

## Response body

Same shape as [`../backend-convention.md`](../backend-convention.md) §Responses and errors: `{ code, message, status, traceId, fieldErrors?, details? }`.

```json
{
  "code": "INVALID_INPUT",
  "message": "입력값을 확인해 주세요",
  "status": 400,
  "traceId": "9f2c1e7a5b",
  "fieldErrors": [{ "field": "amount", "message": "300 이하로 입력해 주세요" }]
}
```

```json
{
  "code": "GENERATION_LIMIT_EXCEEDED",
  "message": "오늘 생성 횟수를 모두 썼어요",
  "status": 429,
  "traceId": "4c8d2a1f0e",
  "details": { "remaining": 0, "resetsAt": "2026-11-21T00:00:00+09:00" }
}
```

- `fieldErrors` only for `INVALID_INPUT`. Each item is `{ field, message }`; `message` is Korean text shown under the input.
- `details` only when the screen needs extra values (remaining count, reset time). Omit it otherwise. Not in `global/error/ErrorResponse` yet ([#45](https://github.com/likelion-backendschool-9th/REMAKE/issues/45)).
- Never include stack traces, SQL or provider error text.

## HTTP usage

- 400 rule/input violation; 401 no/expired auth; 403 forbidden or restricted account; 404 missing **or not owned** (do not leak existence); 409 state conflict; 413/415 file size/type; 422 well-formed but unprocessable; 429 limit/rate; 5xx server or upstream (AI/OCR) problems, not the user fault.
- Failed AI generation and any 5xx/rollback **never consume quota**.
- Query endpoints with no data return 200 with empty arrays/0 (e.g. month with no receipts), not an error.

## Severity and logging

INFO: normal flow caused by input/state. WARN: limits, permissions, format. ERROR: server/AI/OCR problems, logged with traceId and counted for admin alerts.

## Codes (59 from the error definition + 5 code-only)

Code-only rows are in `global/error/ErrorCode` but not in the planning sheet: 4 common framework codes and feature codes that other features use internally.

| Code | HTTP | Severity | Meaning |
|---|---|---|---|
| `INVALID_INPUT` | 400 | INFO | Missing/invalid field, bad month/date/sort, receipt older than 6 months (fieldErrors) |
| `UNAUTHORIZED` | 401 | INFO | No/forged access token -> go to login |
| `TOKEN_EXPIRED` | 401 | INFO | Access token expired -> refresh once, retry once |
| `FORBIDDEN` | 403 | WARN | Not allowed (another user's admin-only resource, non-admin on /admin) |
| `ACCOUNT_SUSPENDED` | 403 | WARN | Account restricted by admin; only logout allowed |
| `CONCURRENT_UPDATE` | 409 | WARN | Optimistic lock conflict on ingredient (version); reload and retry |
| `RATE_LIMITED` | 429 | WARN | Too many requests (honor Retry-After) |
| `INTERNAL_ERROR` | 500 | ERROR | Unexpected server error; no quota consumed; transaction rolled back |
| `SERVICE_UNAVAILABLE` | 503 | ERROR | Maintenance / DB down; retry after 30 s |
| `NOT_FOUND` | 404 | INFO | Common (code only): no such URL. A missing resource uses its own `*_NOT_FOUND` code |
| `METHOD_NOT_ALLOWED` | 405 | WARN | Common (code only): HTTP method not supported on this path |
| `NOT_ACCEPTABLE` | 406 | WARN | Common (code only): the requested response format (`Accept`) is not supported |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | WARN | Common (code only): the request body format (`Content-Type`) is not supported. An uploaded file of the wrong type is `UNSUPPORTED_FILE_TYPE` |
| `INGREDIENT_CATEGORY_NOT_FOUND` | 404 | INFO | Feature (code only): no ingredient category with that ID or code. Thrown by the ingredient category UseCase that other features (ingredient registration, receipts) call; not returned by its own list API |
| `NETWORK_OFFLINE` | (클라이언트) | INFO | Client only: device offline |
| `CLIENT_TIMEOUT` | (클라이언트) | WARN | Client only: timeout (10 s / OCR 30 s / AI 60 s); retry with same Idempotency-Key |
| `OAUTH_FAILED` | 401 | INFO | Social auth failed or cancelled |
| `OAUTH_PROVIDER_UNSUPPORTED` | 400 | WARN | provider not kakao/naver/google |
| `REFRESH_TOKEN_INVALID` | 401 | INFO | Refresh token expired/revoked/forged -> login |
| `CATALOG_ITEM_NOT_FOUND` | 404 | WARN | Excluded-ingredient catalog id does not exist |
| `ACCOUNT_EMAIL_MISSING` | 422 | WARN | No `users.email` and deletion-codes body omitted `email`. Client must collect an alternate email and retry with `{ email }` ([#54](https://github.com/likelion-backendschool-9th/REMAKE/issues/54) A) |
| `DELETION_CODE_RESEND_TOO_SOON` | 429 | INFO | Resend within 30 s |
| `DELETION_CODE_INVALID` | 400 | INFO | Wrong code (details.remaining attempts) |
| `DELETION_CODE_EXPIRED` | 400 | INFO | Code older than 5 min |
| `DELETION_CODE_LOCKED` | 429 | WARN | 5 wrong attempts; request a new code |
| `INGREDIENT_NOT_FOUND` | 404 | INFO | Missing or not owned |
| `INSUFFICIENT_QUANTITY` | 400 | INFO | Removing more than owned |
| `INVALID_UNIT` | 400 | INFO | Unit not in the 14-unit list or not allowed for the item |
| `INGREDIENT_ALREADY_FINISHED` | 409 | INFO | Using/discarding a FINISHED ingredient |
| `INGREDIENT_HAS_LOGS` | 409 | INFO | Cannot delete: use/discard history exists |
| `FILE_TOO_LARGE` | 413 | INFO | Upload over limit (receipt image 10 MB) |
| `UNSUPPORTED_FILE_TYPE` | 415 | INFO | Not jpg/png/heic (or allowed photo type) |
| `RECEIPT_NOT_RECOGNIZED` | 422 | WARN | Image is not a receipt / nothing readable |
| `OCR_UPSTREAM_ERROR` | 502 | ERROR | OCR provider error (502) |
| `OCR_TIMEOUT` | 504 | ERROR | OCR exceeded 30 s |
| `RECEIPT_NOT_FOUND` | 404 | INFO | Missing or not owned |
| `GENERATION_LIMIT_EXCEEDED` | 429 | WARN | Daily recipe generation limit reached (details: remaining/resetsAt) |
| `NO_INGREDIENTS` | 400 | INFO | No fridge ingredients to generate from |
| `AI_UPSTREAM_ERROR` | 502 | ERROR | LLM provider error; quota not consumed |
| `AI_TIMEOUT` | 504 | ERROR | LLM exceeded 60 s; quota not consumed |
| `AI_INVALID_RESPONSE` | 502 | ERROR | LLM output failed validation; not stored, quota not consumed |
| `RECIPE_NOT_FOUND` | 404 | INFO | Missing or not owned |
| `COOK_ITEM_INVALID` | 400 | INFO | Cook-deduct item not valid for this recipe/unit |
| `SAVED_RECIPE_NOT_FOUND` | 404 | INFO | Recipe is not in the user saved list |
| `MEAL_PLAN_LIMIT_EXCEEDED` | 429 | WARN | Monthly meal-plan limit reached |
| `MEAL_PLAN_NO_INGREDIENTS` | 422 | INFO | Ingredients cannot fill even one day (422) |
| `MEAL_PLAN_OVERLAP` | 409 | INFO | Period overlaps an existing plan and overwrite not set |
| `SLOT_ALREADY_EXISTS` | 409 | INFO | Meal slot already filled |
| `SLOT_NOT_FOUND` | 404 | INFO | Missing slot |
| `PAST_DATE_NOT_ALLOWED` | 400 | INFO | Cannot add a meal on a past date |
| `NOTIFICATION_NOT_FOUND` | 404 | INFO | Missing notification |
| `PUSH_SUBSCRIPTION_INVALID` | 400 | WARN | Bad web-push subscription |
| `CART_ITEM_NOT_FOUND` | 404 | INFO | Missing cart item |
| `RECIPE_NOT_SHAREABLE` | 400 | WARN | Only recipes generated by Remake by this user can be shared |
| `ALREADY_SHARED` | 409 | INFO | Recipe already shared |
| `PHOTO_REQUIRED` | 400 | INFO | Cooking photo (>= 1) required |
| `SHARED_RECIPE_NOT_FOUND` | 404 | INFO | Missing, hidden or deleted shared recipe |
| `REPORT_ALREADY_EXISTS` | 409 | INFO | User already reported this recipe |
| `EVENT_BATCH_TOO_LARGE` | 413 | INFO | More than 50 events in a batch |
| `USER_NOT_FOUND` | 404 | INFO | Admin: user missing |
| `REPORT_NOT_FOUND` | 404 | INFO | Admin: report missing |
| `FUNNEL_NOT_FOUND` | 404 | INFO | Admin: unknown funnel code |
| `ADMIN_REASON_REQUIRED` | 400 | INFO | Admin action needs a reason |
| `QUOTA_ADJUST_INVALID` | 400 | INFO | Invalid quota adjustment delta |
