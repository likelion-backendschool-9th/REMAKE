# 01. 시작하기·소셜 로그인 (이준호)

## 범위

스플래시, 로그인(카카오·네이버·구글), 온보딩(가구원 수 → 알레르기), 설정(가구원 수·알레르기·재료 보충·알림 받기·로그아웃·계정 삭제), 토큰 발급·재발급. 이벤트 수집 모듈과 집계·KPI/퍼널 API도 이 영역(→ [08](08_event-tracking.md)).

## 화면 (피그마 프레임)

p1-로그인, p1-로그인-최근 로그인, 스플래쉬, p1.1-가구원 수(+포커스), p1.2-알러지 선택(+포커스), p1.3-설정, p1.3-설정-로그아웃, p1.3.1-가구원 설정, p1.3.2-알러지 재료 제외, p1.3.2-계정 삭제(+포커스·액티브), 모달

## 화면 흐름

1. 스플래시 → 세션이 없으면 로그인, 있으면 냉장고(`/` 접속 시 냉장고로 이동).
2. 로그인 버튼 3개와 이용 약관·개인정보 처리 방침 링크. 마지막으로 로그인한 제공자 버튼 위에 "최근 로그인" 말풍선. **서버가 아니라 기기 `localStorage` 키 `lastLoginProvider`에 저장**합니다(API 없음).
3. 처음 로그인(가입)이면 온보딩 2단계: 가구원 수 → 알레르기(제외 재료). 각 단계는 건너뛸 수 있고 건너뛴 것도 완료로 칩니다.
4. 로그인 없이 어떤 URL에 들어와도 로그인 화면으로 보내고, 로그인 뒤 원래 주소로 돌려보냅니다. 로그아웃은 확인 모달을 거칩니다.

## API

| 메서드 | 경로 | 비고 |
|---|---|---|
| POST | `/api/v1/public/auth/oauth/{provider}` | 요청 `{ code, redirectUri }` → `{ onboardingCompleted }`. 액세스(30분)·리프레시(14일) 모두 HttpOnly 쿠키(`Secure; SameSite=Lax`) |
| POST | `/api/v1/public/auth/refresh` | 리프레시 쿠키로 액세스 쿠키 재발급(본문 없음 / 204). 무효면 `REFRESH_TOKEN_INVALID` |
| POST | `/api/v1/user/auth/logout` | 리프레시 토큰 폐기 + 두 쿠키 삭제 |
| GET·PUT | `/api/v1/user/profile` | 가구원 수·온보딩 완료·마지막 식단 목적 |
| GET·PATCH | `/api/v1/user/settings` | `recipeSupplementEnabled`, `notificationEnabled`, `provider` |
| GET·PUT | `/api/v1/user/excluded-ingredients` | PUT은 `{ catalogIds }`로 통째 교체 |
| POST | `/api/v1/user/account/deletion-codes` | 이메일로 인증번호 6자리 발송(재전송 겸용) |
| POST | `/api/v1/user/account/deletion-codes/verify` | `{ code }`, 맞으면 계정 삭제 204 |

## 테이블

`users`(설정 값 + `plan` BASIC/PREMIUM + `role`), `user_excluded_ingredients`, `refresh_tokens`(해시 저장, `token_hash` UK), `account_deletion_requests`.

## 구현 규칙

- JWT([#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42) A): 액세스·리프레시 **모두** HttpOnly 쿠키. 액세스 30분, 리프레시 14일(`Secure; SameSite=Lax`). 응답 본문에 `accessToken`을 넣지 않고, 프론트는 `Authorization: Bearer`를 쓰지 않는다. 리프레시는 DB에 해시로만 저장하고 로그아웃·삭제 때 폐기(로테이션 여부는 구현 때 확정).
- `/api/v1/admin/**`는 ADMIN role만 통과. 서버 보안 설정에서 막고 UI는 보조일 뿐입니다.
- 계정 삭제: 인증번호 6자리, 유효 5분, 재전송 30초 간격, 5회 틀리면 잠금(새 번호를 받으면 해제). 삭제하면 `provider_user_id`·`email`을 비우고 행은 유지(공유 레시피와 이벤트 통계가 남도록), 영수증 이미지와 개인 데이터는 삭제. `events.user_id`에는 FK를 걸지 않습니다.
- 소셜이 이메일을 주지 않으면 `ACCOUNT_EMAIL_MISSING`(대체 방법은 미결 #25).
- 알레르기 같은 설정 값은 본인만 조회, 건강 상태(질환) 정보는 수집하지 않습니다. 제외 재료 이름은 이벤트에 넣지 않고 개수만.
- 설정의 프리미엄 구독 화면은 UI만 만들고 기능은 없습니다.

## 분석 API 접근 제어 우선순위 (#58 확정)

- [#58](https://github.com/likelion-backendschool-9th/REMAKE/issues/58) 결정에 따라 8.2 분석 API에 필요한 ADMIN 권한 검사와 비관리자 접근 차단은 P0 선행 작업입니다. 분석 API 구현·시연 전에 실제 보안 필터를 통과하는 ADMIN 허용·일반 사용자 403·비로그인 차단을 검증합니다. 그 외 어드민 기능은 P1을 유지합니다.

## 에러 코드

`OAUTH_FAILED`, `OAUTH_PROVIDER_UNSUPPORTED`, `ACCOUNT_SUSPENDED`, `REFRESH_TOKEN_INVALID`, `CATALOG_ITEM_NOT_FOUND`, `ACCOUNT_EMAIL_MISSING`, `DELETION_CODE_RESEND_TOO_SOON`, `DELETION_CODE_INVALID`(details에 남은 횟수), `DELETION_CODE_EXPIRED`, `DELETION_CODE_LOCKED`, 공통 `UNAUTHORIZED`·`TOKEN_EXPIRED`·`FORBIDDEN`.

## 발행 이벤트

서버(6개): `login_succeeded`, `login_failed`, `signup_completed`, `onboarding_completed`, `account_deletion_requested`, `account_deleted`. 프론트: `login_started`(속성 `provider`, `is_recent_provider`), `onboarding_step_completed`, `setting_changed`, `excluded_ingredients_updated`, `logout_confirmed`, `pwa_install_*`, `push_permission_result` 등.

## 체크리스트

- [ ] 3개 제공자 로그인·가입·재로그인, 토큰 재발급, 로그아웃
- [ ] 비로그인 접근 차단과 로그인 후 원래 주소 복귀
- [ ] 온보딩 건너뛰기, 설정에서 수정 → 이후 AI 생성에 반영
- [ ] 계정 삭제 3단계(인증번호 입력·타이머·재전송·잠금)
- [ ] admin API 403 테스트
