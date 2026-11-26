# 06. 알림·어드민 운영 (이상호)

기준: PRD v4·정책정의서 v4·ERD v4·API 정의서 v4·에러 정의서 v2·이벤트 정의서 v2(모두 피그마 기준, 2026-11-22, 노션). 어드민은 11/26 피그마 '운영자 페이지'(2005:4) 기준 반영. 표는 원본에서 그대로 옮겼고 피그마 노드 근거 칸만 뺐어요.

## 범위

소비기한 알림(앱 안 알림 띠·알림 화면)과 알림 정기 작업, 어드민 운영(신고 관리·사용자 관리·랭킹·보상, API 65~71) 백엔드. 어드민 권한(ADMIN만 `/api/v1/admin/**`)도 이 영역이에요. 어드민 모니터링(대시보드·퍼널·기능별 지표·이벤트 로그, API 61~64) 백엔드는 이준호 담당이라 [`docs/agent/spec/api-summary.md`](../../agent/spec/api-summary.md) "Admin: monitoring"을 보세요.

## 화면

화면 코드는 이벤트의 `screen` 값이에요. "추가될 페이지"는 아직 피그마에 없는 화면이에요.

| 화면 코드 | 피그마 화면 |
| --- | --- |
| `notification_list` | p6 알림 |

## 정책

상태: 확정 = 그대로 구현, 제안 = 상수로 분리해 구현하고 PR에 적기, 미결 = 구현 전에 준호님께 질문, 화면 대기 = 규칙은 정해졌고 화면이 추가될 예정.

### 8. 알림

| 항목 | 규칙 | 상태 |
| --- | --- | --- |
| 알림 종류 | 소비기한 임박(EXPIRY_SOON), 기한 지난 재료(EXPIRED), 식단표 완성(MEAL_PLAN_COMPLETED), 식단표 생성 실패(MEAL_PLAN_FAILED) 네 가지 | 확정 |
| 알림 문구 | "OO의 소비기한이 얼마 남지 않았어요. 제품의 포장지에서 사용 기한을 확인해 주세요", "기한이 지난 재료 N개가 있어요. 냉장고를 확인해 주세요". 식단표(05): "11월 24일부터 5일치 식단표가 완성됐어요"(제안), "식단표를 만들지 못했어요. 잠시 후 다시 만들어 주세요. 생성 횟수는 차감되지 않았어요", "냉장고 재료가 부족해 식단표를 만들지 못했어요. 생성 횟수는 차감되지 않았어요" | 확정 / 완성 문구는 제안 |
| 채널 | 앱 안에서만(상단 알림 띠·알림 화면). 기기 푸시 없음 | 확정 |
| 알림 띠 | 모든 탭 화면 상단에 최신 알림 한 줄 + 안 읽은 개수 빨간 숫자. 누르면 알림 화면 | 확정 |
| 알림 화면 | 오늘·1일 전·2일 전 묶음. 소비기한 알림 항목은 누르는 동작 없음. 식단표 완성 알림은 p5에서 그 식단표 시작일, 실패 알림은 식단표 생성 설정 화면으로 이동. 회색 글자는 읽음 표시가 아니라 글자 색 | 확정 |
| 읽음 처리 | 알림 화면을 열면 모두 읽음. 개별 읽음·모두 읽음 버튼 없음 | 확정 |
| 빈 화면 | 알림 0건 빈 화면(추가 예정) | 화면 대기 |
| 임박 기준 | 소비기한이 오늘~3일 뒤(D-3~D-DAY)인 재료는 그동안 매일 재료마다 한 건. 냉장고 화면에서 D-DAY는 "위험"이지만 알림은 "임박"(의도한 차이) | 확정 |
| 기한 지남 | 기한 지난 재료는 하루 한 건으로 묶어 개수로 알림. 기한 지남 이벤트는 처음 찾았을 때 한 번 | 확정 |
| 알림 시각 | 소비기한 알림은 매일 오전 9시(한국 시간) 정기 작업. 식단표 알림은 생성 작업이 끝날 때 | 확정 |
| 알림 끔 | 알림 받기를 끈 회원은 소비기한 알림을 만들지 않음. 식단표 알림은 끈 회원에게도 만듦 | 확정 |
| 보관 | 30일 지난 알림은 매일 새벽 삭제 | 확정 |
| 기준일 설정 | 사용자가 바꿀 수 있는 기준일·알림 시각 설정 없음 | 확정 |

### 10. 어드민

| 항목 | 규칙 | 상태 |
| --- | --- | --- |
피그마 '운영자 페이지'(etTm84sh 2005:4)가 기준이에요(11/26). 화면과 API는 API 정의서 61~71에 있어요.

| 항목 | 규칙 | 상태 |
| --- | --- | --- |
| 접근 | 소셜 로그인 + ADMIN role로 /admin. 별도 로그인 없음. /admin은 대시보드(/admin/monitoring/dashboard)로 이동 | 확정 |
| 권한 없음 | ADMIN이 아니면 서버가 403(FORBIDDEN), 화면은 "운영자만 들어올 수 있어요". 프론트는 프로필 role로 먼저 판단 | 확정 |
| 개인 정보 | 어드민 화면은 회원 번호만 보여 줌. 이메일 등은 보이지 않음 | 확정 |
| 지표 기준 | 기간은 오늘·최근 7일·최근 30일, "실제 데이터만"이면 시드(is_seed) 제외. 분모가 0이면 "-"로 보이고 목표 배지 숨김. 계산식은 이벤트 정의서 5장 | 확정 |
| 이상 징후 | OCR 실패율과 신고 수만 봄. 최근 1시간 값이 지난 7일 같은 시간대 평균의 2배 이상이면 대시보드 맨 위에 띄움 | 확정 |
| 퍼널 이탈 | 이전 단계 대비 70% 미만이면 "이탈 큼" | 확정 |
| 신고 묶음 | 같은 글 신고는 한 줄로 묶고 신고 수가 많은 순. 처리는 글 단위로 그 글의 대기 신고를 한 번에 | 확정 |
| 신고 처리 결과 | 위반으로 처리 = 글 숨김 + 다음 제재. 문제 없음 = 글 그대로, 제재 없음. 글 삭제 = 공유 공간에서 지우고(되돌릴 수 없음) 위반으로 세어 다음 제재. 모두 확인 모달 후 처리 | 확정 |
| 제재 단계 | 확정된 위반 1차 7일 레시피 공유 정지, 2차 30일 공유 정지, 3차 30일 계정 정지, 4차 영구 계정 정지. 4차 직전에는 상세에 경고 | 확정 |
| 동시 처리 | 다른 운영자가 먼저 처리했으면 "이미 처리한 신고" 안내 후 목록을 새로 불러옴(409) | 확정 |
| 계정 제한 | 운영자가 직접 정지·해제하는 기능은 없고 제재로만 걸림. 제재 중인 사용자가 앱에서 보는 화면은 추가될 페이지 15번(그전까지 앱 쪽 차단 규칙은 보류) | 확정 / 앱 화면은 보류 |
| 횟수 조정 | 운영자가 회원별 보상 생성 횟수를 늘리거나 줄임. 사유 필수, 조정 이력에 남음. 남은 보상보다 많이 줄일 수 없음 | 확정 |
| 숨김·삭제 | 숨긴(HIDDEN)·삭제한(DELETED) 공유 레시피는 공유 목록·랭킹·보상에서 빠짐 | 확정 |
| 랭킹·보상 조회 | 주간 TOP 3와 지급 이력만 보여 줌. 저장 수 0인 레시피는 보상 없음 | 확정 |

## API

경로의 `/api/v1`은 줄였어요. 요청의 `?`는 선택 값, `|`는 그중 하나예요. 번호는 API 정의서 번호예요(다른 문서도 이 번호로 가리켜요).

| 번호 | 메서드 | 경로 | 설명 | 요청 | 응답 | 관련 테이블 |
| --- | --- | --- | --- | --- | --- | --- |
| 43 | GET | `/user/notifications/summary` | 가장 최근 알림 문구와 안 읽은 수(11/21 답 30) | `-` | `200 { latestMessage, unreadCount }` | notifications |
| 44 | GET | `/user/notifications` | 알림 목록(최근 것부터). 종류 EXPIRY_SOON·EXPIRED·MEAL_PLAN_COMPLETED·MEAL_PLAN_FAILED. 소비기한 알림은 누르는 동작 없음, 식단표 완성은 p5 시작일(`mealPlanId`), 실패는 생성 설정 화면. 이 목록을 불러오면 안 읽은 알림이 모두 읽음이 됨(따로 읽음 처리 API 없음, API 답 4) | `?page=0&size=20` | `200 { content: [{ id, type, message, mealPlanId, createdAt }], page, size, totalElements, hasNext }`(`mealPlanId`는 MEAL_PLAN_COMPLETED만, 나머지는 null) | notifications |

기기 푸시가 없어 푸시 구독 API는 없어요.

### 어드민 운영 API (11/26)

모든 `/api/v1/admin/**`는 ADMIN만(아니면 403 `FORBIDDEN`). 회원은 회원 번호만 내려줘요.

| 번호 | 메서드 | 경로 | 설명 | 요청 | 응답 | 관련 테이블 |
| --- | --- | --- | --- | --- | --- | --- |
| 65 | GET | `/admin/reports` | 신고 목록. 같은 글 신고는 한 줄(글 단위), 신고 수 많은 순. `pendingCount`는 사이드 메뉴 숫자 | `?status=PENDING\|DONE&page=0&size=20` | `200 { pendingCount, content: [{ sharedRecipeId, title, reportCount, reasonCounts: [{ reason, count }], lastReportedAt, authorViolationCount, status, result?, sanction?: { level, type, days }, handledAt?, handledBy? }], page, size, totalElements, hasNext }` | shared_recipe_reports, shared_recipes, user_sanctions |
| 66 | GET | `/admin/reports/{sharedRecipeId}` | 신고 상세. 공유 레시피를 사용자에게 보이는 그대로, 신고 목록, 작성자 위반·제재, 위반 처리 때 적용될 다음 제재 | `-` | `200 { sharedRecipe: { id, title, imageUrl, createdAt, saveCount, recipe(재료·단계·인분·시간) }, status, reports: [{ reportedAt, reason, reporterId }], author: { userId, violationCount, currentSanction? }, nextSanction: { level, type, days(영구면 null) }, handled?: { result, handledAt, handledBy, sanction? } }` | shared_recipes, shared_recipe_reports, recipes, user_sanctions |
| 67 | PATCH | `/admin/reports/{sharedRecipeId}` | 신고 처리(글 단위). VIOLATION = 글 숨김 + 다음 제재, NO_ISSUE = 그대로, DELETED = 글 삭제 + 위반으로 세어 다음 제재. 대기 신고가 없으면 409 | `{ result: VIOLATION\|NO_ISSUE\|DELETED }` | `200 { result, sanction?: { level, type, endsAt } }` | shared_recipe_reports, shared_recipes, user_sanctions |
| 68 | GET | `/admin/users` | 사용자 목록·회원 번호 검색 | `?filter=ALL\|SANCTIONED&userId=&page=0&size=20` | `200 { content: [{ userId, provider, joinedAt, plan, violationCount, sanction?: { type, endsAt }, deletedAt? }], page, size, totalElements, hasNext }` | users, user_sanctions |
| 69 | GET | `/admin/users/{userId}` | 사용자 상세 패널 | `-` | `200 { userId, provider, joinedAt, deletedAt?, quota: { todayRemaining, rewardRemaining, blocked }, currentSanction?, sanctions: [{ level, type, startsAt, endsAt, sharedRecipeTitle }] }` | users, user_sanctions, usage_logs, generation_quota_grants |
| 70 | POST | `/admin/users/{userId}/generation-quota-adjustments` | 보상 생성 횟수 늘리기·줄이기. 사유 필수. 줄일 때는 남은 보상보다 많이 줄일 수 없음 | `{ delta, reason }` | `201 { rewardRemaining }` | generation_quota_grants |
| 71 | GET | `/admin/weekly-rewards` | 주간 TOP 3와 보상 지급 이력. week를 비우면 지난주 | `?week=2026-11-17` | `200 { weeks: [weekStart], week: { weekStart, weekEnd, grantedAt?, top: [{ rank, sharedRecipeId, title, saveCount, authorId, reward, granted }] }, history: [{ weekStart, winners: [{ rank, userId }], totalReward }] }` | shared_recipe_weekly_ranks, generation_quota_grants |

## 테이블

`notifications`. 재료 조회는 02 UseCase로 해요. 어드민 운영은 `shared_recipe_reports`(처리 결과 `result`·`handled_by`·`handled_at`), `user_sanctions`(제재), `generation_quota_grants`(`grant_type`·`reason`·`admin_id`)를 써요. 컬럼은 [`../erd.md`](../erd.md)를 보세요.

## 구현 메모

- 매일 오전 9시(한국 시간) 정기 작업이 알림을 만들어요. 임박(`EXPIRY_SOON`)은 D-3~D-DAY 재료마다 매일 한 건, 기한 지남(`EXPIRED`)은 하루 한 건으로 묶어 개수만 적어요.
- 알림 받기를 끈 회원(`users.notification_enabled = false`)은 소비기한 알림을 만들지 않아요. 식단표 완성·실패 알림은 끈 회원에게도 만들어요.
- 식단표 알림은 05의 생성 작업이 끝날 때 만들어요. 완성 알림은 `notifications.meal_plan_id`에 식단표를 연결하고, 누르면 p5에서 그 식단표 시작일을 열어요. 실패 알림은 식단표 생성 설정 화면으로 가요. `notification_sent`(E66)는 식단표 알림에도 보내요(`notification_type` `meal_plan_completed`·`meal_plan_failed`, `batch_count` 1).
- `ingredient_expired` 이벤트는 기한 지난 재료를 처음 찾았을 때 한 번만 발행해요. `notification_sent`도 이 작업이 발행해요.
- 알림 목록(API 44)을 불러오면 안 읽은 알림을 모두 읽음으로 바꿔요. 따로 읽음 API는 없어요. 소비기한 알림 항목은 누르는 동작이 없고, 식단표 알림만 이동해요.
- 30일 지난 알림은 매일 새벽 정리 작업이 지워요.
- 신고 처리(API 67)는 글 단위예요. 그 글의 PENDING 신고를 모두 DONE으로 하고 `result`·`handled_by`·`handled_at`을 기록해요. VIOLATION이면 `shared_recipes.status = HIDDEN`, DELETED면 `DELETED`이고 둘 다 `user_sanctions`에 다음 단계 한 행(1차 SHARE_BAN 7일·2차 SHARE_BAN 30일·3차 ACCOUNT_BAN 30일·4차 ACCOUNT_BAN 영구)을 넣어요. NO_ISSUE면 글·제재 그대로예요. 대기 신고가 없으면 409 `REPORT_ALREADY_HANDLED`예요.
- 생성 횟수 조정(API 70)은 늘리면 ADMIN_ADJUST 행(`remaining` = `amount`, 한 달 유효)을 만들어요. 줄이면 `amount` 음수·`remaining` 0 행을 남기고, 남은 보상 행에서 기한 가까운 것부터 빼요(남은 보상보다 많이 줄일 수 없음).
- 운영자가 계정을 직접 정지·해제하는 API는 없어요. 제재 중인 사용자가 앱에서 보는 화면과 앱 API 차단(`ACCOUNT_SUSPENDED`, 공유 정지 오류)은 추가될 페이지 15로 보류라 지금은 만들지 않아요.

## 지금 코드와 다른 점

- 아직 이 영역 코드는 없어요. 이전 가이드의 웹 푸시(`push_subscriptions`), 영수증 완료 알림은 없어졌어요. 이전 어드민 API 중 감사 로그(`admin_audit_logs`)·공유 글 직접 관리·계정 직접 정지는 11/26에 없앴어요(api-summary "Admin: changes from the previous admin APIs").
- 기존 어드민 보류 이슈(#84·#85·#87·#94)는 이번 API 번호·범위로 정리가 필요해요.

## 에러 코드

`INVALID_INPUT`. 어드민 운영: `FORBIDDEN`(403, 61~71), `REPORT_NOT_FOUND`(404, 66·67), `REPORT_ALREADY_HANDLED`(409, 67), `USER_NOT_FOUND`(404, 69·70), `ADMIN_REASON_REQUIRED`(400, 70). 공통 코드(`UNAUTHORIZED`, `TOKEN_EXPIRED`, `RATE_LIMITED`, `INTERNAL_ERROR`, `SERVICE_UNAVAILABLE`)는 모든 API에 나올 수 있어요. API별 코드와 화면 표시는 [`docs/agent/spec/errors.md`](../../agent/spec/errors.md)에 있어요.

## 발행 이벤트

발송 FE는 화면 담당 프론트가 API 56으로 보내고, BE는 이 영역 담당이 발행 API가 끝나는 지점에서 `EventTracker`로 기록해요([08](08_event-tracking.md)).

| ID | 이벤트 | 발생 시점 | 속성 | 발송 | 우선순위 | 발행 API |
| --- | --- | --- | --- | --- | --- | --- |
| E66 | `notification_sent` | 소비기한 알림 작업이나 식단표 생성 작업(05)이 앱 안 알림을 만들었을 때(알림 1건당 1건) | notification_type, batch_count | BE | P0 | 서버 정기 작업 소비기한 알림(API 정의서 5장) |
| E67 | `notification_banner_clicked` | 상단 알림 띠를 눌러 알림 화면으로 갈 때 | unread_count | FE | P0 | 56 POST /user/events(프론트 전송) |
| E68 | `notification_list_viewed` | 알림 화면이 열릴 때(열면 모두 읽음) | unread_count | FE | P1 | 56 POST /user/events(프론트 전송) |

## 체크리스트

- [ ] 오전 9시 작업: 임박은 재료마다, 기한 지남은 하루 한 건
- [ ] 알림 끈 회원 제외
- [ ] 목록 조회로 모두 읽음, 알림 띠 안 읽은 수
- [ ] 30일 지난 알림 정리
- [ ] 식단표 완성·실패 알림(05 생성 작업이 만듦, 알림 끔과 관계없음)
- [ ] 어드민 권한: `/api/v1/admin/**`는 ADMIN만, 아니면 403 `FORBIDDEN`
- [ ] 신고 목록(글 단위)·상세·처리(위반·문제 없음·글 삭제, 제재 1~4차 자동, 이미 처리됨 409)
- [ ] 사용자 목록·상세·보상 생성 횟수 조정(사유 필수, 남은 보상보다 많이 줄이지 않음)
- [ ] 랭킹·보상 조회(주간 TOP 3·지급 이력)
