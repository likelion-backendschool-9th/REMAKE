# 08. 이벤트 트래킹 가이드

이 프로젝트의 핵심 기능입니다. 사용자 행동을 이벤트로 쌓고 어드민에서 KPI·퍼널로 모니터링합니다. 팀원 모두 "이벤트 데이터를 남기는 API/모듈을 활용"했다고 쓸 수 있도록 **이준호가 공통 `EventTracker` 모듈과 수집 API**를 만들고, 각 담당자가 **자기 API에서 서버 이벤트를 발행**합니다.

기준 문서: 이벤트 데이터 정의서(노션/구글 시트 참고. 이벤트 76개, 속성, 지표 21개, 화면 코드 38개). 영문 전체 목록은 [`docs/agent/spec/events.md`](../../agent/spec/events.md).

## 시연 우선순위와 선행 관계 ([#58](https://github.com/likelion-backendschool-9th/REMAKE/issues/58) 확정)

| 작업 | 확정 우선순위·범위 | 선행 관계·검증 |
| --- | --- | --- |
| 8.1 ADMIN 접근 제어 중 분석 API에 필요한 범위 | ADMIN 권한 검사·비관리자 접근 차단만 P0로 조정 | 실제 보안 필터로 ADMIN 허용·일반 사용자 403·비로그인 차단을 먼저 검증 |
| 8.2 KPI·퍼널·이벤트 로그 분석 API | P0 유지 | 위 접근 제어를 선행 구현·검증한 뒤 분석 API 구현·시연 |
| 그 외 어드민 기능 | P1 유지 | 분석 API의 선행 범위에 포함하지 않음 |
| 11.8 이벤트 발행·검증 | P0 검증은 1차 시연에 포함되는 기능의 이벤트로 한정 | 해당 기능의 성공·실패·거절 지점에서 계약대로 발행·수집되는지 검증 |
| 공유·장바구니·보상 및 관련 이벤트 | 기능은 P1 유지, 이벤트 발행·검증도 각 P1 기능 일정에 맞춤 | P0 시연을 위해 이 기능을 앞당기지 않음 |

- ADMIN 접근 제어는 [#84](https://github.com/likelion-backendschool-9th/REMAKE/issues/84), 분석 API는 [#93](https://github.com/likelion-backendschool-9th/REMAKE/issues/93), 공통 이벤트 수집·서버 트래커는 [#65](https://github.com/likelion-backendschool-9th/REMAKE/issues/65), 프론트 트래커는 [#116](https://github.com/likelion-backendschool-9th/REMAKE/issues/116)에서 구현합니다. 이 문서의 검증 기준은 구현 완료를 뜻하지 않습니다.
- P1 기능 일정에 맞추는 이벤트는 `cart_item_added`, `cart_viewed`, `cart_amount_changed`, `cart_items_deleted`, `ranking_viewed`, `shared_recipe_viewed`, `shared_write_started`, `shared_recipe_published`, `shared_recipe_saved`, `shared_recipe_reported`, `weekly_reward_granted`입니다. `recipe_saved`의 공유 저장 경로도 P1이며, 내 레시피 저장 경로는 해당 P0 시연 기능 일정에 따릅니다. 이벤트별 기존 세부 우선순위(P1/P2)는 [이벤트 명세](../../agent/spec/events.md)를 따릅니다.
- 이벤트 이름·속성·CLIENT/SERVER 구분·발행 책임은 변경하지 않습니다. 원래 P0였던 공유·장바구니 이벤트의 구현·검증 우선순위만 P1로 맞춥니다.
- **WBS 반영은 팀장님께 별도 요청해야 합니다.** 요청 내용은 ① 분석 API에 필요한 8.1 접근 제어를 P0로 조정하고 `ADMIN 권한 검사·비관리자 접근 차단 → 8.2 분석 API` 선행 관계 명시 ② 11.8의 P0 검증 대상을 1차 시연 기능 이벤트로 한정 ③ 공유·장바구니·보상 이벤트는 각 P1 기능 일정에 맞춘다고 기록하는 것입니다. 이 문서 수정만으로 WBS가 갱신되지는 않습니다.

## 1. 구조

```
[프론트] track(...) ──배치(최대 50개)──▶ POST /api/v1/user/events ──▶ ┐
[백엔드] eventTracker.track(...) (API 끝 지점, 배치) ───────────────────▶ ├▶ events 테이블 ▶ 일일 집계 event_daily_stats ▶ 어드민
```

- 프론트 이벤트(화면 노출·클릭)와 서버 이벤트(OCR·AI 결과, 한도, 알림 배치)를 나눠 적재합니다. **같은 행동은 한쪽에서만** 기록합니다(정의서 "발송" 열이 FE/BE).
- 어드민은 외부 분석 도구 없이 `events`를 직접 집계해 보여줍니다. 원본은 180일 보관 후 삭제, 일별 집계는 유지.

## 2. 이벤트의 모양

모든 이벤트에 붙는 공통 값: `user_id`(내부 회원 번호, 로그인 전·탈퇴 후 null), `session_id`(앱을 연 뒤 30분 무행동이면 새 세션), `source`(CLIENT/SERVER), `screen`(화면 코드), `platform`(PWA/TWA/WEB), `app_version`, `is_seed`(시연용 가상 이벤트), `occurred_at`(한국 시간).

이벤트별 값은 `properties`(JSON). **중첩 없이 문자·숫자·참거짓만, 25개 이하.**

```json
{ "name": "receipt_saved", "screen": "receipt_result",
  "properties": { "registered_count": 8, "ocr_item_count": 9, "edited_item_count": 2,
                  "deleted_item_count": 1, "added_item_count": 0, "duration_sec": 74 } }
```

## 3. 절대 넣으면 안 되는 것 (개인정보·자유 입력)

- 이메일, 이름, 소셜 ID
- 알레르기(제외 재료) 이름, 사용자가 입력한 재료 이름, 레시피 내용, 그 밖의 모든 자유 텍스트
- 대신: **카테고리 코드**(`category_code`), **개수**(`excluded_count`, `item_count`), **enum/참거짓**을 씁니다. 가구원 수 같은 사용자 속성은 이벤트에 싣지 않고 `users`와 조인해서 분석합니다.
- 비밀키, 토큰, 에러 원문(스택·외부 서비스 응답)도 넣지 않습니다. 에러는 `error_code`만.

백엔드 `EventTracker`는 이 규칙을 **필터로 강제**합니다(허용 속성 목록에 없는 키는 버림, 25개 초과·중첩 거부, 금지 키워드 차단). 그래도 호출하는 쪽에서 처음부터 안 넣는 게 원칙입니다.

## 4. 백엔드에서 발행하기

`event` 패키지의 `EventTracker`를 주입받아 **API가 끝나는 지점**(성공·실패·거절)에서 호출합니다. 아래는 제안하는 모양이며 최종 시그니처는 이준호가 정합니다.

```java
// 성공 후
eventTracker.track(EventName.RECEIPT_SAVED, Screen.RECEIPT_RESULT, Map.of(
    "registered_count", registeredCount,
    "ocr_item_count", ocrItemCount));

// 실패·거절도 발행 (예: 한도 소진)
eventTracker.track(EventName.QUOTA_EXCEEDED, Screen.RECIPE_CREATE, Map.of(
    "limit_feature", "RECIPE", "limit_total", 2, "used_count", 2));
```

규칙

- 모듈은 **비동기**로 적재하고 실패해도 요청을 깨뜨리지 않습니다(예외 삼킴·로그만). 트랜잭션 밖(커밋 후)에서 발행해 롤백된 행동이 기록되지 않게 합니다.
- 이벤트 이름·속성은 enum/상수로 관리(정의서와 1:1). 문자열 직접 입력 금지.
- 각 API의 "발행 이벤트"가 그 API 담당자의 책임입니다(API 정의서 v3 표 참고, 노션/구글 시트. 영문 요약은 [`docs/agent/spec/api-summary.md`](../../agent/spec/api-summary.md)).
- 서버 이벤트 29개 담당: 이준호 6(로그인·가입·온보딩·계정 삭제), 김재연 3(재료 등록·사용·버림), 이동준 3(OCR 성공·실패, 영수증 확정), 김종수 4(레시피 생성 성공·실패, 저장, 요리 완료), 한윤수 5(식단표·칸 상세 성공·실패, 식단 추가), 이상호 2(기한 만료 발견, 알림 발송), 최유리 5(장바구니 담기, 공유 게시·저장·신고, 주간 보상). 한도 소진 `quota_exceeded`는 레시피·식단표 담당이 각자, 공유 저장 때 `recipe_saved`는 최유리도 발행.

## 5. 프론트에서 발행하기

`lib/events`의 트래커 하나만 씁니다(컴포넌트에서 직접 fetch 금지). 위치와 규칙은 [`../frontend-blueprint.md`](../frontend-blueprint.md) §11.

```ts
track('receipt_item_edited', { item_index: 2, edit_field: 'amount' });
```

- `screen`·`sessionId`·`occurredAt`은 트래커가 붙입니다. 이벤트는 메모리 큐에 모아 일정 주기, 화면 숨김·닫힘(`visibilitychange`/`pagehide` → `navigator.sendBeacon`), 로그아웃 때 최대 50개씩 보냅니다.
- 전송 실패는 조용히 무시하고(작은 횟수만 재시도) 사용자 화면에 영향을 주지 않습니다.
- 정의서에서 **FE**로 표시된 이벤트만 보냅니다. 서버가 이미 기록하는 행동(OCR 결과, AI 생성 결과, 알림 발송)을 프론트가 또 보내지 않습니다.
- `screen_viewed`는 공통 레이아웃 훅 한 곳에서 처리하고, 화면 코드(`fridge_list`, `receipt_result`, `recipe_detail`, `meal_plan_calendar` 등)는 `lib/events/screens.ts` 상수를 씁니다.
- 서버는 모르는 이벤트 이름을 버리고 `{ accepted, rejected }`로 응답합니다(202). 한 번에 50개 초과는 `EVENT_BATCH_TOO_LARGE`.

## 6. 새 이벤트를 추가·변경할 때

1. 이벤트 정의서(구글 시트)에 이름·발생 시점·화면·속성·발송(FE/BE)·우선순위·연결 지표·발행 API를 먼저 적고 이준호에게 알립니다.
2. 이름은 `snake_case` 동사 과거형(`receipt_saved`). 한 번 정하면 바꾸지 않습니다(바꾸면 대시보드·적재 코드를 같이 고쳐야 함, 변경은 버전 기록).
3. 개인정보·자유 입력이 들어가지 않는지 확인합니다.
4. 코드 상수에 추가 → 발행 → 어드민에서 `GET /admin/events`로 들어오는지 확인합니다.

## 7. KPI와 어드민

- **북극성 지표**: 소비기한 내 소진율 = 사용했어요 ÷ (사용했어요 + 버렸어요 + 기한이 지난 채 남은 재료).
- KPI 20개 예: 온보딩 완료율, 첫 재료 등록률, OCR 성공률·품목 수정률, 임박 알림 열기율, 임박 재료 소진율, 레시피·식단표 생성 성공률·응답 시간, 부족 재료 → 장바구니 담기율, 공유 작성 완료율, 신고율, D1·D7 재방문, AI·OCR 일별 호출과 한도 소진 비율. 산식은 정의서 "지표 연결" 탭.
- 어드민 API: 대시보드 요약, KPI 시계열, 퍼널(가입→온보딩→첫 재료→첫 레시피→요리 완료, 영수증 스캔→확정→재료 등록), 이벤트 로그, 이벤트 카탈로그, 일별 호출·비용.
- 실사용자가 적으므로 시연용 **시드 데이터 생성 스크립트**를 만들고(`is_seed = true`), 어드민에서 포함·제외를 선택할 수 있게 합니다.
- 매일 새벽 배치가 전날 `events`를 `event_daily_stats`로 집계하고 180일 지난 원본을 삭제합니다.

## 8. 체크리스트

아래 발행·검증 항목은 위 시연 범위에 따라 P0 기능 이벤트를 먼저 확인하고, 공유·장바구니·보상 이벤트는 각 P1 기능 구현 시 확인합니다.

- [ ] 내가 맡은 API의 "발행 이벤트"를 성공·실패 지점에서 모두 발행
- [ ] 속성에 이메일·이름·재료명·알레르기·자유 입력이 없다
- [ ] 같은 행동을 프론트와 서버가 중복 기록하지 않는다
- [ ] 트래킹 실패가 사용자 기능을 막지 않는다(비동기, 예외 삼킴)
- [ ] 정의서와 코드의 이벤트 이름·속성이 일치한다
