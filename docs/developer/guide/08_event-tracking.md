# 08. 이벤트 트래킹 가이드

이 프로젝트의 핵심 기능입니다. 사용자 행동을 이벤트로 쌓고 지표(KPI)·퍼널로 모니터링합니다. 팀원 모두 "이벤트 데이터를 남기는 API/모듈을 활용"했다고 쓸 수 있도록 **이준호가 공통 `EventTracker` 모듈과 수집 API**를 만들고, 각 담당자가 **자기 API에서 서버 이벤트를 발행**합니다.

기준 문서: 이벤트 정의서 v2(피그마 기준, 2026-11-22, 노션. 이벤트 79개, 속성 102개, 지표 21개, 화면 코드 41개). 영문 전체 목록은 [`docs/agent/spec/events.md`](../../agent/spec/events.md), 영역별 이벤트 표는 각 영역 문서(01~07)의 "발행 이벤트"에 있습니다.

## 1. 구조

```
[프론트] track(...) ──묶음(최대 50개)──▶ POST /api/v1/user/events(API 56) ──▶ ┐
[백엔드] eventTracker.track(...) (API·정기 작업 끝 지점) ──────────────────▶ ├▶ events 테이블 ▶ 지표 계산(SQL)
```

- 프론트 이벤트(화면 노출·클릭)와 서버 이벤트(OCR·AI 결과, 한도, 알림, 보상, 재료 등록·차감, 장바구니 담기·빼기)를 나눠 적재합니다. **같은 행동은 한쪽에서만** 기록합니다(정의서 "발송" 칸이 FE/BE).
- 외부 분석 도구는 쓰지 않습니다. 원본은 180일 보관합니다.
- 일별 집계 테이블(`event_daily_stats`)은 없습니다. 정의서 5장 계산식(영문 [`events.md`](../../agent/spec/events.md) Metrics)대로 `events`를 직접 조회하고(운영자 페이지 a1~a3·a6에서 조회, API 61~64), 일별 집계 테이블은 개발하며 판단합니다.

## 2. 이벤트의 모양

모든 이벤트에 붙는 공통 값: `user_id`(내부 회원 번호, 로그인 전 null, 탈퇴해도 번호만 남음), `session_id`(앱을 연 뒤 30분 무행동이면 새 세션), `source`(CLIENT/SERVER), `screen`(화면 코드, 서버 이벤트는 요청 헤더 `X-Screen`, 정기 작업은 null), `platform`(TWA/PWA/WEB, 헤더 `X-Client-Platform`), `app_version`(헤더 `X-App-Version`), `is_seed`(시연용 가상 이벤트), `occurred_at`(한국 시간).

이벤트별 값은 `properties`(JSON). **중첩 없이 문자·숫자·참거짓만, 25개 이하.** 코드값은 소문자입니다.

```json
{ "name": "receipt_saved", "screen": "receipt_result",
  "properties": { "registered_count": 8, "ocr_item_count": 9, "edited_item_count": 2,
                  "added_item_count": 0, "cart_removed_count": 1,
                  "duration_sec": 74 } }
```

가구원 수·구독 여부·가입일·로그인 제공자 같은 사용자 속성은 이벤트에 싣지 않고 `users`와 조인해서 분석합니다.

## 3. 절대 넣으면 안 되는 것 (개인정보·자유 입력)

- 이메일, 이름, 소셜 ID
- 알레르기(제외 재료) 이름, 사용자가 입력한 재료 이름, 레시피 내용, 영수증 사진, 그 밖의 모든 자유 텍스트
- 대신: **카테고리 코드**(`category_code`), **개수**(`excluded_count`, `item_count`), **enum/참거짓**을 씁니다.
- 비밀키, 토큰, 에러 원문(스택·외부 서비스 응답)도 넣지 않습니다. 에러는 에러 정의서 코드를 `error_code`에 그대로.

백엔드 `EventTracker`는 이 규칙을 **필터로 강제**합니다(허용 속성 목록에 없는 키는 버림, 25개 초과·중첩 거부, 금지 키워드 차단). 그래도 호출하는 쪽에서 처음부터 안 넣는 게 원칙입니다.

## 4. 백엔드에서 발행하기

영수증 저장 E34의 `cart_removed_count`는 #57의 선택 차감 후 잔량이 0이 되어 실제 삭제된 장바구니 행 수예요. 차감 수량 합이나 요청 건수가 아니며, 부분 차감·미선택이면 0이에요.

`event` 패키지의 `EventTracker`(`event/application/port/in`)를 주입받아 **API가 끝나는 지점**(성공·실패·거절)에서 호출합니다.

```java
// 성공 후
eventTracker.track(EventName.RECEIPT_SAVED, Screen.RECEIPT_RESULT, Map.of(
    "registered_count", registeredCount,
    "ocr_item_count", ocrItemCount),
    EventTrackCommand.ofUser(userId));

// 실패·거절도 발행 (예: 한도 소진)
eventTracker.track(EventName.QUOTA_EXCEEDED, Screen.RECIPE_CREATE, Map.of(
    "limit_feature", "recipe", "limit_total", 2, "used_count", 2),
    EventTrackCommand.ofUser(userId));
```

규칙

- 모듈은 커밋 뒤 **비동기**로 적재하고, 실패해도 요청을 깨뜨리지 않습니다(로그만, 자동 재시도 없음). 롤백된 행동은 기록되지 않습니다.
- 이벤트 이름·속성은 enum/상수로 관리(정의서와 1:1). 문자열 직접 입력 금지.
- AI 실패는 `usage_logs`에 남기지 않으므로 실패 이벤트가 유일한 기록입니다. 실패 지점에서 꼭 발행합니다.
- 각 API의 "발행 이벤트"가 그 API 담당자의 책임입니다(각 영역 문서, 영문 [`api-summary.md`](../../agent/spec/api-summary.md)).
- 서버 이벤트 34개 담당: 이준호 11(로그인·가입·온보딩·계정 삭제·구독 5개), 김재연 1(`ingredient_discarded`)과 공동 2(`ingredient_added`, `ingredient_used`), 이동준 3(OCR 성공·실패, 영수증 등록), 김종수 3(레시피 생성 성공·실패, 요리 완료), 한윤수 5(식단표 성공·실패, 식단 삭제, 레시피 교체 성공·실패), 이상호 2(기한 지남 발견, 알림 발송), 최유리 5(장바구니 담기, 장바구니 → 냉장고, 공유 게시·신고, 주간 보상). 공동 발행: `quota_exceeded`(김종수 28·29, 한윤수 38·42), `ingredient_added`(김재연 15, 이동준 21, 최유리 47), `ingredient_used`(김재연 18, 김종수 32), `recipe_saved`(김종수 34, 최유리 53).
- 결정 [#46](https://github.com/likelion-backendschool-9th/REMAKE/issues/46): `cart_item_added`는 `AddCartItemsUseCase`만 발행합니다([07](07_shared-cart.md)).

## 5. 프론트에서 발행하기

`lib/events`의 트래커 하나만 씁니다(컴포넌트에서 직접 fetch 금지). 위치와 규칙은 [`../frontend-blueprint.md`](../frontend-blueprint.md) §11.

```ts
track('receipt_item_edited', { item_index: 2, edit_field: 'amount' });
```

- `screen`·`sessionId`·`occurredAt`은 트래커가 붙입니다. 이벤트는 메모리 큐에 모아 일정 주기, 화면 숨김·닫힘(`visibilitychange`/`pagehide` → `navigator.sendBeacon`), 로그아웃 때 최대 50개씩 보냅니다.
- 전송 실패는 조용히 무시하고(작은 횟수만 재시도) 사용자 화면에 영향을 주지 않습니다.
- 정의서에서 **FE**로 표시된 이벤트만 보냅니다. 서버가 이미 기록하는 행동(OCR 결과, AI 생성 결과, 알림 발송, 재료 등록)을 프론트가 또 보내지 않습니다.
- `screen_viewed`는 공통 레이아웃 훅 한 곳에서 처리하고, 화면 코드 41개(`fridge_list`, `receipt_result`, `recipe_detail`, `meal_plan_calendar` 등)는 `lib/events/screens.ts` 상수를 씁니다. 모든 API 요청에 지금 화면 코드를 `X-Screen` 헤더로 붙입니다.
- 화면에 보인 오류는 `error_shown`(`error_code`, `http_status`, `screen_name`)으로 보냅니다. 카메라를 못 쓸 때도 `CAMERA_UNAVAILABLE`로 여기서 기록합니다.
- 서버는 모르는 이벤트 이름을 버리고 `{ accepted, rejected }`로 응답합니다(202). 한 번에 50개 초과는 `EVENT_BATCH_TOO_LARGE`.

## 6. 새 이벤트를 추가·변경할 때

1. 이벤트 정의서(노션)에 이름·발생 시점·화면·속성·발송(FE/BE)·우선순위·연결 지표·발행 API를 먼저 적고 이준호에게 알립니다.
2. 이름은 `snake_case`, `대상_행동` 과거형(`receipt_saved`). 한 번 정하면 바꾸지 않습니다(바꾸면 지표 계산·적재 코드를 같이 고쳐야 함, 변경은 버전 기록).
3. 개인정보·자유 입력이 들어가지 않는지 확인합니다.
4. 코드 상수에 추가 → 발행 → `events` 테이블에 들어오는지 확인합니다.

## 7. 지표

- **북극성 지표(M00)**: 소비기한 내 소진율 = `ingredient_used` ÷ (`ingredient_used` + `ingredient_discarded` + `ingredient_expired`).
- KPI 20개(M01~M20): 온보딩 완료율, 첫 재료 등록률, OCR 성공률·품목 수정률, 영수증 등록 소요 시간, 임박 알림 확인율, 임박 재료 소진율, 레시피 생성 → 요리 완료율, AI 생성 성공률·응답 시간, 식단표 → 칸 열람률, 장바구니 → 냉장고 전환율, 공유 작성 완료율, 공유 저장율·신고율, D1·D7·WAU, AI·OCR 호출 수와 한도 소진율, 핵심 흐름 퍼널, 이번 달 버린 식재료 금액, 소비 패턴 이용률, 오류 노출률. 계산식은 영문 [`events.md`](../../agent/spec/events.md) Metrics.
- 실사용자가 적으므로 시연용 **시드 데이터 생성 스크립트**를 만들고(`is_seed = true`), 지표 계산 때 실제 데이터만 보려면 `is_seed = false`로 거릅니다.
- 어드민(모니터링)은 운영자 페이지 a1~a3·a6에서 조회합니다(API 61~64, 11/26, 백엔드 이준호).

## 8. 지금 코드와 다른 점

- `EventName`은 정의서 79개와 맞습니다(`EventContractTest`). `cook_feedback_answered`(E50)는 `recipe_id`·`recommended`를 씁니다. E86 `subscription_renewed` 호출은 자동 결제 작업(#168)에서 이어집니다.
- `Screen` enum은 정의서 화면 코드 41개(+ `UNKNOWN`)와 맞습니다. 스케줄·배치처럼 화면이 없으면 `null`을 넘깁니다.
- 속성 코드 값은 소문자입니다(#172). `storage`·`filter_value`만 정의서대로 대문자입니다. 검증기와 서버 송신은 한 PR에서 같이 바꿨고, 이미 쌓인 대문자 행은 Flyway `V30`으로 소문자로 고쳤습니다.
- 우선순위는 정의서를 따릅니다. 결정 [#58](https://github.com/likelion-backendschool-9th/REMAKE/issues/58)대로 공유·장바구니·보상 이벤트는 P1이고, #58의 어드민 부분(ADMIN 접근 제어 선행, 분석 API)은 운영자 페이지 a1~a3·a6에서 조회(API 61~64)로 정리됐습니다(11/26).

## 9. 체크리스트

- [ ] 내가 맡은 API의 "발행 이벤트"를 성공·실패·거절 지점에서 모두 발행
- [ ] 속성에 이메일·이름·재료명·알레르기·자유 입력이 없다
- [ ] 같은 행동을 프론트와 서버가 중복 기록하지 않는다
- [ ] 트래킹 실패가 사용자 기능을 막지 않는다(비동기, 예외 삼킴)
- [ ] 정의서와 코드의 이벤트 이름·속성·화면 코드가 일치한다
