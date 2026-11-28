# 01. 로그인·온보딩·설정·구독 (이준호)

기준: PRD v4·정책정의서 v4·ERD v4·API 정의서 v4·에러 정의서 v2·이벤트 정의서 v2(모두 피그마 기준, 2026-11-22, 노션). 표는 원본에서 그대로 옮겼고 피그마 노드 근거 칸만 뺐어요.

## 범위

스플래시, 소셜 로그인(카카오·네이버·구글), 온보딩(가구원 수 → 제외 재료), 설정(가구원 수·알레르기·재료 보충·알림 받기·로그아웃·계정 삭제), 토큰 발급·재발급, 구독(토스페이먼츠 카드 자동결제). 이벤트 수집 모듈과 수집 API도 이 담당이에요([08](08_event-tracking.md)).

## 화면

화면 코드는 이벤트의 `screen` 값이에요. "추가될 페이지"는 아직 피그마에 없는 화면이에요.

| 화면 코드 | 피그마 화면 |
| --- | --- |
| `splash` | 스플래쉬 |
| `login` | p1 로그인 |
| `onboarding_household` | p1.1 가구원 수(온보딩) |
| `onboarding_allergy` | p1.2 알레르기 선택(온보딩) |
| `setting_main` | p1.3 설정 |
| `logout_modal` | p1.3 로그아웃 모달 |
| `setting_household` | p1.3.1 가구원 설정 |
| `setting_allergy` | p1.3.2 알레르기 재료 제외 |
| `account_delete_modal` | p1.3 계정 삭제 확인 모달 |
| `account_delete_code` | 계정 삭제 이메일 인증번호 |
| `subscription_manage` | p1.3.4 구독 관리(구독 전·구독 중) |

## 정책

상태: 확정 = 그대로 구현, 제안 = 상수로 분리해 구현하고 PR에 적기, 미결 = 구현 전에 준호님께 질문, 화면 대기 = 규칙은 정해졌고 화면이 추가될 예정.

| 항목 | 규칙 | 상태 |
| --- | --- | --- |
| 진입 흐름 | 스플래시(/) → 로그인 여부를 보고 로그인 또는 냉장고 | 확정 |
| 가입·로그인 | 세 제공자 중 하나로 로그인하고, 처음이면 자동 가입 | 확정 |
| 최근 로그인 | 마지막으로 성공한 제공자 버튼 위에 "최근 로그인" 말풍선. 값은 기기(localStorage)에 저장, 로그아웃해도 남음 | 확정 |
| 약관 동의 | 별도 동의 화면·체크 박스 없음. 소셜 로그인하면 이용 약관에 동의하고 개인정보 처리방침을 확인한 것으로 봄(11/24 약관 결정) | 확정 |
| 로그인 안내 문구 | 로그인 화면에 "로그인하면 이용 약관에 동의하는 것으로 봅니다" 안내 한 줄. 최종 문구는 피그마 기준(11/24 약관 결정) | 확정 |
| 약관 드로워 | 로그인 화면 링크 → 약관 드로워(common-약관드로워)에 이용 약관·개인정보 처리방침·결제 약관 3개. 본문은 docs 약관 md 내용. 3개 공통: 운영자 이준호, 시행일 2026-12-01, 서비스 주소 https://remake.newlecture.com/ (11/24 약관 결정) | 확정 |
| 온보딩 | 첫 로그인 직후 2단계: 가구원 수 → 레시피에서 제외할 재료. 두 값을 마지막에 한 번에 저장 | 확정(피그마) |
| 온보딩 건너뛰기 | 건너뛰기 버튼 없음. 가구원 수는 입력해야 "다음", 알레르기는 0개로 넘어갈 수 있음 | 확정 |
| 온보딩 재노출 | 마치면 다시 보여주지 않음 | 확정 |
| 제외 재료 고르기 | 칩 19개(난류·우유·메밀·땅콩·대두·밀·고등어·갑각류·돼지고기·새우·아황산류·복숭아·오징어·닭고기·쇠고기·굴·게·잣·어패류) 중 여러 개. "N개의 재료 제외" | 확정(피그마) |
| 제외 재료 0개 | 온보딩(p1.2)과 설정(p1.3.2)은 0개여도 완료 버튼 활성(설정에서 알레르기를 모두 해제할 수 있음). 커스텀 레시피 알레르기 화면은 0개면 버튼 비활성 | 확정 |
| 가구원 수 | 1~50명 숫자만. 입력 전 버튼 비활성. "정확한 인원 수에 따라 레시피의 정확도가 올라갑니다" | 확정 |
| 가구원 수 쓰임 | 빠른 레시피 인원은 가구원 수 | 확정 |
| 설정 수정 | 가구원 수·제외 재료·토글 2개는 설정에서 언제든 바꿈. 바꾼 값은 이후 생성에 반영 | 확정 |
| 설정 화면 | 계정 카드(로그인 제공자·가구원·알레르기 개수), 프리미엄 이용권, 가구원 수, 알레르기 관리, 재료 보충 토글, 알림 받기 토글, 로그아웃, 계정 삭제 | 확정(피그마) |
| 건강 상태 | 질환 등 건강 정보는 받지 않음 | 확정 |
| 로그인 실패 | 소셜 인증 실패는 OAUTH_FAILED(401), 공통 토스트. 사용자가 취소한 것은 실패로 세지 않음 | 확정 |
| 계정 연결 | 제공자를 바꿔 로그인하면 다른 계정(자동 합치기 없음) | 제안 |
| 로그아웃 | 확인 모달("로그아웃을 하시면 서비스를 이어서 사용하실 수 없습니다") → 리프레시 토큰 삭제, 쿠키 2개 삭제 | 확정(피그마) |
| 계정 삭제 흐름 | 설정 > 계정 삭제 → 확인 모달 → 카카오·구글은 이메일 인증번호 화면, 네이버는 재인증. 성공하면 쿠키를 지우고 로그인 화면으로 | 확정 |
| 인증번호 | 6자리, 이메일로 발송, 5분 유효(타이머 04:59), "재전송 하기"로 새 번호. 같은 번호를 5회 넘게 틀리면 그 번호는 못 쓰고 다시 받아야 함. 오류(틀림·만료·횟수 초과)는 오류 토스트로 보여줌(문구는 에러 정의서 그대로, 11/22 결정으로 6칸 아래 빨간 글자에서 바꿈) | 확정 |
| 탈퇴용 이메일 | 카카오는 비즈앱으로 로그인 때 이메일 필수 동의, 구글은 기본 제공. 그래도 없으면 ACCOUNT_EMAIL_MISSING(고객 문의 안내) | 확정 |
| 네이버 재인증 | 지금 계정과 다른 네이버 계정으로 인증하면 삭제하지 않고 설정으로(REAUTH_ACCOUNT_MISMATCH) | 확정 |
| 재인증 취소 | 소셜 인증을 취소하면 삭제하지 않고 설정으로 | 제안 |
| 소셜 연결 해제 | 삭제할 때 제공자 연결 끊기를 함께 호출. 실패해도 삭제는 진행하고 로그만 | 제안 |
| 탈퇴 시 데이터 | 제외 재료·토큰·인증번호·냉장고 재료·사용 기록·영수증·식단표·장바구니·알림은 삭제. 회원 행은 소셜 ID·이메일을 비우고 남김(재가입 가능). 레시피는 남고, 작성자가 지우지 않은 공유 레시피만 익명으로 남음(11/24 약관 결정) | 확정 |
| 탈퇴자 보상 | 탈퇴한 회원의 공유 레시피는 보상 대상에서 뺌 | 제안 |
| 구독 관리 화면 | 설정 "구독 하기" → p1.3.4 구독 관리. 구독 전이면 다음 결제일(오늘 + 1달)·결제 금액·혜택 2개·결제 약관 동의·구독 버튼, 구독 중이면 "구독 중" 배지·다음 결제일·결제 금액·"구독 해지 하기" | 확정(피그마) |
| 구독 가격·혜택 | 한 달 9,900원(부가세 포함, 11/24 약관 결정). 레시피 생성 하루 최대 4회, 식단표 생성 한 달 4회·1회 7일치 | 확정(피그마) |
| 가격·혜택 변경 | 다음 결제일 30일 전에 서비스 안에서 알리고, 동의하지 않으면 해지 가능(결제 약관 7항) | 확정 |
| 구독 결제 | 결제 약관 동의(체크 전 버튼 비활성) → 토스페이먼츠 결제 화면(카드 자동결제 등록)으로 이동 → 돌아오면 서버가 첫 달을 결제하고 성공하면 바로 구독. 결제가 실패하거나 토스 화면에서 취소하면 구독 전 화면으로 돌아가 토스트 | 확정 |
| 결제 약관 | "자세히 보기"는 약관 드로워(common-약관드로워)에 결제 약관을 함께 넣음(11/24 약관 결정) | 확정 |
| 자동 연장 | 다음 결제일(시작일 + 1달, 그 날짜가 없는 달은 말일)에 같은 카드로 자동 결제하고 성공하면 한 달 늘림 | 확정 |
| 자동 결제 실패 | 실패하면 구독을 바로 끝냄(ENDED, billing_key 비움, users.plan = BASIC). 다시 구독하려면 카드를 새로 등록(11/24 약관 결정) | 확정 |
| 구독 해지 | "구독 해지 하기"를 누르면 해지 예약. 바로 끝나지 않고 다음 결제일 전날까지 구독 혜택 유지, 다음 결제일에 결제하지 않고 일반 회원으로 | 확정 / #170(2026-11-27)에서 피그마 해지 화면 사용 확정. 화면 상세·연동은 #169 확인 |
| 구독이 끝난 뒤 | 일반 한도로 돌아감. 그달 이미 쓴 식단표 생성 횟수는 그대로 셈. 해지 예약 기간에는 다시 구독할 수 없고(ALREADY_SUBSCRIBED), 끝난 뒤에는 처음처럼 구독 | 확정(결제 약관 3항) |
| 탈퇴와 구독 | 탈퇴하면 구독을 바로 끝내고 더 결제하지 않음. 결제 기록은 지우지 않음. 남은 구독 기간은 환불하지 않음(11/24 약관 결정) | 확정 |
| 청약 철회·환불 | 결제일로부터 7일 안에 **혜택 미사용**(결제 이후 식단표 생성 0회 + BASIC 일일 한도를 넘는 AI 레시피 생성 0회)이면 전액 환불. 그 밖에는 해지만 되고 환불 없음(11/24 약관, #228) | 확정 |
| 환불 처리 | 피그마 환불 페이지에서 요청. 성공 시 토스 결제 취소 → 구독 ENDED·`users.plan` BASIC·billingKey 비움. 결제 기록은 DB에 남기고 앱 결제 내역 화면은 없음. 서버 이벤트는 E89 `subscription_refunded`만(E88과 겹치지 않음). API 경로·메서드는 #343에서 확정 | 확정(경로 문자열만 후속) |
| 결제 키 보관 | 토스 시크릿 키와 자동결제 키(billingKey)는 서버에만 두고 프론트로 보내지 않음. 자동결제 키는 암호화해 저장 | 확정 |
| 시연 결제 | 토스페이먼츠 테스트 키로만 하는 시연 결제(실제 청구 없음)(11/24 약관 결정) | 확정 |

## API

경로의 `/api/v1`은 줄였어요. 요청의 `?`는 선택 값, `|`는 그중 하나예요. 번호는 API 정의서 번호예요(다른 문서도 이 번호로 가리켜요).

| 번호 | 메서드 | 경로 | 설명 | 요청 | 응답 | 관련 테이블 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | POST | `/public/auth/oauth/{provider}` | 소셜 로그인·가입. provider = kakao·naver·google. 액세스·리프레시 토큰은 HttpOnly 쿠키로 설정 | `{ code, redirectUri }` | `200 { onboardingCompleted }` | users, refresh_tokens |
| 2 | POST | `/public/auth/refresh` | 리프레시 쿠키로 액세스 쿠키 재발급 | `본문 없음(리프레시 쿠키)` | `204` | refresh_tokens |
| 3 | POST | `/user/auth/logout` | 리프레시 토큰 행 삭제 + 두 쿠키 삭제 | `본문 없음` | `204` | refresh_tokens |
| 4 | PUT | `/user/onboarding` | 온보딩 저장. p1.2 완료 때 두 화면 값을 한 번에 보냄. 알레르기는 0개여도 됨(11/21 결정) | `{ householdSize(1~50), allergenCodes: [] }` | `204` | users, user_excluded_ingredients |
| 5 | GET | `/user/profile` | 설정 화면 값 한 번에. `role`(USER/ADMIN)로 프론트가 어드민 진입을 판단(11/26) | `-` | `200 { provider, role, plan, householdSize, excludedIngredientCount, recipeSupplementEnabled, notificationEnabled, lastMealPurpose, onboardingCompleted }` | users, user_excluded_ingredients |
| 6 | PATCH | `/user/profile` | 가구원 수·레시피 재료 보충·알림 받기 중 바꾼 것만 보냄 | `{ householdSize?(1~50), recipeSupplementEnabled?, notificationEnabled? }` | `200 (GET /user/profile과 같은 모양)` | users |
| 7 | GET | `/user/excluded-ingredients` | 내가 고른 제외 재료 코드 | `-` | `200 { allergenCodes: [] }` | user_excluded_ingredients |
| 8 | PUT | `/user/excluded-ingredients` | 제외 재료를 통째로 바꿈. 0개여도 저장(11/22 답: 설정에서도 완료 버튼 활성) | `{ allergenCodes: [] (0개 가능) }` | `200 { allergenCodes }` | user_excluded_ingredients |
| 9 | POST | `/user/account/deletion-code` | 카카오·구글 회원의 이메일로 인증번호 6자리 발송(5분 유효). 재전송도 같은 API | `본문 없음` | `201 { expiresAt }` | account_deletion_requests |
| 10 | POST | `/user/account/deletion` | 계정 삭제. 카카오·구글은 인증번호, 네이버는 재인증 code로 본인 확인(11/21 결정). 성공하면 쿠키 삭제 | `카카오·구글 { method: "EMAIL_CODE", code } / 네이버 { method: "REAUTH", oauthCode, redirectUri }` | `204` | users 외 탈퇴 처리 대상 전부 |
| 57 | GET | `/user/subscription` | 내 구독 상태. NONE이면 구독 전 화면, ACTIVE면 구독 중 화면. 구독 전 화면의 다음 결제일은 "오늘 구독하면 다음 결제일"(오늘 + 1달) | `-` | `200 { status: NONE\|ACTIVE\|CANCELED, price, nextBillingDate, currentPeriodEnd? }` | subscriptions |
| 58 | POST | `/user/subscription/billing-auth` | 토스페이먼츠 카드 등록 창을 열 값을 줌. 프론트는 이 값으로 토스 SDK 카드 자동결제 등록(requestBillingAuth)을 불러 토스 결제 화면으로 이동(11/22 답 1). 등록이 끝나면 토스가 프론트 성공 주소로 authKey·customerKey를 붙여 돌려보냄 | `본문 없음` | `200 { customerKey, orderName, amount }` | subscriptions |
| 59 | POST | `/user/subscription` | 구독 시작. 서버가 토스에 authKey로 자동결제 키(billingKey) 발급 요청 → 그 키로 첫 달 9,900원 결제 → 성공하면 구독 시작(users.plan PREMIUM, 다음 결제일 = 오늘 + 1달). 첫 결제가 실패하면 구독을 만들지 않음 | `{ authKey, customerKey }` | `201 (GET /user/subscription과 같은 모양)` | subscriptions, subscription_payments, users |
| 60 | POST | `/user/subscription/cancel` | 해지 예약. 바로 끝나지 않고 다음 결제일 전날까지 구독 혜택 유지, 다음 결제일에 자동 결제하지 않고 일반으로 바뀜(11/22 답 4). 이미 해지 예약이면 그대로 200 | `본문 없음` | `200 (GET /user/subscription과 같은 모양, status CANCELED)` | subscriptions |

구독 API(57~60)는 영역 1이지만 다른 문서의 번호가 바뀌지 않게 맨 뒤 번호를 썼어요.

## 테이블

`users`, `user_excluded_ingredients`, `refresh_tokens`, `account_deletion_requests`, `subscriptions`, `subscription_payments`. 컬럼은 [`../erd.md`](../erd.md)를 보세요.

## 구현 메모

- JWT([#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42) A안): 액세스(30분)·리프레시(14일) 모두 HttpOnly 쿠키(`Secure; SameSite=Lax`). 응답 본문에 토큰을 넣지 않고 프론트는 `Authorization` 헤더를 쓰지 않아요. 리프레시는 DB에 해시로만 저장하고, 로그아웃하면 행을 지워요.
- `/api/v1/admin/**`는 서버 보안 설정에서 ADMIN만 통과시켜요(아니면 403 `FORBIDDEN`). 어드민은 11/26 피그마 '운영자 페이지'로 확정됐고, 프로필 응답(API 5)의 `role`로 프론트가 어드민 진입을 판단해요. 어드민 규칙은 [06 문서](06_notification-admin.md) "10. 어드민"을 보세요.
- 온보딩은 p1.2 완료 때 `PUT /user/onboarding` 한 번으로 가구원 수와 제외 재료를 같이 저장해요. 건너뛰기 버튼은 없어요.
- 제외 재료는 칩 19개 코드(`allergenCodes`)로 주고받아요. 코드 목록은 ERD 부록 B, 화면은 프론트 상수예요.
- 계정 삭제: 카카오·구글은 `POST /user/account/deletion-code`로 이메일 인증번호를 보내고 `POST /user/account/deletion` `{ method: "EMAIL_CODE", code }`로 확인해요. 인증번호는 해시만 `account_deletion_requests`에 저장하고, 재전송하면 새 행을 만들어요(5분 유효, 5회 넘게 틀리면 그 번호는 잠금).
- 네이버는 확인 모달 뒤 재인증(`auth_type=reauthenticate`)을 거쳐 `{ method: "REAUTH", oauthCode, redirectUri }`로 보내요. 지금 계정과 다른 네이버 계정이면 `REAUTH_ACCOUNT_MISMATCH`로 삭제하지 않아요.
- 탈퇴 처리: `users`는 `provider_user_id`·`email`을 비우고 `deleted_at`을 기록해 남겨요. 제외 재료·토큰·인증번호·냉장고 재료·사용 기록·영수증·식단표·장바구니·알림은 지우고, 레시피는 남기고, 작성자가 지우지 않은 공유 레시피만 익명으로 남겨요(11/24 약관 결정). 구독은 바로 끝내요(ENDED, billingKey 비움).
- 인증번호 메일 발송 코드는 포트 뒤 어댑터에 두고, 로컬은 번호를 로그로 찍는 mock을 써요. SMTP·토스 키 같은 새 환경 변수는 `compose.prod.yml`·`prod.env.example`에 추가하고 배포 담당에게 서버 `.env` 등록을 요청해요.
- 구독 흐름: 결제 약관 체크 → `POST /user/subscription/billing-auth` → 프론트가 토스 SDK `requestBillingAuth`로 카드 등록 창 → 토스가 성공 주소로 `authKey`·`customerKey`를 붙여 돌려보냄 → `POST /user/subscription`에서 billingKey 발급과 첫 달 9,900원 결제 → 성공하면 `users.plan = PREMIUM`. 첫 결제가 실패하면 구독을 만들지 않아요.
- 토스 시크릿 키와 billingKey는 서버에만 두고, billingKey는 암호화해 저장해요. 매일 새벽 자동 결제 작업이 다음 결제일이 오늘인 구독을 결제해요(API 요약의 정기 작업 표).
- 환불 흐름(#228, #318): 피그마 환불 페이지 → 환불 요청 API(경로 #343에서 확정) → 혜택 미사용이면 토스 취소·구독 종료·E89. 해지 화면은 #170에서 피그마 사용으로 결정됐고 상세·연동은 #169를 확인해요. 자동 결제 실패·탈퇴 시 환불 없음·시연 결제(테스트 키만)는 11/24 약관 결정으로 확정. 자동 결제 실패 시 E88 `end_reason`은 #229에서 확인해요.
- 설정 값은 본인만 조회해요. 건강(질환) 정보는 받지 않고, 제외 재료 이름은 이벤트에 넣지 않고 개수만 넣어요.

## 지금 코드와 다른 점

- `users.status`(ACTIVE/SUSPENDED)는 없어요. 새 마이그레이션으로 지우고, `ACCOUNT_SUSPENDED`는 코드에 두되 반환하지 않아요(보류).
- `refresh_tokens.revoked_at`은 없어요. 로그아웃하면 행을 지워요.
- 이전 가이드의 `/user/settings`, `/user/account/withdrawal/start`·`verify`, `catalogIds`는 API 5·6, 9·10, `allergenCodes`로 바뀌었어요.
- 구독은 UI만 있는 기능에서 토스페이먼츠 결제 연동으로 바뀌었어요. 현재 시연·배포에서는 토스 테스트 API·테스트 키를 사용하므로 실제 카드 대금은 청구되지 않아요. 로컬 HTTP mock과는 구분해요(#170).

## 에러 코드

`INVALID_INPUT`, `OAUTH_FAILED`, `OAUTH_PROVIDER_UNSUPPORTED`, `REFRESH_TOKEN_INVALID`, `ACCOUNT_EMAIL_MISSING`, `REAUTH_ACCOUNT_MISMATCH`, `DELETION_CODE_INVALID`, `DELETION_CODE_EXPIRED`, `DELETION_CODE_LOCKED`, `ALREADY_SUBSCRIBED`, `BILLING_AUTH_FAILED`, `PAYMENT_FAILED`, `PAYMENT_UPSTREAM_ERROR`, `SUBSCRIPTION_NOT_FOUND`. 공통 코드(`UNAUTHORIZED`, `TOKEN_EXPIRED`, `RATE_LIMITED`, `INTERNAL_ERROR`, `SERVICE_UNAVAILABLE`)는 모든 API에 나올 수 있어요. API별 코드와 화면 표시는 [`docs/agent/spec/errors.md`](../../agent/spec/errors.md)에 있어요.

## 발행 이벤트

발송 FE는 화면 담당 프론트가 API 56으로 보내고, BE는 이 영역 담당이 발행 API가 끝나는 지점에서 `EventTracker`로 기록해요([08](08_event-tracking.md)).

| ID | 이벤트 | 발생 시점 | 속성 | 발송 | 우선순위 | 발행 API |
| --- | --- | --- | --- | --- | --- | --- |
| E08 | `login_started` | 로그인 버튼(카카오·네이버·구글)을 눌렀을 때 | provider, is_recent_provider | FE | P0 | 56 POST /user/events(프론트 전송) |
| E09 | `login_succeeded` | 소셜 인증이 끝나 로그인에 성공했을 때 | provider, is_new_user | BE | P0 | 1 소셜 로그인 |
| E10 | `login_failed` | 소셜 인증이 실패했을 때(사용자 취소 제외) | provider, error_code | BE | P0 | 1 소셜 로그인 |
| E11 | `signup_completed` | 처음 로그인해 계정이 만들어졌을 때(가입) | provider | BE | P0 | 1 소셜 로그인 |
| E12 | `onboarding_step_completed` | 온보딩 한 단계에서 다음·완료를 눌렀을 때 | onboarding_step | FE | P0 | 56 POST /user/events(프론트 전송) |
| E13 | `onboarding_completed` | 온보딩 값이 저장됐을 때 | excluded_count | BE | P0 | 4 온보딩 저장 |
| E14 | `setting_changed` | 설정 값을 바꿔 저장했을 때(가구원 수·레시피 재료 보충·알림 받기) | setting_key, new_value_bool | FE | P1 | 56 POST /user/events(프론트 전송) |
| E15 | `excluded_ingredients_updated` | 설정에서 제외 재료(알레르기)를 저장했을 때 | excluded_count | FE | P1 | 56 POST /user/events(프론트 전송) |
| E16 | `logout_confirmed` | 로그아웃 모달에서 로그아웃을 눌렀을 때 | 공통 속성만 | FE | P1 | 56 POST /user/events(프론트 전송) |
| E17 | `account_deletion_requested` | 계정 삭제 요청이 서버에 도착했을 때(인증번호 확인·재인증 전) | provider, deletion_method | BE | P1 | 10 계정 삭제 |
| E18 | `account_deleted` | 본인 확인이 끝나 계정이 삭제됐을 때 | tenure_days, deletion_method | BE | P1 | 10 계정 삭제 |
| E83 | `subscription_checkout_started` | 구독 전 화면에서 결제 약관 동의 후 하단 버튼을 눌러 토스 결제 화면으로 넘어갈 때 | 공통 속성만 | FE | P1 | 56 POST /user/events(프론트 전송) |
| E84 | `subscription_started` | 첫 결제가 성공해 구독이 시작됐을 때 | payment_amount | BE | P1 | 59 구독 시작 |
| E85 | `subscription_payment_failed` | 첫 결제나 매달 자동 결제가 실패했을 때 | is_renewal, error_code | BE | P1 | 59 구독 시작, 서버 정기 작업 구독 자동 결제(API 정의서 5장) |
| E86 | `subscription_renewed` | 매달 자동 결제가 성공해 구독이 한 달 늘었을 때 | payment_amount, renewal_count | BE | P1 | 서버 정기 작업 구독 자동 결제(API 정의서 5장) |
| E87 | `subscription_canceled` | 구독 중 화면에서 "구독 해지 하기"로 해지 예약했을 때 | subscribed_days | BE | P1 | 60 구독 해지 |
| E88 | `subscription_ended` | 구독이 끝나 일반 회원으로 바뀌었을 때(환불 아님: 해지 기간 종료·탈퇴·자동결제 실패) | end_reason, subscribed_days | BE | P1 | 서버 정기 작업 구독 자동 결제(API 정의서 5장), 10 계정 삭제 |
| E89 | `subscription_refunded` | 7일 안 전액 환불이 성공했을 때. 구독은 이 흐름에서 끝나며 E88은 찍지 않음 | payment_amount, refund_amount, subscribed_days | BE | P1 | 환불 요청 API(경로 #343에서 확정) |

## 체크리스트

- [ ] 3개 제공자 로그인·가입·재로그인, 토큰 재발급, 로그아웃
- [ ] 비로그인 접근 차단과 로그인 후 원래 화면 복귀
- [x] 온보딩 2단계 저장(제외 재료 0개 포함), 설정 수정이 이후 생성에 반영 (API 4~8. 생성 반영은 다른 영역이 `GetUserGenerationPreferencesUseCase`를 쓸 때)
- [x] 계정 삭제(카카오·구글): 인증번호 발송·재전송·5분 만료·5회 잠금, 이메일 없을 때 `ACCOUNT_EMAIL_MISSING` (API 9·10 BE. 메일 발송은 로컬 mock, SMTP는 배포 키 준비 후)
- [x] 계정 삭제(네이버): 재인증, 다른 계정이면 거부 (API 10 BE. FE 화면·취소 흐름은 #98)
- [ ] 구독 시작·첫 결제 실패·해지 예약·다음 결제일 종료
- [ ] admin API 403 테스트
