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
5. 계정 삭제([#54](https://github.com/likelion-backendschool-9th/REMAKE/issues/54)): 설정 → 확인 모달(복구 불가 안내, 선택 사유) → 본인 확인 → 성공 시 탈퇴 완료, 취소·실패 시 설정으로 복귀. 본인 확인은 제공자마다 다르다.
   - 카카오·네이버: **가입한 소셜로 재인증**(로그인 화면을 다시 거침).
   - 구글: **구글 이메일로 받은 인증번호 6자리 입력**(5분 타이머·재전송·5회 잠금). 구글은 재로그인을 강제할 수 없어서다(아래 구현 규칙 참고).

## API

| 메서드 | 경로 | 비고 |
|---|---|---|
| POST | `/api/v1/public/auth/oauth/{provider}` | 요청 `{ code, redirectUri, state }`. **로그인** 또는 **카카오·네이버 탈퇴 재인증** 공통 콜백. 서버에 저장한 OAuth `state`의 `purpose`로 분기한다(`withdraw`면 탈퇴, 아니면 로그인). 로그인 성공 시 `{ onboardingCompleted }` + 액세스·리프레시 HttpOnly 쿠키. 탈퇴 성공 시 쿠키 삭제 후 탈퇴 완료로 이어짐. 구글 탈퇴는 이 콜백을 쓰지 않는다 |
| POST | `/api/v1/public/auth/refresh` | 리프레시 쿠키로 액세스 쿠키 재발급(본문 없음 / 204). 무효면 `REFRESH_TOKEN_INVALID` |
| POST | `/api/v1/user/auth/logout` | 리프레시 토큰 폐기 + 두 쿠키 삭제 |
| GET·PUT | `/api/v1/user/profile` | 가구원 수·온보딩 완료·마지막 식단 목적 |
| GET·PATCH | `/api/v1/user/settings` | `recipeSupplementEnabled`, `notificationEnabled`, `provider` |
| GET·PUT | `/api/v1/user/excluded-ingredients` | PUT은 `{ catalogIds }`로 통째 교체 |
| POST | `/api/v1/user/account/withdrawal/start` | 로그인 필수. 회원의 제공자로 분기. **카카오·네이버** → `{ method: "SOCIAL_REAUTH", authorizeUrl }`: OAuth `state`를 서버에 저장(`purpose=withdraw`, userId, provider, 5분 만료). `purpose`는 쿼리로 두지 않는다. **구글** → `{ method: "EMAIL_CODE", maskedEmail, expiresInSeconds: 300 }`: 저장된 구글 이메일로 인증번호 6자리 발송. 다시 부르면 재전송(30초 간격). 이벤트: `account_deletion_requested` |
| POST | `/api/v1/user/account/withdrawal/verify` | 구글 회원 전용. `{ code }`, 맞으면 계정 삭제 204(쿠키 삭제). 다른 제공자는 `FORBIDDEN`. 이벤트: `account_deleted` |
| POST | `/api/v1/public/auth/oauth/{provider}/unlink-webhook` 등 | (권장) 카카오·네이버가 외부에서 연결을 끊을 때 서비스 회원 상태를 맞추는 웹훅 |

## 테이블

`users`(설정 값 + `plan` BASIC/PREMIUM + `role`), `user_excluded_ingredients`, `refresh_tokens`(해시 저장, `token_hash` UK).  
단기 OAuth `state`(purpose·userId·provider·만료)는 서버 저장소(세션·DB 등)에 둔다.  
`account_deletion_requests`는 **구글 회원**의 탈퇴 인증번호에만 쓴다(카카오·네이버는 행을 만들지 않음).

## 구현 규칙

- JWT([#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42) A): 액세스·리프레시 **모두** HttpOnly 쿠키. 액세스 30분, 리프레시 14일(`Secure; SameSite=Lax`). 응답 본문에 `accessToken`을 넣지 않고, 프론트는 `Authorization: Bearer`를 쓰지 않는다. 리프레시는 DB에 해시로만 저장하고 로그아웃·삭제 때 폐기(로테이션 여부는 구현 때 확정).
- `/api/v1/admin/**`는 ADMIN role만 통과. 서버 보안 설정에서 막고 UI는 보조일 뿐입니다.
- 계정 삭제([#54](https://github.com/likelion-backendschool-9th/REMAKE/issues/54)): 제공자마다 본인 확인 방법이 다르다. 회원 식별은 항상 `(provider, provider_user_id)`이고, 이메일은 식별에 쓰지 않는다.
  - **왜 구글만 다른가**: 카카오(`prompt=login`)·네이버(`auth_type=reauthenticate`)는 로그인 화면을 다시 띄우도록 강제할 수 있다. 구글은 공식 문서에 "Google does not support Google Account reauth requests"라고 적혀 있고, `prompt` 값도 `none`·`consent`·`select_account`뿐이다. `auth_time`은 "이 기기에서 구글 계정에 마지막으로 로그인한 시각"이라, 아침에 로그인하고 저녁에 탈퇴하면 계정을 다시 골라도 값이 갱신되지 않는다. 그래서 `auth_time`으로 5분을 검사하면 사용자는 몇 번을 다시 해도 탈퇴할 수 없다. 구글은 계정 삭제 같은 민감한 작업에 **앱이 자체 추가 인증(step-up)을 하라**고 권장하므로, 구글 회원은 구글이 주는 인증된 이메일로 인증번호를 보낸다. 참고: [Security bundle(Sessions)](https://developers.google.com/identity/siwg/security-bundle#sessions), [OIDC 인증 파라미터](https://developers.google.com/identity/openid-connect/openid-connect#authenticationuriparameters)
  - **카카오·네이버 (소셜 재인증)**
    1. `POST /user/account/withdrawal/start`에서 `state` 발급·저장 후 인가 URL 반환. 재인증 파라미터: 카카오 `prompt=login`, 네이버 `auth_type=reauthenticate`.
    2. 기존 `POST /public/auth/oauth/{provider}`에서 `state` 검증(일치·미만료·1회용 삭제) 후 `purpose=withdraw`이면 탈퇴 분기.
    3. 재인증 프로필의 `(provider, id)`가 **현재 로그인 회원과 같아야** 한다. 다르면 `ACCOUNT_PROVIDER_MISMATCH`로 중단.
    4. 재인증으로 받은 액세스 토큰으로 연결 끊기: 카카오 `unlink`, 네이버 `grant_type=delete`. **연결 끊기 실패해도 서비스 탈퇴는 계속**하고 로그만 남긴다.
    5. 사용자가 인가를 취소(`access_denied` 등)하면 탈퇴하지 않고 설정으로 되돌린다. 만료·재사용 `state`는 `OAUTH_STATE_INVALID`.
  - **구글 (이메일 인증번호)**
    1. 구글 로그인은 `openid email` scope를 요청하고, ID 토큰의 `email_verified=true`일 때만 `users.email`에 저장한다. 구글 계정은 항상 이메일이 있으므로 정상이라면 `ACCOUNT_EMAIL_MISSING`은 나오지 않는다(예외 방어용).
    2. `POST /user/account/withdrawal/start`: 6자리 번호를 만들어 해시만 `account_deletion_requests`에 저장하고 메일 발송. 유효 5분, 재전송은 같은 호출로 30초 간격(`DELETION_CODE_RESEND_TOO_SOON`). 응답에 가린 이메일(`ab***@gmail.com`)을 주어 화면에 "이 주소로 보냈어요"를 보여 준다.
    3. `POST /user/account/withdrawal/verify` `{ code }`: 가장 최근 행으로만 확인. 틀리면 `DELETION_CODE_INVALID`(details에 남은 횟수), 5분 지나면 `DELETION_CODE_EXPIRED`, 5회 틀리면 `DELETION_CODE_LOCKED`(새 번호를 받으면 해제).
    4. 구글 `revoke`는 하지 않는다. 서버는 구글 토큰을 보관하지 않고, 재인증 단계가 없어 새 토큰도 없다. `provider_user_id`를 비우므로 같은 구글 계정으로 다시 로그인하면 새 회원이 된다. 앱 접근 권한은 사용자가 구글 계정 설정(보안 → 서드파티 연결)에서 지울 수 있다고 탈퇴 완료 화면에 안내한다.
    5. 메일 발송 코드는 `user/adapter/out/mail`(포트 뒤)에 둔다. 로컬은 메일 대신 로그로 번호를 찍는 mock을 쓰고, SMTP 등 새 환경 변수는 구현할 때 `compose.prod.yml`·`prod.env.example`에 추가하고 배포 담당에게 서버 `.env` 등록을 요청한다.
    6. 구글 콘솔 추가 설정(Session age claims)이나 앱 검증은 **필요 없다**. 로컬·개발에서도 구글 탈퇴를 그대로 테스트할 수 있다.
  - **공통 내부 처리**: 기존 ERD대로 **소프트 삭제**(`deleted_at` 설정, `provider_user_id`·`email` 비움, 행 유지). 영수증 이미지 등 개인 데이터 삭제, 리프레시·세션 전부 폐기. 이벤트: `account_deleted`.
- (권장) 카카오 연결 해제 웹훅 등으로 앱 밖에서 연결이 끊긴 경우에도 회원 상태를 맞춘다.
- 알레르기 같은 설정 값은 본인만 조회, 건강 상태(질환) 정보는 수집하지 않습니다. 제외 재료 이름은 이벤트에 넣지 않고 개수만.
- 설정의 프리미엄 구독 화면은 UI만 만들고 기능은 없습니다.

## 에러 코드

`OAUTH_FAILED`, `OAUTH_PROVIDER_UNSUPPORTED`, `OAUTH_STATE_INVALID`, `ACCOUNT_PROVIDER_MISMATCH`, `ACCOUNT_SUSPENDED`, `REFRESH_TOKEN_INVALID`, `CATALOG_ITEM_NOT_FOUND`, 공통 `UNAUTHORIZED`·`TOKEN_EXPIRED`·`FORBIDDEN`.  
구글 탈퇴 인증번호: `ACCOUNT_EMAIL_MISSING`, `DELETION_CODE_RESEND_TOO_SOON`, `DELETION_CODE_INVALID`(details에 남은 횟수), `DELETION_CODE_EXPIRED`, `DELETION_CODE_LOCKED`.

## 발행 이벤트

서버(6개): `login_succeeded`, `login_failed`, `signup_completed`, `onboarding_completed`, `account_deletion_requested`(withdrawal/start, 구글 재전송이면 `is_resend=true`), `account_deleted`(카카오·네이버 재인증 성공 또는 구글 인증번호 확인 후 탈퇴 완료). 프론트: `login_started`(속성 `provider`, `is_recent_provider`), `onboarding_step_completed`, `setting_changed`, `excluded_ingredients_updated`, `logout_confirmed`, `pwa_install_*`, `push_permission_result` 등.

## 체크리스트

- [ ] 3개 제공자 로그인·가입·재로그인, 토큰 재발급, 로그아웃
- [ ] 비로그인 접근 차단과 로그인 후 원래 주소 복귀
- [ ] 온보딩 건너뛰기, 설정에서 수정 → 이후 AI 생성에 반영
- [ ] 계정 삭제(카카오·네이버): 확인 모달 → 소셜 재인증 → 연결 끊기 → 소프트 삭제. 다른 계정 재인증 거부, 취소 시 설정 복귀, state 재사용·만료 거부
- [ ] 계정 삭제(구글): 확인 모달 → 인증번호 발송 → 입력 → 소프트 삭제. 30초 재전송 제한, 5분 만료, 5회 잠금과 새 번호로 해제, 카카오·네이버 회원의 verify 호출 거부
- [ ] admin API 403 테스트
