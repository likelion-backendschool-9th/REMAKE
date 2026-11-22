# 00. 공통 개발 가이드

## 1. 서비스 한 줄과 범위

Remake는 영수증 OCR로 냉장고 재료를 등록하고, 냉장고 재료로 AI 레시피와 월 식단표를 만들고, 부족한 재료는 장바구니에 담아 주는 모바일 우선 PWA입니다. 포트폴리오용이라 쇼핑몰 연동·외부 이커머스는 하지 않고, 결제는 프리미엄 구독(토스페이먼츠 카드 자동결제)만 있습니다. 이 프로젝트의 차별점은 **사용자 행동 이벤트를 쌓고 지표로 모니터링**하는 것이라 모든 담당자가 이벤트 발행에 참여합니다([08 문서](08_event-tracking.md)).

어드민 화면은 아직 피그마에 없어서 어드민 기능은 보류입니다. 피그마가 정답이고, 피그마에 없는 기능은 만들기 전에 준호님께 물어봅니다.

## 2. 기술 스택

| 구분 | 내용 |
|---|---|
| 프론트 | Next.js(App Router)+TypeScript, CSS Modules+CSS 변수(Tailwind 금지), TanStack Query, Zustand, React Hook Form+Zod, Serwist(PWA), MSW(API 목업), openapi-typescript+openapi-fetch, 자체 서버(nginx) 배포. 자세한 건 [`../frontend-blueprint.md`](../frontend-blueprint.md) |
| 백엔드 | Java + Spring Boot, JPA, PostgreSQL, Flyway, Spring Security(JWT), `@Scheduled`, springdoc-openapi |
| 외부 | 소셜 로그인(카카오·네이버·구글), OCR, LLM(AI 레시피·식단표), 토스페이먼츠(구독 결제), 메일(탈퇴 인증번호) |
| 앱 배포 | PWA를 TWA(PWABuilder)로 감싸 구글 플레이 배포. React Native·GraphQL은 쓰지 않음. 기기 푸시 알림 없음(앱 안 알림만) |
| 디자인 | 모바일 360px 기준, 상단 탭(냉장고·영수증·레시피·식단표·장바구니), 설정은 톱니, 알림은 상단 띠. Pretendard, 4pt 그리드, 오류 색 #DC2626 |

## 3. 폴더 구조 (제안)

```
remake/
  frontend/   (Next.js)
  backend/    (Spring Boot)
  docs/
```

**백엔드**는 기능 영역을 패키지로 씁니다. 테이블은 [`../erd.md`](../erd.md)의 26개입니다.

| 패키지 | 테이블 | 담당 |
|---|---|---|
| `global` | 공통 예외·보안·BaseEntity | 김종수 |
| `auth`, `user`, `subscription` | users, user_excluded_ingredients, refresh_tokens, account_deletion_requests, subscriptions, subscription_payments | 이준호 |
| `ingredient` | ingredient_categories, ingredient_catalog, ingredients, ingredient_logs | 김재연 |
| `receipt` | receipts, receipt_items | 이동준 |
| `recipe`, `quota` | recipes, recipe_ingredients, recipe_steps, saved_recipes, usage_logs | 김종수 |
| `mealplan` | meal_plans, meal_plan_slots | 한윤수 |
| `notification` | notifications | 이상호 |
| `cart`, `share` | cart_items, shared_recipes, shared_recipe_reports, shared_recipe_weekly_ranks, generation_quota_grants | 최유리 |
| `event` | events | 이준호 |

패키지 안은 헥사고날 구조(`domain`, `application/port`, `application/service`, `adapter`)입니다. 다른 기능의 데이터는 그 기능의 UseCase로만 가져옵니다. 자세한 건 [`../backend-convention.md`](../backend-convention.md).

**프론트**는 라우트 폴더에 그 라우트만 쓰는 코드를 모으고(`_components/`, `_hooks/`, `_lib/`), 여러 라우트가 쓰는 코드만 `components/`, `hooks/`, `lib/`로 올립니다. 주소는 `/{역할자}/{기능}/{자원}`입니다. 자세한 건 [`../frontend-blueprint.md`](../frontend-blueprint.md) §3.

## 4. API 공통 규칙

API 60개 전체는 영문 요약 [`docs/agent/spec/api-summary.md`](../../agent/spec/api-summary.md)에 번호와 함께 있고, 각 영역 문서에 그 영역 표가 있습니다.

- 경로: `/api/v1/{public|user}/{자원}[/{id}[/{동작}]]`. 자원은 복수형 케밥케이스(`saved-recipes`), 동작은 뒤에 동사(`generate`, `cook`, `scan`). `me`는 쓰지 않습니다(`/user/profile`).
- `public`은 로그인·토큰 재발급만, `user`는 로그인 사용자(토큰 주인의 데이터만). `admin`은 ADMIN role만(서버에서 검증, 아니면 403)인데 이번 버전에는 어드민 API가 없습니다.
- 성공 응답은 래퍼 없이 리소스 그대로. 생성 201, 본문 없음 204, 이벤트 수집·식단표 생성 접수(38) 202.
- 날짜 `2026-11-19`, 시각 ISO 8601 `+09:00`, 금액은 원 단위 정수, 페이징 `?page=0&size=20` → `{ content, page, size, totalElements, hasNext }`. 냉장고·장바구니·월 목록은 페이징하지 않습니다.
- 남의 리소스는 403이 아니라 404(존재 여부를 알리지 않기 위해).
- 중복 요청 키(`Idempotency-Key`)는 없습니다. 등록·차감 같은 버튼은 요청이 끝날 때까지 비활성으로 막습니다.
- 시간 제한: 일반 10초, OCR 30초, AI 레시피 60초(레시피에만 적용, 식단표 생성은 202로 바로 응답하고 서버 작업으로 만듦). AI는 1회 자동 재시도, OCR은 재시도 없음.
- 모든 요청에 `X-Client-Platform`(TWA/PWA/WEB), `X-App-Version`, `X-Screen`(지금 화면 코드) 헤더. 서버는 이 값을 이벤트의 `platform`, `app_version`, `screen`에 씁니다.
- 인증([#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42) A): 액세스·리프레시 JWT를 모두 HttpOnly 쿠키로 둡니다. 운영은 같은 도메인 상대 경로(`/api/...`) + `credentials: 'include'`. 프론트는 토큰을 JS에 두지 않고 `Authorization: Bearer`도 붙이지 않습니다. 자세한 흐름은 [`01_auth-onboarding.md`](01_auth-onboarding.md)·[`../frontend-blueprint.md`](../frontend-blueprint.md) §9.

## 5. 에러 공통 규칙

응답 형식은 항상 `{ code, message, status, traceId, fieldErrors?, details? }`입니다. 프론트는 `code`로 분기하고 `message`는 사용자에게 보여줄 한국어 문장입니다. 코드 58개와 API별 코드는 [`docs/agent/spec/errors.md`](../../agent/spec/errors.md)에 있습니다.

백엔드: `BusinessException(ErrorCode)` 하나를 던지고 `@RestControllerAdvice`가 변환합니다. 스택 트레이스·SQL·외부 서비스 에러 문구는 응답에 넣지 않습니다. 요청마다 `traceId`를 만들어 로그(MDC)에 같이 남깁니다. 실패와 롤백은 횟수를 차감하지 않습니다. 데이터가 없는 조회는 에러가 아니라 200과 빈 값입니다.

프론트: 에러 코드별 화면 처리를 `lib/api`의 매핑표 한 곳에서 정합니다. 입력 오류는 입력칸 아래 빨간 글자, 따로 화면이 없는 오류는 공통 토스트, 화면이 있는 오류는 OCR 실패·생성 실패 화면, 식단표 겹침 모달, 횟수 소진 화면입니다. 모르는 코드는 `INTERNAL_ERROR`로 처리하고 `traceId`는 문의용으로만 보관합니다.

## 6. 정책에서 온 공통 제약

자세한 건 영문 요약 [`docs/agent/spec/domain-rules.md`](../../agent/spec/domain-rules.md)와 각 영역 문서의 "정책" 표입니다.

- 소셜 로그인만(카카오·네이버·구글). 로그인 없이는 어떤 화면도 못 씁니다. 로그인 후 첫 화면은 냉장고입니다.
- 소비기한 상태: 위험(지났거나 D-DAY), 임박(D-1~D-3), 여유. 서버가 `status`, `dDay`를 내려주고 프론트는 다시 계산하지 않습니다.
- AI에 **자유 입력 금지**. 레시피·식단표 생성은 고른 옵션(id, enum)만 받고, 사용자가 쓴 글을 프롬프트에 붙이지 않습니다.
- 횟수: 레시피 하루 2회(구독 4회), 식단표 월 1회·5일치(구독 월 4회·7일치). 빠른·커스텀 생성, 다시 받기, 식단 레시피 교체가 하루 레시피 횟수를 같이 씁니다. **실패하면 차감하지 않습니다.** 영수증 스캔은 횟수 제한이 없습니다.
- 단위(14개)·카테고리(12개)는 코드 목록만 씁니다. 단위는 사용자가 고르지 않고 서버가 재료명으로 정합니다(재료 사전 `ingredient_catalog`).
- 공유 공간은 완전 익명: 어떤 응답에도 작성자 정보를 넣지 않습니다.
- 알림은 앱 안에서만(기기 푸시 없음).
- 이벤트·로그·에러에 개인정보와 비밀키를 넣지 않습니다.

## 7. 협업 규칙

- 기능 영역별 브랜치, 작은 PR. 다른 영역 패키지를 고칠 일이 있으면 담당자에게 먼저 알립니다.
- 비즈니스 규칙에는 테스트를 씁니다(횟수 계산, 단위 환산, 소비기한 상태, 식단표 재료 배정, 중복 영수증, 랭킹·보상).
- API 경로·에러 코드·이벤트 이름은 바꾸면 프론트·에러표·분석이 같이 깨지므로 정의서를 먼저 고치고 알립니다.
- 정책 표에 "제안"으로 표시된 항목은 상수나 설정으로 분리해 구현하고 PR에 적어 둡니다. "미결"은 구현 전에 준호님께 물어봅니다.
- 새 이벤트는 이벤트 정의서(노션)에 먼저 추가합니다.
