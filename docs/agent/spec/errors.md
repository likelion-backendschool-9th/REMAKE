# Error contract

Source: error and exception definition v2 (Figma-based, Korean, kept in Notion; not in the repo). 59 codes. Codes are fixed UPPER_SNAKE_CASE; never rename. Clients branch on `code`; `message` is Korean user-facing text (the texts below are proposals; screens may change wording, codes do not change). API numbers refer to `api-summary.md`. Admin codes are on hold (no admin screens in Figma).

## Response body

Same shape as [`../backend-convention.md`](../backend-convention.md) §Responses and errors: `{ code, message, status, traceId, fieldErrors?, details? }`.

```json
{
  "code": "INSUFFICIENT_QUANTITY",
  "message": "가지고 있는 양보다 많아요",
  "status": 400,
  "traceId": "9f2c1e7a5b",
  "details": { "remaining": 3 }
}
```

| Field | Rule |
| --- | --- |
| `code` | UPPER_SNAKE_CASE. The frontend branches on it, so never change it once set |
| `message` | Korean sentence safe to show as is. No internal info, SQL, or stack traces |
| `status` | Same number as the HTTP status |
| `traceId` | Links to server logs. Keep it with inquiries and events |
| `fieldErrors` | Only for `INVALID_INPUT`. `[{ field, message }]`; `message` is the text under the input |
| `details` | Values a screen needs (optional). E.g. `remaining` (amount or count left), `remainingAttempts` (code attempts), `overlappingDates` |

## HTTP usage

| Status | Use |
| --- | --- |
| 400 | Input or rule violation (the user can fix it) |
| 401 | No or expired auth |
| 402 | Payment declined (`PAYMENT_FAILED`) |
| 403 | Re-authenticated with a different account (`REAUTH_ACCOUNT_MISMATCH`). Admin `FORBIDDEN` is on hold |
| 404 | Missing **or not owned** (never reveal that someone else's resource exists) |
| 409 | Conflict with the current state (already shared this week, meal-plan overlap, duplicate receipt, already subscribed) |
| 413, 415 | File size, type |
| 422 | Well-formed but cannot be processed (not a receipt, too few ingredients, no email) |
| 429 | Limits, too many requests, too many code attempts |
| 500, 502, 503, 504 | Server or upstream (AI, OCR, payment) problems, not the user's fault |

## Severity and logging

| Severity | Applies to | Server log |
| --- | --- | --- |
| INFO | Normal flow from user input or state (`INVALID_INPUT`, `*_NOT_FOUND`, 409) | INFO |
| WARN | Limits, auth, format (429, 401, 413, 415) | WARN |
| ERROR | Server, AI, OCR problems (500, 502, 503, 504) | ERROR + traceId |

## How screens show errors

- Input errors: red text under the input (font system "error mobile" 12px), color `#DC2626`. Many screens block first by disabling the button (p1.1, p1.3.1, p1.3.2, p2.2, p4.1.1.1, p4.1.2 sheets).
- Errors with their own screen (all in "pages to be added"): OCR failure (no. 4; not recognized, no food items, older than 6 months differ only in text), generation failure (no. 8; too few ingredients, AI failure, meal-plan shortage differ only in text), meal-plan overlap modal (no. 10), quota exhausted (no. 12, shared by recipes and meal plans).
- Everything else (server errors, network, files, camera unavailable, login failure, past-date changes) uses the common toast (no. 13).

## Codes

| No. | Area (owner) | Code | HTTP | APIs | When | Message (proposal) | Client handling | Quota/data |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | common (이준호) | `INVALID_INPUT` | 400 | all | Missing field, bad format or range; reasons in `fieldErrors`. E.g. household size outside 1-50 (4, 6), unknown allergen code (4, 8, 28), bad month/date or outside the last 6 months (22, 25, 26), bad filter/sort (33, 50), zero meals (38), no share title (51), zero items to register (21) | 입력한 내용을 확인해 주세요 | Mostly blocked by disabled buttons; otherwise red text under the input | Not saved |
| 2 | common (이준호) | `UNAUTHORIZED` | 401 | all under `user` | Access cookie missing or forged | 로그인이 필요해요 | Go to login (p1); keep the previous URL to return after login | - |
| 3 | common (이준호) | `TOKEN_EXPIRED` | 401 | all under `user` | Access token expired (30 min) | (not shown) | Call API 2 once and retry the original request once. If refresh fails, handle as `REFRESH_TOKEN_INVALID` | - |
| 4 | common (이준호) | `RATE_LIMITED` | 429 | all | Too many requests from one member or IP | 잠시 후 다시 시도해 주세요 | Wait `Retry-After`, retry. Common toast | - |
| 5 | common (이준호) | `INTERNAL_ERROR` | 500 | all | Unexpected server error | 문제가 생겼어요. 잠시 후 다시 시도해 주세요 | Retry guidance, keep traceId. Common toast | Save and charge both rolled back (one transaction) |
| 6 | common (이준호) | `SERVICE_UNAVAILABLE` | 503 | all | Maintenance, deploy, DB unreachable | 서비스 점검 중이에요 | Auto retry after 30 s. Common toast | - |
| 7 | common (이준호) | `NETWORK_OFFLINE` | (client) | all | Device offline or server unreachable (no HTTP response) | 인터넷 연결을 확인해 주세요 | Show the last successful result for reads, block writes. Common toast | - |
| 8 | common (이준호) | `CLIENT_TIMEOUT` | (client) | all | No response within the server time limit (10 s, OCR 30 s, AI recipe 60 s). The server cuts first with 504, so the frontend waits 5 s longer (proposal) | 응답이 늦어지고 있어요. 다시 시도해 주세요 | Cancel and suggest retry. Common toast | Generation/OCR cut by the server timeout is not saved or charged |
| 9 | common (이준호) | `NOT_FOUND` | 404 | - | Unknown URL. Missing resources use their own `*_NOT_FOUND` | (common) | Same as `INTERNAL_ERROR` | - |
| 10 | common (이준호) | `METHOD_NOT_ALLOWED` | 405 | - | Method not supported on this path | (common) | Common | - |
| 11 | common (이준호) | `NOT_ACCEPTABLE` | 406 | - | Requested response format (`Accept`) not supported | (common) | Common | - |
| 12 | common (이준호) | `UNSUPPORTED_MEDIA_TYPE` | 415 | - | Request body format (`Content-Type`) not supported. A wrong photo file is `UNSUPPORTED_FILE_TYPE` | (common) | Common | - |
| 13 | login/settings (이준호) | `OAUTH_FAILED` | 401 | 1, 10 | Social auth failed (cancelled, code expired, provider error), including Naver withdrawal re-auth | 로그인에 실패했어요. 다시 시도해 주세요 | Login: stay on p1. Withdrawal: back to settings (p1.3), nothing deleted. Common toast | - |
| 14 | login/settings (이준호) | `OAUTH_PROVIDER_UNSUPPORTED` | 400 | 1 | provider is not kakao/naver/google | 지원하지 않는 로그인 방식이에요 | Does not happen in the normal flow | - |
| 15 | login/settings (이준호) | `REFRESH_TOKEN_INVALID` | 401 | 2 | Refresh token expired (14 days), removed, or forged | 로그인 시간이 만료됐어요. 다시 로그인해 주세요 | Clear local cache, go to login (p1) | - |
| 16 | login/settings (이준호) | `REAUTH_ACCOUNT_MISMATCH` | 403 | 10 | Naver withdrawal re-auth with a different Naver account | 지금 로그인한 계정으로 다시 인증해 주세요 | Back to settings (p1.3), nothing deleted. Common toast | - |
| 17 | login/settings (이준호) | `ACCOUNT_EMAIL_MISSING` | 422 | 9 | Kakao/Google member has no email to send the code to | 인증번호를 보낼 이메일이 없어요 | Should not happen (Kakao requires email consent at login, Google always provides it). Show support guidance | Nothing sent |
| 18 | login/settings (이준호) | `DELETION_CODE_INVALID` | 400 | 10 | Wrong code, or no code was sent. `details.remainingAttempts` | 인증번호가 맞지 않아요 | Error toast on p1.3.3 (11/22; replaces red text under the boxes) | `account_deletion_requests.attempts` +1 |
| 19 | login/settings (이준호) | `DELETION_CODE_EXPIRED` | 400 | 10 | More than 5 minutes after sending | 인증번호 유효 시간이 지났어요. 다시 받아 주세요 | Get a new code with "resend". Error toast | - |
| 20 | login/settings (이준호) | `DELETION_CODE_LOCKED` | 429 | 10 | More than 5 wrong attempts on the same code | 입력 횟수를 넘었어요. 인증번호를 다시 받아 주세요 | "Resend" gives a new code (new row, attempts from 0). Error toast | That code can no longer be used |
| 21 | login/settings (이준호) | `ALREADY_SUBSCRIBED` | 409 | 58, 59 | Subscribing again while ACTIVE or CANCELED (scheduled) | 이미 구독 중이에요 | Rare (the subscribed screen has no subscribe button). Common toast, then reload subscription management | No charge |
| 22 | login/settings (이준호) | `SUBSCRIPTION_NOT_FOUND` | 404 | 60 | Cancel without a subscription | 구독 중인 멤버십이 없어요 | Does not happen in the normal flow. Common toast | - |
| 23 | login/settings (이준호) | `BILLING_AUTH_FAILED` | 400 | 59 | No billingKey from the returned `authKey`/`customerKey` (expired, forged, another member's key) | 카드 등록에 실패했어요. 다시 시도해 주세요 | Back to the p1.3.4 before-subscribe screen with the common toast | No subscription created |
| 24 | login/settings (이준호) | `PAYMENT_FAILED` | 402 | 59 | Toss declined the first payment (balance/limit, suspended card). `details.reason` = Toss reason (only user-safe text) | 결제에 실패했어요. 다른 카드로 다시 시도해 주세요 | Back to the before-subscribe screen with the common toast | No subscription; one FAILED `subscription_payments` row |
| 25 | login/settings (이준호) | `PAYMENT_UPSTREAM_ERROR` | 502 | 59 | Toss server error or no response (timeout included) | 결제 서비스에 문제가 있어요. 잠시 후 다시 시도해 주세요 | Common toast. Never auto-retry the payment (no double charge). If the result is unknown, the server checks the payment with Toss by order id before deciding | No subscription until the result is confirmed |
| 26 | fridge (김재연) | `INGREDIENT_NOT_FOUND` | 404 | 13, 16, 17, 18, 28 (custom `ingredientIds`) | Missing or not owned, including ingredients deleted at 0 | 재료를 찾을 수 없어요 | Back to the fridge list (p2) and reload | - |
| 27 | fridge (김재연) | `INSUFFICIENT_QUANTITY` | 400 | 18, 32 | Taking out more than owned (use, discard, cook). `details.remaining` | 가지고 있는 양보다 많아요 | Show the remaining amount. The steppers stop at the remaining amount (proposal) | No change |
| 28 | fridge (김재연) | `INGREDIENT_CATEGORY_NOT_FOUND` | 404 | 15, 16, 21 | `categoryCode` not one of the 12 (including "all") | 선택할 수 없는 카테고리예요 | Does not happen in the normal flow; reload API 19 | Not saved |
| 29 | receipts (이동준) | `RECEIPT_NOT_RECOGNIZED` | 422 | 20 | Not a receipt, or items unreadable | 영수증을 인식하지 못했어요. 다시 찍어 주세요 | OCR failure screen, retake | No limit (scans are unlimited) |
| 30 | receipts (이동준) | `OCR_UPSTREAM_ERROR` | 502 | 20 | OCR service error | 영수증을 분석하지 못했어요. 잠시 후 다시 시도해 주세요 | OCR failure screen | No limit |
| 31 | receipts (이동준) | `OCR_TIMEOUT` | 504 | 20 | No OCR response within 30 s | 분석이 오래 걸리고 있어요. 다시 시도해 주세요 | OCR failure screen | No limit |
| 32 | receipts (이동준) | `RECEIPT_NO_FOOD_ITEMS` | 422 | 20 | Readable receipt with no food items | 식재료가 없는 영수증이에요 | OCR failure screen, different text | No limit |
| 33 | receipts (이동준) | `RECEIPT_TOO_OLD` | 422 | 20, 21 | Purchase date older than 6 months (blocked at scan, checked again at registration) | 6개월이 지난 영수증은 등록할 수 없어요 | OCR failure screen, different text | Not saved |
| 34 | receipts (이동준) | `RECEIPT_DUPLICATE` | 409 | 20, 21 | Already registered (same store (branch name), receipt number, and purchase time; if the number is unreadable compare store, purchase date, and food names; if the time is unreadable compare by purchase date). Blocked at scan, checked again at registration | 이미 등록한 영수증이에요 | OCR failure screen, different text | Not saved |
| 35 | receipts (이동준) | `RECEIPT_NOT_FOUND` | 404 | 23, 24 | Missing or not owned, or removed by the 6-month cleanup | 영수증을 찾을 수 없어요 | Back to the receipt list (p3) and reload | - |
| 36 | receipts (이동준) | `FILE_TOO_LARGE` | 413 | 20, 21, 51 | Photo over 10 MB | 파일이 너무 커요 (최대 10MB) | Pick another photo. The frontend checks before upload. Common toast | No OCR call |
| 37 | receipts (이동준) | `UNSUPPORTED_FILE_TYPE` | 415 | 20, 21, 51 | Not jpg, png, heic | 지원하지 않는 파일 형식이에요 | Same as above | No OCR call |
| 38 | receipts (이동준) | `CAMERA_UNAVAILABLE` | (client) | - | Camera permission denied or camera unavailable on p3.1 (no server request) | 카메라를 쓸 수 없어요. 갤러리에서 골라 주세요 | Suggest the gallery. Common toast | No OCR call |
| 39 | AI recipes (김종수) | `GENERATION_LIMIT_EXCEEDED` | 429 | 28, 29, 42 | Daily recipe count used up (basic 2, premium 4, + rewards). Regenerate and meal-slot replace use the same count. `details.remaining` = 0 | 오늘의 레시피 생성 횟수를 모두 사용했어요 | Quota exhausted screen | Not charged |
| 40 | AI recipes (김종수) | `NO_INGREDIENTS` | 422 | 28 | Recipe supplement off and too few fridge (or chosen) ingredients, including an empty fridge | 냉장고 재료가 부족해서 레시피를 만들지 못했어요. 설정에서 레시피 재료 보충을 켜 보세요 | Generation failure screen | Not charged |
| 41 | AI recipes (김종수) | `RECIPE_NOT_FOUND` | 404 | 29, 30, 31, 32, 34-37, 51 | Missing or not visible (neither generated nor saved by me) | 레시피를 찾을 수 없어요 | Back to recipes (p4.1) and reload | - |
| 42 | AI recipes (김종수) | `COOK_ITEM_INVALID` | 400 | 32 | A deduction item is not my ingredient, or amount <= 0 | 차감할 재료를 확인해 주세요 | Re-check items on p4.1.1.2 | No change |
| 43 | AI recipes (김종수) | `SAVED_RECIPE_NOT_FOUND` | 404 | 35, 36, 37 | Unsave or favorite on a recipe that is not saved | 저장된 레시피가 아니에요 | Reload the saved list | - |
| 44 | AI recipes (김종수) | `AI_UPSTREAM_ERROR` | 502 | 28, 29, 42 (38: failure notification "식단표를 만들지 못했어요. 잠시 후 다시 만들어 주세요. 생성 횟수는 차감되지 않았어요") | AI service error or refusal | 레시피를 만들지 못했어요. 횟수는 차감되지 않았어요 | Keep the input and retry. Generation failure screen, different text | Not charged; nothing in `usage_logs` |
| 45 | AI recipes (김종수) | `AI_TIMEOUT` | 504 | 28, 29, 42 (38: failure notification "식단표를 만들지 못했어요. 잠시 후 다시 만들어 주세요. 생성 횟수는 차감되지 않았어요") | No AI response within 60 s (recipes). Meal plan: the server job's limit is (open) | 생성이 오래 걸리고 있어요. 횟수는 차감되지 않았어요 | Same as above | Not charged |
| 46 | AI recipes (김종수) | `AI_INVALID_RESPONSE` | 502 | 28, 29, 42 (38: failure notification "식단표를 만들지 못했어요. 잠시 후 다시 만들어 주세요. 생성 횟수는 차감되지 않았어요") | AI output failed validation (missing fields, unusable unit) | 레시피를 만들지 못했어요. 횟수는 차감되지 않았어요 | Same as above | Not charged |
| 47 | meal plan (한윤수) | `MEAL_PLAN_LIMIT_EXCEEDED` | 429 | 38 | Monthly meal-plan count used up (basic 1, premium 4) | 이번 달 식단표 생성 횟수를 모두 사용했어요 | Quota exhausted screen | Not charged |
| 48 | meal plan (한윤수) | `MEAL_PLAN_NO_INGREDIENTS` | - (not a response) | 38 (failure notification) | Ingredients cannot fill even one day, found by the server job after 202 | 냉장고 재료가 부족해 식단표를 만들지 못했어요. 생성 횟수는 차감되지 않았어요 | In-app failure notification (`MEAL_PLAN_FAILED`), no screen | Plan `FAILED`, not charged |
| 49 | meal plan (한윤수) | `MEAL_PLAN_OVERLAP` | 409 | 38 | Without `overwrite`, the period overlaps an existing plan. `details.overlappingDates` | 이미 식단이 있는 날짜가 있어요. 덮어쓸까요? | Warning modal; on "overwrite" resend with `overwrite: true` | Not saved or charged |
| 50 | meal plan (한윤수) | `PAST_DATE_NOT_ALLOWED` | 400 | 38, 41, 42 | Start date before today, or deleting/replacing a past slot | 지난 날짜는 바꿀 수 없어요 | The start-date sheet blocks past dates. Past meals keep the ⋮ menu; delete/replace shows this text. Common toast | No change |
| 51 | meal plan (한윤수) | `SLOT_NOT_FOUND` | 404 | 41, 42 | Missing or not owned slot (including slots removed by overwrite) | 식단을 찾을 수 없어요 | Reload the calendar (p5) | - |
| 52 | sharing/cart (최유리) | `CART_ITEM_NOT_FOUND` | 404 | 46, 47, 48 | Missing or not owned cart item. If any of several ids is missing, process none | 이미 삭제된 항목이에요 | Reload the cart (p7) | No change |
| 53 | sharing/cart (최유리) | `RECIPE_NOT_SHAREABLE` | 400 | 51 | Not my generated recipe (including others' recipes saved from the shared space) | 공유할 수 없는 레시피예요 | Does not happen in the normal flow | - |
| 54 | sharing/cart (최유리) | `ALREADY_SHARED` | 409 | 51 | Same recipe already shared this week (week starts Monday) | 이번 주에 이미 공유한 레시피예요 | "Share" is disabled first; if it happens close the share screen | - |
| 55 | sharing/cart (최유리) | `PHOTO_REQUIRED` | 400 | 51 | Share without a cooking photo | 실제로 요리한 사진을 올려 주세요 | Rare (button enabled only with title and photo) | - |
| 56 | sharing/cart (최유리) | `SHARED_RECIPE_NOT_FOUND` | 404 | 52, 53, 54, 55 | Missing or hidden (HIDDEN) shared recipe | 삭제되었거나 볼 수 없는 레시피예요 | Back to the weekly popular list (p4.1.4) | - |
| 57 | sharing/cart (최유리) | `REPORT_ALREADY_EXISTS` | 409 | 55 | Already reported (UK reporter_id + shared_recipe_id) | 이미 신고한 레시피예요 | Common toast | Not saved |
| 58 | events (이준호) | `EVENT_BATCH_TOO_LARGE` | 413 | 56 | More than 50 events in one batch | (not shown) | Split and resend. Never affects the screen | - |
| 59 | meal plan (한윤수) | `MEAL_PLAN_IN_PROGRESS` | 409 | 38 | A new generate request while one of my plans is `GENERATING` (11/22; numbered last so other codes keep their numbers) | 식단표를 만들고 있어요. 완성되면 알림을 보내드려요 (proposal) | Error toast | Not saved or charged |

## Codes per API

Every `user` API can also return the common codes (`UNAUTHORIZED`, `TOKEN_EXPIRED`, `RATE_LIMITED`, `INTERNAL_ERROR`, `SERVICE_UNAVAILABLE`); they are not repeated.

| No. | API | Codes |
| --- | --- | --- |
| 1 | `POST /public/auth/oauth/{provider}` | INVALID_INPUT, OAUTH_FAILED, OAUTH_PROVIDER_UNSUPPORTED |
| 2 | `POST /public/auth/refresh` | REFRESH_TOKEN_INVALID |
| 3 | `POST /user/auth/logout` | common only |
| 4 | `PUT /user/onboarding` | INVALID_INPUT |
| 5 | `GET /user/profile` | common only |
| 6 | `PATCH /user/profile` | INVALID_INPUT |
| 7 | `GET /user/excluded-ingredients` | common only |
| 8 | `PUT /user/excluded-ingredients` | INVALID_INPUT |
| 9 | `POST /user/account/deletion-code` | ACCOUNT_EMAIL_MISSING |
| 10 | `POST /user/account/deletion` | INVALID_INPUT, OAUTH_FAILED, REAUTH_ACCOUNT_MISMATCH, DELETION_CODE_INVALID, DELETION_CODE_EXPIRED, DELETION_CODE_LOCKED |
| 11 | `GET /user/ingredients/summary` | common only |
| 12 | `GET /user/ingredients` | INVALID_INPUT |
| 13 | `GET /user/ingredients/{id}` | INGREDIENT_NOT_FOUND |
| 14 | `GET /user/ingredient-units` | INVALID_INPUT |
| 15 | `POST /user/ingredients` | INVALID_INPUT, INGREDIENT_CATEGORY_NOT_FOUND |
| 16 | `PATCH /user/ingredients/{id}` | INVALID_INPUT, INGREDIENT_NOT_FOUND, INGREDIENT_CATEGORY_NOT_FOUND |
| 17 | `DELETE /user/ingredients/{id}` | INGREDIENT_NOT_FOUND |
| 18 | `POST /user/ingredients/{id}/consume` | INVALID_INPUT, INGREDIENT_NOT_FOUND, INSUFFICIENT_QUANTITY |
| 19 | `GET /user/ingredient-categories` | common only |
| 20 | `POST /user/receipts/scan` | INVALID_INPUT, RECEIPT_NOT_RECOGNIZED, OCR_UPSTREAM_ERROR, OCR_TIMEOUT, RECEIPT_NO_FOOD_ITEMS, RECEIPT_TOO_OLD, RECEIPT_DUPLICATE, FILE_TOO_LARGE, UNSUPPORTED_FILE_TYPE |
| 21 | `POST /user/receipts` | INVALID_INPUT, INGREDIENT_CATEGORY_NOT_FOUND, RECEIPT_TOO_OLD, RECEIPT_DUPLICATE, FILE_TOO_LARGE, UNSUPPORTED_FILE_TYPE |
| 22 | `GET /user/receipts` | INVALID_INPUT |
| 23 | `GET /user/receipts/{id}` | RECEIPT_NOT_FOUND |
| 24 | `DELETE /user/receipts/{id}` | RECEIPT_NOT_FOUND |
| 25 | `GET /user/expenses/patterns` | INVALID_INPUT |
| 26 | `GET /user/expenses/daily-items` | INVALID_INPUT |
| 27 | `GET /user/usage-quotas` | common only |
| 28 | `POST /user/recipes/generate` | INVALID_INPUT, INGREDIENT_NOT_FOUND, GENERATION_LIMIT_EXCEEDED, NO_INGREDIENTS, AI_UPSTREAM_ERROR, AI_TIMEOUT, AI_INVALID_RESPONSE |
| 29 | `POST /user/recipes/{id}/regenerate` | GENERATION_LIMIT_EXCEEDED, RECIPE_NOT_FOUND, AI_UPSTREAM_ERROR, AI_TIMEOUT, AI_INVALID_RESPONSE |
| 30 | `GET /user/recipes/{id}` | RECIPE_NOT_FOUND |
| 31 | `GET /user/recipes/{id}/cook-preview` | RECIPE_NOT_FOUND |
| 32 | `POST /user/recipes/{id}/cook` | INVALID_INPUT, INSUFFICIENT_QUANTITY, RECIPE_NOT_FOUND, COOK_ITEM_INVALID |
| 33 | `GET /user/saved-recipes` | INVALID_INPUT |
| 34 | `PUT /user/saved-recipes/{recipeId}` | RECIPE_NOT_FOUND |
| 35 | `DELETE /user/saved-recipes/{recipeId}` | RECIPE_NOT_FOUND, SAVED_RECIPE_NOT_FOUND |
| 36 | `PUT /user/saved-recipes/{recipeId}/favorite` | RECIPE_NOT_FOUND, SAVED_RECIPE_NOT_FOUND |
| 37 | `DELETE /user/saved-recipes/{recipeId}/favorite` | RECIPE_NOT_FOUND, SAVED_RECIPE_NOT_FOUND |
| 38 | `POST /user/meal-plans/generate` | INVALID_INPUT, MEAL_PLAN_LIMIT_EXCEEDED, MEAL_PLAN_OVERLAP, MEAL_PLAN_IN_PROGRESS, PAST_DATE_NOT_ALLOWED. Failure notification after 202 (not a response): MEAL_PLAN_NO_INGREDIENTS, AI_UPSTREAM_ERROR, AI_TIMEOUT, AI_INVALID_RESPONSE |
| 39 | `GET /user/meal-plans/calendar` | INVALID_INPUT |
| 40 | `GET /user/meal-plans/days/{date}` | INVALID_INPUT |
| 41 | `DELETE /user/meal-plan-slots/{slotId}` | PAST_DATE_NOT_ALLOWED, SLOT_NOT_FOUND |
| 42 | `POST /user/meal-plan-slots/{slotId}/replace` | GENERATION_LIMIT_EXCEEDED, AI_UPSTREAM_ERROR, AI_TIMEOUT, AI_INVALID_RESPONSE, PAST_DATE_NOT_ALLOWED, SLOT_NOT_FOUND |
| 43 | `GET /user/notifications/summary` | common only |
| 44 | `GET /user/notifications` | INVALID_INPUT |
| 45 | `GET /user/cart-items` | common only |
| 46 | `PATCH /user/cart-items/{id}` | INVALID_INPUT, CART_ITEM_NOT_FOUND |
| 47 | `POST /user/cart-items/move-to-fridge` | INVALID_INPUT, CART_ITEM_NOT_FOUND |
| 48 | `POST /user/cart-items/delete` | INVALID_INPUT, CART_ITEM_NOT_FOUND |
| 49 | `GET /user/shared-recipes/weekly-top` | common only |
| 50 | `GET /user/shared-recipes/ranking` | INVALID_INPUT |
| 51 | `POST /user/shared-recipes` | INVALID_INPUT, FILE_TOO_LARGE, UNSUPPORTED_FILE_TYPE, RECIPE_NOT_FOUND, RECIPE_NOT_SHAREABLE, ALREADY_SHARED, PHOTO_REQUIRED |
| 52 | `GET /user/shared-recipes/{id}` | SHARED_RECIPE_NOT_FOUND |
| 53 | `PUT /user/shared-recipes/{id}/save` | SHARED_RECIPE_NOT_FOUND |
| 54 | `DELETE /user/shared-recipes/{id}/save` | SHARED_RECIPE_NOT_FOUND |
| 55 | `POST /user/shared-recipes/{id}/reports` | INVALID_INPUT, SHARED_RECIPE_NOT_FOUND, REPORT_ALREADY_EXISTS |
| 56 | `POST /user/events` | INVALID_INPUT, EVENT_BATCH_TOO_LARGE |
| 57 | `GET /user/subscription` | common only |
| 58 | `POST /user/subscription/billing-auth` | ALREADY_SUBSCRIBED |
| 59 | `POST /user/subscription` | INVALID_INPUT, ALREADY_SUBSCRIBED, BILLING_AUTH_FAILED, PAYMENT_FAILED, PAYMENT_UPSTREAM_ERROR |
| 60 | `POST /user/subscription/cancel` | SUBSCRIPTION_NOT_FOUND |

## Quota charging and failures

| Feature | Limit | Charged when | On failure |
| --- | --- | --- | --- |
| Recipe generation (quick, custom) | Per day basic 2, premium 4, + rewards | Right after the validated result is saved | AI error, timeout, validation failure, `NO_INGREDIENTS`: not charged |
| Regenerate | Same count as generation | Same | Same |
| Meal-slot replace | Same count as generation | Same | Same. Past slots -> `PAST_DATE_NOT_ALLOWED` |
| Meal-plan generation | This month basic 1, premium 4 | Right after the server job saves the plan | Too few ingredients (`MEAL_PLAN_NO_INGREDIENTS`), AI error, timeout: failure notification, not charged. Overlap (409) and quota used up (429) are rejected before generation: not charged |
| Receipt scan | No limit | Never | Retake after OCR failure or non-receipt |

- The charge (one `usage_logs` row) and the result are saved in one transaction; if saving fails, the charge rolls back. `usage_logs` holds successes only.
- Reward grants (`generation_quota_grants`) are used after the basic count runs out.
- Two concurrent generation requests from one member could exceed the limit, so lock per member while checking and charging (proposal).

## Empty results are not errors

No data returns 200 with empty values; the screen draws its empty state.

| API | Case | Response |
| --- | --- | --- |
| 12 `GET /user/ingredients` | 0 ingredients | `items: []` |
| 22 `GET /user/receipts` | 0 receipts that month | `totalAmount` 0, `weeks: []` |
| 25 `GET /user/expenses/patterns` | No spending that month | `totalAmount` 0, `categories: []`, `dailyTotals: []` |
| 26 `GET /user/expenses/daily-items` | No spending that day | `totalAmount` 0, `items: []` |
| 33 `GET /user/saved-recipes` | 0 saved or 0 after filter | `content: []` |
| 49 `GET /user/shared-recipes/weekly-top` | Fewer than 3 saved shared recipes this week | 0-2 items |
| 50 `GET /user/shared-recipes/ranking` | 0 search results | `content: []` |
| 50 `GET /user/shared-recipes/ranking` | No last-week rank (first week, new recipe) | `rankChange: null` ("—") |
| 31 `GET /user/recipes/{id}/cook-preview` | None of the recipe's ingredients are in my fridge | `items: []` |
| 39 `GET /user/meal-plans/calendar` | No plans | `periods: []`, `datesWithMeals: []` |
| 40 `GET /user/meal-plans/days/{date}` | No meals that day | `slots: []` |
| 44 `GET /user/notifications` | 0 notifications | `content: []` |
| 45 `GET /user/cart-items` | Empty cart | `items: []` |

## Auth flow

1. On `TOKEN_EXPIRED` the frontend calls API 2 once and resends the original request.
2. If several requests expire together, refresh once and let the others wait for it.
3. On `REFRESH_TOKEN_INVALID` clear the local cache and go to login; return to the original screen after login.
4. Account deletion: Kakao/Google send a code (9) then submit it (10, `DELETION_CODE_*` shown as a toast); Naver re-authenticates (10, `OAUTH_FAILED`, `REAUTH_ACCOUNT_MISMATCH`). On success clear cookies and go to login.

## External services (AI, OCR) and files

| Case | Server | Frontend |
| --- | --- | --- |
| Error or refusal | AI: one automatic retry, then fail. OCR: no retry. Not charged | Keep the input and retry |
| Timeout | Cut at AI recipe 60 s / OCR 30 s and return 504. Meal plan: the server job ends with a failure notification (limit (open)) | Same for recipes and OCR. Meal plan: nothing on screen; the notification tells the user |
| Validation failure | AI output with missing fields or unusable units: ask once more, then 502 | Same |
| Repeated failures | Watch the failure rate ("20% in 10 minutes" is an earlier draft). Dashboard display waits for admin screens | After 3 failures in a row show "잠시 후 다시 시도해 주세요" (do not block the feature) |

- The server checks receipt photos (20, 21) and share photos (51) for size (10 MB) and type (jpg, png, heic) first and returns 413/415 without calling OCR.
- The frontend runs the same checks before upload (the server check is final).
- No share photo -> `PHOTO_REQUIRED` (400).

## Concurrency and duplicate requests

| Case | Handling |
| --- | --- |
| Receipt registration pressed twice | Disable the button until the request ends (frontend). No server duplicate-request key |
| Cook deduction pressed twice | Same. The server checks the remaining amount before subtracting; `INSUFFICIENT_QUANTITY` if short |
| Same ingredient edited on two devices | Last write wins (no `version`). Deduction as above |
| Same recipe saved twice | PUT, same result (not an error). `saved_recipes` UK(user_id + recipe_id) |
| Same recipe shared twice in a week | `ALREADY_SHARED` |
| Same post reported twice | `REPORT_ALREADY_EXISTS` |
| Weekly reward job runs twice | `generation_quota_grants` UK(week_start + rank_no), no double grant |
| Today's meal cooked during overwrite | Cooked meals (`cooked_at` set) are not overwritten |

Errors are also recorded as events (see `events.md`).

## Codes on hold and removed

On hold (admin screens are not in Figma): `FORBIDDEN`, `ACCOUNT_SUSPENDED`, `USER_NOT_FOUND`, `REPORT_NOT_FOUND`, `FUNNEL_NOT_FOUND`, `ADMIN_REASON_REQUIRED`, `QUOTA_ADJUST_INVALID`. `FORBIDDEN` and `ACCOUNT_SUSPENDED` already exist in `global/error/ErrorCode`; keep them in code but do not return them from new APIs (others' resources return 404).

Removed (do not use in new code; remove from `ErrorCode` when the related code is changed):

| Code | Why |
| --- | --- |
| `CONCURRENT_UPDATE` | `ingredients.version` removed. Deduction checks the remaining amount (`INSUFFICIENT_QUANTITY`) |
| `CATALOG_ITEM_NOT_FOUND` | No dictionary id in requests. Unknown allergen codes are `INVALID_INPUT` |
| `INVALID_UNIT` | Clients do not send units; the server picks them |
| `INGREDIENT_ALREADY_FINISHED` | Rows are deleted at 0 -> `INGREDIENT_NOT_FOUND` |
| `INGREDIENT_HAS_LOGS` | Deleting is allowed with logs (logs keep the name) |
| `SLOT_ALREADY_EXISTS` | No manual meal add |
| `NOTIFICATION_NOT_FOUND` | No API takes a notification id |
| `PUSH_SUBSCRIPTION_INVALID` | No device push |

Withdrawal codes from the previous version (`OAUTH_STATE_INVALID`, `ACCOUNT_PROVIDER_MISMATCH`, `DELETION_CODE_RESEND_TOO_SOON`) are not in v2: Naver re-auth mismatch is `REAUTH_ACCOUNT_MISMATCH`, and there is no resend interval code.
