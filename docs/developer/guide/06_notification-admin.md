# 06. 알림 + 어드민 백엔드 (이상호)

## 범위

소비기한 알림(웹 푸시·알림 목록·상단 알림 띠), 알림 배치, 어드민 중 신고·사용자·호출 관리와 감사 로그(`admin_audit_logs`). KPI·퍼널·이벤트 로그 API는 이준호(01·08).

## 화면 (피그마 프레임)

p6-알람(+스켈레톤), 상단 알림 띠(냉장고·영수증 등 각 탭 상단, 안 읽은 개수 배지), p1.3-설정의 "알림 받기". **어드민 화면은 피그마에 아직 없습니다**(PRD §6 기획안 기준, 디자인은 이후).

## 알림 규칙

- 종류는 2개: `EXPIRY_SOON`(소비기한 임박, 재료별), `EXPIRED`(기한 지난 재료 N개, 하루 한 건). **"영수증 분석 완료" 알림은 없습니다**(OCR 동기). 피그마 알림 화면의 해당 줄은 삭제 예정.
- 임박 기준은 3일 고정, 알림 시각은 매일 오전 9시(KST) 고정(사용자 설정 없음).
- 설정은 "알림 받기" ON/OFF 하나. OFF인 사용자는 배치에서 제외.
- 알림 목록은 최근 30일을 오늘·1일 전·2일 전 식으로 묶고, 누르면 해당 재료/냉장고로 이동(`target`). 읽음 처리.
- iOS는 홈 화면에 추가한 PWA만 웹 푸시를 받습니다 → 설치 안내 필요, 출시 전 실측.

## API

| 메서드 | 경로 | 비고 |
|---|---|---|
| GET | `/api/v1/user/notifications/summary` | `{ latest, unreadCount }` 상단 띠 |
| GET | `/api/v1/user/notifications` | 목록 |
| PATCH | `/api/v1/user/notifications/{id}/read` | 읽음 |
| POST | `/api/v1/user/push-subscriptions` | `{ endpoint, keys }` |
| DELETE | `/api/v1/user/push-subscriptions/{id}` | 구독 해제 |

어드민(`/api/v1/admin/**`, ADMIN만): 신고 `GET /reports`, `PATCH /reports/{id}`(`HIDDEN`/`DELETED`/`DISMISSED`=문제 없음), 공유 레시피 `GET·PATCH /shared-recipes[/{id}]`, 사용자 `GET /users`(이메일 비노출), `PATCH /users/{id}/generation-quota`(`{ delta, reason }`), `PATCH /users/{id}/status`(`ACTIVE`/`SUSPENDED`+사유), `GET /usage/daily`(AI·OCR 호출 현황), `GET /audit-logs`, `GET /weekly-rewards`. 전체 목록은 API 정의서 v3(노션/구글 시트 참고)나 영문 요약 [`docs/agent/spec/api-summary.md`](../../agent/spec/api-summary.md).

## 테이블

`notifications`, `push_subscriptions`, `admin_audit_logs`(어드민 조치 이력).

## 배치 (`@Scheduled`)

**소비기한 알림**(매일 09:00): 오늘부터 3일 뒤까지(오늘이 기한인 D-DAY 포함) 재료는 기간 안에 있는 동안 매일 `EXPIRY_SOON`을, 기한이 오늘보다 이전인 재료는 `EXPIRED`(하루 한 건)를 생성 + 푸시. 재료 조회는 재료 쪽 UseCase(`GetExpiryAlertIngredientsUseCase`)가 판정해서 돌려줍니다. 화면(02 영역)은 D-DAY를 위험으로 보고 알림은 임박으로 봅니다(의도한 차이). 알림 OFF 사용자 제외. 같은 재료의 기한 지남은 처음 발견했을 때 한 번만 `ingredient_expired` 이벤트(재료당 1회).

## 어드민 구현 규칙

- 일반 사용자와 같은 소셜 로그인을 쓰고 `role=ADMIN`인 계정만 통과. 보안 설정에서 `/api/v1/admin/**`를 막고 권한이 없으면 403 `FORBIDDEN`. 프론트 `/admin`도 막지만 서버 검증이 기준.
- **모든 조작은 `admin_audit_logs`에 기록**(누가·무엇을·사유). 횟수 조정과 계정 제한은 **사유 필수**(`ADMIN_REASON_REQUIRED`).
- 신고는 자동으로 숨기지 않고 어드민이 처리합니다. `HIDDEN`·`DELETED` 처리 시 공유 레시피 상태도 함께 바뀌고, 숨김·삭제된 공유 레시피 상세는 일반 사용자에게 404.
- 사용자 조회에는 이메일 등 개인정보를 노출하지 않습니다(내부 ID·제공자·상태·가입·탈퇴 현황만).
- 제한된 계정은 로그인 뒤 API 호출에서 `ACCOUNT_SUSPENDED`(403).

## 에러 코드

`NOTIFICATION_NOT_FOUND`, `PUSH_SUBSCRIPTION_INVALID`, `REPORT_NOT_FOUND`, `USER_NOT_FOUND`, `ADMIN_REASON_REQUIRED`, `QUOTA_ADJUST_INVALID`, `SHARED_RECIPE_NOT_FOUND`, `FORBIDDEN`.

## 발행 이벤트

서버(2개): `ingredient_expired`, `notification_sent`(묶음당 1건, 속성 `notification_type`, `channel`, `batch_count`). 프론트: `notification_clicked`(푸시·알림 띠·목록, `click_from`), `notification_list_viewed`, `push_permission_result`.

## 체크리스트

- [ ] 09시 배치, 알림 OFF 제외, 푸시 발송·실패 구독 정리
- [ ] 상단 띠 `summary`와 목록·읽음 처리
- [ ] 어드민 403, 감사 로그 기록, 사유 필수 검증
- [ ] 신고 처리 → 공유 레시피 상태 연동
