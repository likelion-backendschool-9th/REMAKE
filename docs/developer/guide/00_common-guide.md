# 00. 공통 개발 가이드

## 1. 서비스 한 줄과 범위

Remake는 영수증 OCR로 냉장고 재료를 등록하고, 냉장고 재료로 AI 레시피와 월 식단표를 만들고, 부족한 재료는 장바구니에 담아 주는 모바일 우선 PWA입니다. 포트폴리오용이라 결제·외부 쇼핑몰 연동·수익화는 하지 않습니다. 이 프로젝트의 차별점은 **사용자 행동 이벤트를 쌓고 어드민에서 모니터링**하는 것이라 모든 담당자가 이벤트 발행에 참여합니다([08 문서](08_event-tracking.md)).

## 2. 기술 스택

| 구분 | 내용 |
|---|---|
| 프론트 | Next.js(App Router)+TypeScript, CSS Modules+CSS 변수(Tailwind 금지), TanStack Query, Zustand, React Hook Form+Zod, Serwist(PWA), MSW(API 목업), openapi-typescript+openapi-fetch, 자체 서버(nginx) 배포. 자세한 건 [`../frontend-blueprint.md`](../frontend-blueprint.md) |
| 백엔드 | Java + Spring Boot, JPA, PostgreSQL, Spring Security(JWT), `@Scheduled`, springdoc-openapi |
| 외부 | 소셜 로그인(카카오·네이버·구글), OCR, LLM(AI 레시피·식단표), 웹 푸시 |
| 앱 배포 | PWA를 TWA(PWABuilder)로 감싸 구글 플레이 배포. React Native·GraphQL은 쓰지 않음 |
| 디자인 | 모바일 360px 기준, 상단 탭, Pretendard, 4pt 그리드, 파란 버튼·회색 카드 |

## 3. 폴더 구조 (제안)

```
remake/
  frontend/   (Next.js)
  backend/    (Spring Boot)
  docs/
```

**백엔드**는 ERD의 "기능 폴더"를 그대로 패키지로 씁니다.

| 패키지 | 테이블 | 담당 |
|---|---|---|
| `global` | idempotency_keys, 공통 예외·보안·BaseEntity | 김종수 |
| `auth`, `user` | refresh_tokens, users, user_excluded_ingredients, account_deletion_requests | 이준호 |
| `ingredient` | ingredient_categories, ingredient_catalog, ingredients, ingredient_logs | 김재연 |
| `receipt` | receipts, receipt_items | 이동준 |
| `recipe`, `quota` | recipes 외 5개, usage_logs | 김종수 |
| `mealplan` | meal_plans, meal_plan_slots, meal_plan_slot_ingredients | 한윤수 |
| `notification` | notifications, push_subscriptions | 이상호 |
| `admin` | admin_audit_logs | 이상호 |
| `cart`, `share`, `quota`(지급) | cart_items, shared_recipe*, generation_quota_grants | 최유리 |
| `event` | events, event_daily_stats | 이준호 |

패키지 안은 헥사고날 구조(`domain`, `application/port`, `application/service`, `adapter`)입니다. 다른 기능의 데이터는 그 기능의 UseCase로만 가져옵니다. 자세한 건 [`../backend-convention.md`](../backend-convention.md).

**프론트**는 라우트 폴더에 그 라우트만 쓰는 코드를 모으고(`_components/`, `_hooks/`, `_lib/`), 여러 라우트가 쓰는 코드만 `components/`, `hooks/`, `lib/`로 올립니다. 자세한 건 [`../frontend-blueprint.md`](../frontend-blueprint.md) §3.

## 4. API 공통 규칙

- 경로: `/api/v1/{public|user|admin}/{자원}[/{id}[/{동작}]]`. 자원은 복수형 케밥케이스(`saved-recipes`), 동작은 뒤에 동사(`generate`, `cook`, `scan`). `me`는 쓰지 않습니다(`/user/profile`).
- `public`은 로그인·토큰 재발급만, `user`는 로그인 사용자(토큰 주인의 데이터만), `admin`은 ADMIN role만(서버에서 검증, 아니면 403).
- 성공 응답은 래퍼 없이 리소스 그대로. 생성 201, 삭제 204, 이벤트 수집 202.
- 날짜 `2026-11-19`, 시각 ISO 8601 `+09:00`, 금액은 원 단위 정수, 페이징 `?page=0&size=20` → `{ content, page, size, totalElements, hasNext }`.
- 남의 리소스는 403이 아니라 404(존재 여부를 알리지 않기 위해).
- 생성·차감·확정 POST는 `Idempotency-Key` 헤더(프론트가 행동마다 UUID 생성, 재시도 때 같은 값). 서버는 `idempotency_keys`에 24시간 보관하고 같은 키에는 처음 응답을 돌려줍니다.
- 시간 제한: 일반 10초, OCR 30초, AI 생성 60초.
- 모든 요청에 `X-Client-Platform`(PWA/TWA/WEB), `X-App-Version` 헤더.
- 인증: 운영은 프론트와 백엔드가 같은 도메인이라 프론트가 상대 경로(`/api/...`)로 호출하고 HttpOnly 쿠키를 씁니다. 토큰 수명과 갱신 흐름은 [`../frontend-blueprint.md`](../frontend-blueprint.md) §9 "남은 결정 사항"에서 정합니다.

## 5. 에러 공통 규칙

응답 형식은 항상 `{ code, message, status, traceId, fieldErrors?, details? }`입니다. 프론트는 `code`로 분기하고 `message`는 사용자에게 보여줄 한국어 문장입니다. 코드 목록은 API 예외·에러 정의서(노션/구글 시트 참고, 59개)나 영문 요약 [`docs/agent/spec/errors.md`](../../agent/spec/errors.md)를 보세요.

백엔드: `BusinessException(ErrorCode)` 하나를 던지고 `@RestControllerAdvice`가 변환합니다. 스택 트레이스·SQL·외부 서비스 에러 문구는 응답에 넣지 않습니다. 요청마다 `traceId`를 만들어 로그(MDC)에 같이 남깁니다. 5xx와 롤백은 횟수를 차감하지 않습니다. 데이터가 없는 조회는 에러가 아니라 200과 빈 배열·0입니다.

프론트: 에러 코드별 화면 처리(토스트·모달·입력칸 아래 문구)를 `lib/api`의 매핑표 한 곳에서 정합니다. 모르는 코드는 `INTERNAL_ERROR` 화면으로 처리하고 `traceId`는 문의용으로만 보관합니다.

## 6. 정책에서 온 공통 제약

- 소셜 로그인만(카카오·네이버·구글). 로그인 없이는 어떤 페이지도 못 씁니다.
- 소비기한 상태: 위험(지났거나 D-DAY), 임박(D-1~D-3), 여유. 서버가 `status`, `dDay`를 내려주고 프론트는 다시 계산하지 않습니다.
- AI에 **자유 입력 금지**. 레시피·식단표 생성은 고른 옵션(id, enum)만 받고, 사용자가 쓴 글을 프롬프트에 붙이지 않습니다.
- 횟수: 레시피 기본 하루 2회(구독 4회), 식단표 월 1회·5일(구독 월 4회·7일). 구독은 1차에 UI만이라 기본 값만 적용. **실패하면 차감하지 않습니다.** 영수증 스캔은 횟수 제한이 없습니다.
- 단위(14개)·카테고리(12개)는 코드 목록만 씁니다. 자세한 건 [`docs/agent/spec/domain-rules.md`](../../agent/spec/domain-rules.md)(영문)나 PRD 부록 A·B(노션/구글 시트 참고).
- 공유 공간은 완전 익명: 어떤 응답에도 작성자 정보를 넣지 않습니다.
- 이벤트·로그·에러에 개인정보와 비밀키를 넣지 않습니다.

## 7. 협업 규칙

- 기능 영역별 브랜치, 작은 PR. 다른 영역 패키지를 고칠 일이 있으면 담당자에게 먼저 알립니다.
- 비즈니스 규칙에는 테스트를 씁니다(횟수 계산, 단위 환산, 소비기한 상태, 식단표 재료 배정, 랭킹).
- API 경로·에러 코드·이벤트 이름은 바꾸면 프론트·에러표·분석이 같이 깨지므로 정의서를 먼저 고치고 알립니다.
- 정의서에 "제안"·"미결"로 표시된 항목은 상수나 설정으로 분리해 구현하고 PR에 적어 둡니다.
- 새 이벤트는 이벤트 정의서(구글 시트)에 먼저 추가합니다.
