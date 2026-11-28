# QA 시나리오: AUTH·SUB (로그인·설정·구독)

읽는 법·결과 기록 양식·테스트 준비물은 [README](README.md)에 있습니다.

## AUTH. 시작하기·설정·계정 (이준호)

화면 코드: `splash`, `login`(p1), `onboarding_household`(p1.1), `onboarding_allergy`(p1.2), `setting_main`(p1.3), `logout_modal`, `setting_household`(p1.3.1), `setting_allergy`(p1.3.2), `account_delete_modal`, `account_delete_code`.
API: 1 소셜 로그인, 2 토큰 재발급, 3 로그아웃, 4 온보딩 저장, 5·6 프로필 조회·수정, 7·8 제외 재료 조회·저장, 9 삭제 인증번호 발송, 10 계정 삭제.
**구현 상태(2026-11-24 기준)**: 1~8은 구현됨. 9·10(계정 삭제)은 구현 전(#68)이라 해당 시나리오는 **막힘**으로 시작한다. 실제 소셜 로그인은 카카오·네이버·구글 개발자 앱 설정이 있어야 하므로 로컬은 목 또는 테스트 계정으로 실행하고 어디까지 목인지 결과에 적는다.
v3에서 사라진 것: `/user/settings`, `/user/account/withdrawal/start·verify`, `catalogIds`, `users.status`(ACTIVE/SUSPENDED), `refresh_tokens.revoked_at`, 알레르기 경고 표시, 건강 상태 입력, 온보딩 건너뛰기.

### 로그인·토큰 (API 1, 2, 3)

| ID | 화면·API | 전제 | 행동·입력 | 기대 결과 | 방법 | 근거 | 우선 | 연결 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AUTH-01 | splash · login | 미로그인 | 앱 첫 진입(`/`) | 스플래시 뒤 로그인 화면(p1)으로. 로그인 상태면 냉장고로 간다 | E | 확정 | P0 | #98 |
| AUTH-02 | login · 1 | 처음 쓰는 카카오 계정 | 카카오 로그인 | 200 `{onboardingCompleted: false}`, 계정이 자동 가입되고 액세스·리프레시 토큰이 **HttpOnly 쿠키**로 설정된다(`Secure; SameSite=Lax`). 응답 본문에 토큰이 없다. 이벤트 `login_succeeded`(`is_new_user` true)와 `signup_completed` | A, E, V | 확정 | P0 | E09, E11 |
| AUTH-03 | 1 | 같은 계정으로 다시 로그인 | 로그인 | `onboardingCompleted`가 온보딩 완료 여부를 따른다. `signup_completed`는 나가지 않고 `is_new_user` false | A, V | 확정 | P0 | |
| AUTH-04 | 1 | 네이버·구글 각각 | 로그인 | 세 제공자 모두 로그인·가입된다 | A, E | 확정 | P0 | |
| AUTH-05 | 1 | 지원하지 않는 provider(`/oauth/facebook`) | 호출 | 400 `OAUTH_PROVIDER_UNSUPPORTED` | A | 확정 | P1 | |
| AUTH-06 | 1 | 만료·잘못된 `code`, 제공자 오류 | 호출 | 401 `OAUTH_FAILED`, 공통 토스트, 로그인 화면에 머문다. 이벤트 `login_failed`(`error_code`). **사용자가 취소한 경우는 실패로 세지 않는다** | A, E, V | 확정 | P0 | E10 |
| AUTH-07 | 1 | `code`·`redirectUri` 비움 | 호출 | 400 `INVALID_INPUT` | A | 확정 | P1 | |
| AUTH-08 | login | 카카오로 로그인한 적 있음 | 로그인 화면 다시 열기 | 마지막으로 성공한 제공자 버튼 위에 "최근 로그인" 말풍선(기기 localStorage). 로그아웃해도 남는다. 이벤트 `login_started`(`is_recent_provider`) | E, V | 확정 | P2 | E08 |
| AUTH-09 | 1 | 카카오로 가입한 사람이 같은 이메일로 구글 로그인 | 로그인 | **다른 계정**이 만들어진다(자동 합치기 없음, 제안) | A | 확정/제안 | P1 | |
| AUTH-10 | 2 | 액세스 쿠키 만료(30분), 리프레시 유효 | 보호된 API 호출 | 401 `TOKEN_EXPIRED` → 프론트가 API 2를 한 번 호출 → 원래 요청을 한 번만 다시 보낸다. 사용자는 로그아웃되지 않는다 | A, E | 확정 | P0 | |
| AUTH-11 | 2 | 리프레시 만료(14일) / 삭제됨 / 위조 | 재발급 | 401 `REFRESH_TOKEN_INVALID`, 로컬 캐시를 지우고 로그인 화면으로 | A, E | 확정 | P0 | |
| AUTH-12 | 2 | 리프레시가 DB에 **해시로만** 저장됨 | DB 확인 | `refresh_tokens`에 원문 토큰이 없다 | D | 확정 | P0 | #42 |
| AUTH-13 | 2 | 재발급 요청 2개 동시(탭 두 개) | 동시 요청 | 둘 다 성공하거나 한쪽이 `REFRESH_TOKEN_INVALID`인지 확인하고 사용자가 불필요하게 로그아웃되지 않는지 본다 | A | 확인 | P1 | #42 |
| AUTH-14 | 3 · logout_modal | 로그인 | 설정 → 로그아웃 → 모달 확인 | 204. `refresh_tokens` 행 삭제 + 쿠키 2개 삭제. 이후 같은 리프레시로 재발급하면 401. 이벤트 `logout_confirmed` | A, E, D, V | 확정 | P0 | E16 |
| AUTH-15 | logout_modal | | 모달에서 취소 | 아무것도 바뀌지 않는다 | E | 확정 | P2 | |
| AUTH-16 | 전체 | 미로그인 | `/user/**` 호출 | 401 `UNAUTHORIZED`. 화면은 로그인으로 보내고 로그인 후 **원래 화면으로 돌아온다** | A, E | 확정 | P0 | |
| AUTH-17 | 전체 | 일반 사용자 | `/api/v1/admin/**` 호출 | 403(어드민 기능은 보류라 엔드포인트가 없어도 권한 설정은 ADMIN만 통과) | A | 확정 | P1 | |
| AUTH-18 | 전체 | 다른 사용자의 id가 들어가는 모든 `/user/**` | 호출 | 항상 토큰 소유자 기준(다른 사용자의 것은 404). 경로에 `me`·사용자 id가 없다 | A | 확정 | P0 | AGENTS 규칙 1 |
| AUTH-19 | 전체 | | 쿠키 보안 | 액세스·리프레시 쿠키가 `HttpOnly`·`Secure`·`SameSite=Lax`이고 JS에서 읽을 수 없다. 토큰이 URL·로그·응답 본문에 없다 | A, D | 확정 | P0 | #42 |

### 온보딩 (p1.1 · p1.2 · API 4)

| ID | 화면·API | 전제 | 행동·입력 | 기대 결과 | 방법 | 근거 | 우선 | 연결 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AUTH-20 | onboarding_household | 첫 로그인 | 가구원 수 화면 | 입력 전에는 "다음"이 비활성. **건너뛰기 버튼이 없다**. 숫자만 들어간다. "정확한 인원 수에 따라 레시피의 정확도가 올라갑니다" 안내 | E | 확정 | P0 | #98, E12 |
| AUTH-21 | 4 | | 가구원 수 0 / 51 / 소수 / 음수 / 문자 | 400 `INVALID_INPUT`(1~50만). 경계 1·50은 성공 | A, E | 확정 | P0 | |
| AUTH-22 | onboarding_allergy · 4 | 가구원 수 입력 완료 | 제외 재료를 0개로 "완료" | 0개여도 활성이고 204로 저장된다(`allergenCodes: []`). 두 화면 값이 **마지막에 한 번에** 저장된다 | A, E, D | 확정 | P0 | E13 |
| AUTH-23 | 4 | | 칩 19개 중 여러 개 선택(예: 난류·우유·땅콩) | "3개의 재료 제외", 저장 뒤 `excluded_count` 3. 이벤트 `onboarding_completed`(`excluded_count`)가 한 번 | A, E, V | 확정 | P0 | E13 |
| AUTH-24 | 4 | | 모르는 알레르기 코드, 중복 코드 | 400 `INVALID_INPUT`(코드는 부록 B의 19개만). 중복은 한 번만 저장되거나 거부 | A | 확정/확인 | P0 | |
| AUTH-25 | 4 · 5 | 온보딩 완료 | 로그아웃 후 다시 로그인 | `onboardingCompleted` true라 온보딩을 다시 보여주지 않고 냉장고로 간다 | A, E | 확정 | P0 | |
| AUTH-26 | 4 | 온보딩 중간(가구원 수만 입력)에 앱 종료 | 다시 로그인 | 아무것도 저장되지 않았고 `onboardingCompleted` false라 온보딩을 처음부터 다시 본다 | A, E | 확정 | P1 | |
| AUTH-27 | 4 | 온보딩을 두 번 연달아 저장(더블 탭) | 동시 요청 | 같은 결과로 한 번만 저장된다(중복 행 없음) | A, D | 확인 | P1 | ING-43 유형 |
| AUTH-28 | 4 | 온보딩 완료 계정에 다시 `PUT /user/onboarding` | 호출 | 현재 동작 확인(덮어쓰기인지 거부인지). 정의서에 없다 | A | 확인 | P2 | |

### 설정 (p1.3 · API 5, 6, 7, 8)

| ID | 화면·API | 전제 | 행동·입력 | 기대 결과 | 방법 | 근거 | 우선 | 연결 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AUTH-29 | setting_main · 5 | 가구원 3, 제외 재료 2, 토글 기본값 | 설정 열기 | 계정 카드(로그인 제공자·가구원·알레르기 개수), `plan`, `recipeSupplementEnabled`·`notificationEnabled`, `lastMealPurpose`, `onboardingCompleted`. 한 번의 요청으로 온다 | A, E | 확정 | P0 | #98 |
| AUTH-30 | 5 | 신규 가입 직후 | 조회 | 토글 기본값을 정의서에서 확인한다(재료 보충·알림 받기 기본값) | A | 확인 | P1 | |
| AUTH-31 | setting_household · 6 | | 가구원 수를 5로 변경 | 200(5와 같은 모양). **이후의** 빠른 레시피·식단표 인원 기본값이 5가 된다(이미 만든 레시피는 그대로) | A, E | 확정 | P0 | AI-09, E14 |
| AUTH-32 | 6 | | 바꾼 항목만 보냄(`{notificationEnabled:false}`) | 보내지 않은 항목은 그대로. 빈 본문 `{}`·모든 필드 누락의 동작 확인 | A | 확정/확인 | P1 | |
| AUTH-33 | 6 | | `householdSize` 0·51·소수 | 400 `INVALID_INPUT` | A | 확정 | P1 | |
| AUTH-34 | 6 | 재료 보충 ON → OFF | 토글 | 이후 레시피 생성에서 냉장고 재료만 쓴다(AI-10). 이벤트 `setting_changed`(`setting_key`·`new_value_bool`) | A, E, V | 확정 | P0 | E14 |
| AUTH-35 | 6 | 알림 받기 ON → OFF | 토글 | 다음 오전 9시 작업부터 소비기한 알림을 만들지 않는다(NOTI-08). 식단표 알림은 계속 | A, B | 확정 | P0 | NOTI-08 |
| AUTH-36 | setting_allergy · 8 | | 제외 재료를 칩 5개로 통째로 저장 | 200 `{allergenCodes}`. 이전 값은 사라진다(통째 교체) | A, E | 확정 | P0 | E15 |
| AUTH-37 | 8 | 제외 재료 3개 있음 | 0개로 저장(모두 해제) | 0개도 저장된다(설정에서 모두 해제 가능). 화면의 완료 버튼도 활성. 커스텀 레시피 알레르기 화면은 0개면 비활성(다른 화면) | A, E | 확정 | P0 | |
| AUTH-38 | 7 | | 제외 재료 조회 | 내가 고른 코드만. 다른 사용자의 것은 안 보인다 | A | 확정 | P0 | |
| AUTH-39 | 8 → AI | 제외 재료에 "땅콩" 저장 | 이후 레시피·식단표 생성 | 만든 레시피에 땅콩이 들어가지 않는다(AI-18, MEAL-11) | A | 확정 | P0 | #200 |
| AUTH-40 | 8 | 이벤트 `excluded_ingredients_updated` | 저장 | `excluded_count`(개수)만 있고 **재료 이름·코드가 이벤트에 없다**(개인정보) | V | 확정 | P0 | E15, AGENTS 규칙 3 |
| AUTH-41 | setting_main | | 건강 상태(질환) 입력 | 입력란·API가 없다(건강 정보를 받지 않는다) | E, A | 확정 | P2 | |

### 계정 삭제 (API 9, 10 · #68)

| ID | 화면·API | 전제 | 행동·입력 | 기대 결과 | 방법 | 근거 | 우선 | 연결 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AUTH-42 | account_delete_modal | 로그인 | 설정 → 계정 삭제 | 확인 모달. 카카오·구글은 이메일 인증번호 화면, 네이버는 재인증으로 이어진다 | E | 확정 | P0 | #68 |
| AUTH-43 | account_delete_code · 9 | 카카오·구글 회원(이메일 있음) | 인증번호 받기 | 201 `{expiresAt}`, 이메일로 6자리 번호(5분 유효, 타이머 04:59). 로컬은 로그로 찍는 목. **번호 원문이 아니라 해시만** `account_deletion_requests`에 저장 | A, E, D | 확정 | P0 | #68 |
| AUTH-44 | 9 | 이메일이 없는 계정 | 인증번호 받기 | 422 `ACCOUNT_EMAIL_MISSING`, 아무것도 보내지 않는다(고객 문의 안내) | A | 확정 | P1 | |
| AUTH-45 | 10 | 올바른 번호 | 삭제 | 204, 쿠키 삭제, 로그인 화면으로. 이벤트 `account_deletion_requested`(요청 도착 때, 확인 전) + `account_deleted`(`tenure_days`·`deletion_method`) | A, E, V | 확정 | P0 | E17, E18 |
| AUTH-46 | 10 | 틀린 번호 | 삭제 | 400 `DELETION_CODE_INVALID`(`details.remainingAttempts`), 시도 횟수 +1, 오류 토스트 | A, E | 확정 | P0 | |
| AUTH-47 | 10 | 5분 지난 번호 | 삭제 | 400 `DELETION_CODE_EXPIRED`, "재전송 하기"로 새 번호 | A, E | 확정 | P0 | |
| AUTH-48 | 10 | 같은 번호를 5회 넘게 틀림 | 6번째 | 429 `DELETION_CODE_LOCKED`. 그 번호는 맞게 입력해도 쓸 수 없고 재전송하면 새 행(시도 0부터) | A, E | 확정 | P0 | |
| AUTH-49 | 9 | 재전송 | 두 번 발송 | 새 행이 생기고 이전 번호는 못 쓴다(최신 번호만 유효하다) | A, D | 확정/확인 | P1 | |
| AUTH-50 | 10 | 네이버 회원, 같은 네이버 계정으로 재인증 | 삭제 | 204. `{method:"REAUTH", oauthCode, redirectUri}` | A, E | 확정 | P0 | |
| AUTH-51 | 10 | 네이버 회원, **다른** 네이버 계정으로 재인증 | 삭제 | 403 `REAUTH_ACCOUNT_MISMATCH`, 삭제하지 않고 설정으로 | A, E | 확정 | P0 | |
| AUTH-52 | 10 | 재인증 중 사용자가 취소 / 코드 만료 | 삭제 | 삭제하지 않고 설정으로(취소는 제안), 만료·실패는 401 `OAUTH_FAILED` | A, E | 확정/제안 | P1 | |
| AUTH-53 | 10 | 냉장고 재료·사용 기록·영수증·식단표·장바구니·알림·제외 재료·토큰이 있는 회원 | 삭제 후 DB 확인 | 모두 삭제. `users` 행은 소셜 ID·이메일이 비워진 채 남고 `deleted_at`이 기록된다. **레시피·공유 레시피(익명)는 남는다**. FK 때문에 삭제가 중간에 실패하면 전체 롤백 | A, D | 확정 | P0 | ERD |
| AUTH-54 | 10 | 삭제한 계정으로 다시 가입 | 같은 소셜 계정 로그인 | 새 계정으로 재가입된다(`is_new_user` true, 온보딩부터) | A, E | 확정 | P0 | |
| AUTH-55 | 10 | 삭제한 계정의 이전 쿠키 | 보호된 API 호출 | 401(삭제된 사용자는 통과하지 못한다) | A | 확정/확인 | P0 | |
| AUTH-56 | 10 | 구독 중인 회원 | 삭제 | 구독이 바로 끝나고(ENDED, billingKey 비움) 더 결제하지 않는다. **`subscription_payments`는 파기**한다(#339). 이벤트 `subscription_ended`(`end_reason`) | A, D, V | 확정 | P0 | SUB-15, E88, #349, #350 |
| AUTH-57 | 10 | 소셜 연결 끊기 호출이 실패(목) | 삭제 | 삭제는 진행하고 로그만 남는다(제안) | A | 확정/제안 | P2 | |
| AUTH-58 | 10 | 공유 레시피를 올린 탈퇴 회원 | 주간 랭킹 | 탈퇴한 회원의 글은 보상 대상에서 제외(제안) | A | 제안 | P2 | SHARE |
| AUTH-59 | 10 | 삭제 요청 2개를 동시에 | 동시 요청 | 한 번만 처리되고 두 번째는 401 등으로 끝난다(500·부분 삭제 없음) | A, D | 확인 | P1 | |

### 이벤트·수집 API (V)

| ID | 화면·API | 전제 | 행동·입력 | 기대 결과 | 방법 | 근거 | 우선 | 연결 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AUTH-60 | 전체 | | E08~E18 행동을 각각 한 번 | `login_started`(FE), `login_succeeded`·`login_failed`·`signup_completed`(BE), `onboarding_step_completed`(FE), `onboarding_completed`(BE), `setting_changed`·`excluded_ingredients_updated`·`logout_confirmed`(FE), `account_deletion_requested`·`account_deleted`(BE)가 **각각 한 번만** 적재. 로그인 성공 시 `login_failed`가 같이 나가지 않는다 | V | 확정 | P0 | #196 |
| AUTH-61 | 전체 | | 위 이벤트 속성 | 이메일·이름·소셜 ID가 없다. `user_id`는 내부 숫자, `provider`는 코드 값, `deletion_method`는 enum | V | 확정 | P0 | #196, AGENTS 규칙 3 |
| AUTH-62 | 56 | 로그인 | 클라이언트 이벤트(`POST /user/events`) 전송 | 200 계열로 받고 정의서 밖의 이벤트 이름·잘못된 속성은 거절하거나 무시한다. 다른 사용자 id를 본문에 넣어도 토큰 소유자로 기록된다 | A, D | 확정/확인 | P0 | #116, #172 |

---

## SUB. 구독 (이준호)

화면 코드: `subscription_manage`(p1.3.4 구독 관리: 구독 전·구독 중), 결제 약관 드로워.
API: 57 구독 조회, 58 카드 등록 준비, 59 구독 시작, 60 해지 예약. 정기 작업: 구독 자동 결제(매일 새벽).
**구현 상태**: 2026-11-24 작성 당시에는 구독 코드·테이블이 없어 전부 막힘이었다. 이후 구독 API는 #270, 결제 약관 본문은 #232로 병합됐다. 자동결제 작업·화면 연동 등 최신 실행 가능 범위는 #168·#169를 확인하고, 시나리오 결과는 실제 실행한 뒤 기록한다. 결제는 토스페이먼츠 테스트 API·테스트 키로만 실행하며 실제 청구는 없다(로컬 HTTP mock과 구분, #170). 결제 실패·시간 초과는 목으로 재현한다.
**확정 정책**: 자동 결제 실패 시 즉시 종료·키 삭제·일반 전환, 해지 예약 중 재가입 불가, 구독 종료 후 당월 식단표 사용 횟수 유지, 7일 안 혜택 미사용 시 전액 환불·그 밖의 환불 없음은 결제 약관 기준이다.
**후속 명세**: 해지 화면은 #170(2026-11-27)에서 피그마 사용으로 결정됐고 상세·연동은 #169를 확인한다. 환불 추가 흐름 결정은 #228, 상세 정책·API·이벤트 문서 반영은 #318에서 담당한다. 자동 결제 실패 시 E88 `end_reason` 값은 #229에서 확인한다. 이를 정하기 위해 확정된 종료·환불 정책까지 미결로 취급하지 않는다.
**첫 결제 실패 기록(#340, #351)**: 구독·`subscription_payments` 행 없음. E85 `subscription_payment_failed`(`is_renewal` false)만. FAILED 행을 기대하지 않는다.

| ID | 화면·API | 전제 | 행동·입력 | 기대 결과 | 방법 | 근거 | 우선 | 연결 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SUB-01 | subscription_manage · 57 | 일반 회원 | 설정 → 구독 하기 | `status: NONE`, `price: 9900`, `nextBillingDate`는 **오늘 구독하면 다음 결제일**(오늘 + 1달). 혜택 2개(레시피 하루 4회, 식단표 한 달 4회·7일치), 결제 약관 동의 체크 전에는 구독 버튼 비활성 | A, E | 확정 | P0 | E83 |
| SUB-02 | 57 | 오늘이 1월 31일 | 조회 | 다음 결제일이 말일 규칙을 따른다(2월 28일/29일). 날짜가 없는 달은 말일 | A | 확정 | P1 | |
| SUB-03 | 58 | 약관 동의 후 | 구독 버튼 | 200 `{customerKey, orderName, amount: 9900}`. 프론트가 토스 SDK 카드 등록 창으로 이동. 이벤트 `subscription_checkout_started`. **시크릿 키·billingKey가 응답에 없다** | A, E, V | 확정 | P0 | E83 |
| SUB-04 | 59 | 토스 테스트 카드 등록 완료, `authKey`·`customerKey` | 구독 시작 | 201(`status: ACTIVE`, `nextBillingDate` 오늘 + 1달). billingKey 발급 → 첫 달 9,900원 결제 → `users.plan = PREMIUM`, `subscriptions`·`subscription_payments` 행. 이벤트 `subscription_started`(`payment_amount`). 곧바로 남은 횟수가 구독 한도(레시피 4회)로 바뀐다(AI-02) | A, E, D, V | 확정 | P0 | E84 |
| SUB-05 | 59 | 위조·만료·다른 회원의 `authKey` | 구독 시작 | 400 `BILLING_AUTH_FAILED`, 구독을 만들지 않고 구독 전 화면으로 돌아가 토스트 | A, E | 확정 | P0 | |
| SUB-06 | 59 | 토스가 첫 결제를 거절(한도·정지 카드, 목) | 구독 시작 | 402 `PAYMENT_FAILED`(`details.reason`은 사용자에게 보여도 되는 문구만), 구독 없음, **`subscription_payments` 행 없음**, 이벤트 `subscription_payment_failed`(`is_renewal` false)만 | A, E, D, V | 확정(#340) | P0 | E85, #270, #351 |
| SUB-07 | 59 | 토스 서버 오류·시간 초과(목) | 구독 시작 | 502 `PAYMENT_UPSTREAM_ERROR`. **자동 재시도하지 않는다**(이중 결제 방지). 결과를 모르면 주문 번호로 토스에 결제를 확인한 뒤 정한다. 확인 전에는 구독을 만들지 않는다 | A, D | 확정 | P0 | |
| SUB-08 | 59 | 이미 구독 중 / 해지 예약 기간 | 구독 시작 | 409 `ALREADY_SUBSCRIBED`. **해지 예약 기간에는 다시 구독할 수 없다**(끝난 뒤에는 처음처럼) | A | 확정 | P0 | 결제 약관 3항 |
| SUB-09 | 59 | 구독 시작을 두 번 동시에(더블 클릭) | 동시 요청 | **한 번만 결제**되고 구독이 하나다(`subscriptions` 중복 없음). 두 번째는 409 | A, D | 확인 | P0 | ING-43 유형 |
| SUB-10 | 59 | 결제는 성공했으나 DB 저장 중 서버 오류(목) | 구독 시작 | 결제와 구독이 어긋나지 않는다(결제 성공인데 구독 없음이면 결함, 결제 취소·복구 방안 확인) | A, D | 확인 | P0 | |
| SUB-11 | 60 · subscription_manage | 구독 중 | "구독 해지 하기" | 200 `status: CANCELED`. 바로 끝나지 않고 **다음 결제일 전날까지** 구독 한도가 유지된다. 이벤트 `subscription_canceled`(`subscribed_days`). 해지 화면은 #170의 피그마 사용 결정에 따르며 상세·연동은 #169 확인 | A, E, V | 확정 / 화면 상세 확인 | P0 | E87, #170, #169 |
| SUB-12 | 60 | 이미 해지 예약 | 다시 해지 | 그대로 200(멱등), 이벤트는 한 번만 | A, V | 확정 | P1 | |
| SUB-13 | 60 | 구독이 없음 | 해지 | 404 `SUBSCRIPTION_NOT_FOUND` | A | 확정 | P1 | |
| SUB-14 | 정기 작업 | 다음 결제일이 오늘인 구독 2건 | 새벽 작업 실행 | 같은 카드로 자동 결제하고 성공하면 한 달 늘린다(`nextBillingDate` +1달). 이벤트 `subscription_renewed`(`payment_amount`·`renewal_count`) | B, D, V | 확정 | P0 | E86 |
| SUB-15 | 정기 작업 | 해지 예약된 구독, 다음 결제일 당일 | 작업 실행 | 결제하지 않고 구독을 끝낸다(`status` ENDED, `billing_key` 비움, `plan` BASIC). 이벤트 `subscription_ended`(`end_reason`). 이후 한도가 일반으로(레시피 2회, 식단표 1회·5일). **그달 이미 쓴 식단표 횟수는 그대로 센다** | B, D, V | 확정 | P0 | E88, 결제 약관 3·6항 |
| SUB-16 | 정기 작업 | 자동 결제 실패(카드 한도 등, 목) | 작업 실행 | 구독 즉시 종료(`status` ENDED, `billing_key` 비움, `plan` BASIC). `subscription_payment_failed`(`is_renewal` true) 기록. 다시 구독하면 카드 등록부터 진행. E88 `end_reason` 값은 #229에서 결정 후 검증하며 여기서 새 값을 정하지 않는다 | B, D, V | 종료 정책 확정 / 이벤트 값 미결 | P0 | E85, E88, #229 |
| SUB-17 | 정기 작업 | 같은 날 작업을 두 번 실행 | 작업 실행 | **이중 결제가 없다**(결제일당 한 번) | B, D | 확인 | P0 | |
| SUB-18 | 정기 작업 | 작업이 결제 도중 중단 | 재실행 | 결제가 두 번 되지 않고 상태가 복구된다 | B, D | 확인 | P1 | |
| SUB-19 | 57 | 구독 중 | 조회 | `status: ACTIVE`, `nextBillingDate`·`currentPeriodEnd`, 화면에 "구독 중" 배지·다음 결제일·결제 금액 | A, E | 확정 | P0 | |
| SUB-20 | 58, 59 | 토스 시크릿 키·billingKey | 보안 | 키가 프론트·응답·로그에 없고 billingKey는 **암호화해서** 저장한다(DB에서 원문이 보이지 않는다) | A, D | 확정 | P0 | |
| SUB-21 | 59 | 구독 전환 직후 | 진행 중이던 생성 | 구독 시작 즉시 구독 한도가 적용된다(식단표 4회·7일, 레시피 4회). 이미 쓴 횟수는 유지 | A | 확정 | P1 | AI-02 |
| SUB-22 | subscription_manage | 결제 약관 | "자세히 보기" | 약관 드로워에 `docs/developer/payment-terms.md`와 같은 본문 표시. 월 9,900원(부가세 포함), 7일 안 혜택 미사용 시 전액 환불·그 밖의 환불 없음, 탈퇴 시 즉시 종료·남은 기간 환불 없음, 키 암호화·종료 시 삭제, 가격·혜택 변경 시 다음 결제일 30일 전 안내·미동의 시 해지 가능, 테스트 환경의 시연 결제·실제 청구 없음 확인 | E | 확정 | P2 | #232, 결제 약관 1·4~8항 |
| SUB-23 | 57~60 | 로그인 없이 | 호출 | 401 | A | 확정 | P1 | |
