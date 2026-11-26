# REMAKE 프론트엔드 설계 (Blueprint, 한글 번역)

> [`frontend/blueprint.md`](../../frontend/blueprint.md)의 한글 번역본입니다. AI 에이전트는 영문 원본을 읽고, 개발자는 이 문서를 봅니다. 두 문서는 항상 같은 내용을 유지합니다.

냉장고 속 식재료를 관리하고, AI 맞춤 레시피와 소비기한 알림으로 식재료 낭비를 줄이는 서비스의 프론트엔드 설계 문서입니다.
모바일 우선 PWA로 만들고, PWABuilder(TWA)로 안드로이드 앱으로 패키징합니다.

> §4 라우트는 WBS v2의 프론트 URL 표(2026-11-22, 피그마 기준)를 따릅니다. 기획이나 디자인이 바뀌면 이 문서를 먼저 고친 뒤 코드를 바꿉니다.

## 1. 역할 분담

- **프론트(Next.js)**: 화면과 사용자 상호작용만 담당합니다. 비즈니스 로직과 데이터 저장은 하지 않습니다.
- **백엔드(Spring Boot)**: 인증, 데이터, OCR·AI 처리, 앱 안 알림을 담당합니다.
- 프론트는 Spring REST API만 호출합니다. Next의 Route Handler·Server Action으로 백엔드 기능을 대신 만들지 않습니다.
  예외: 서비스 워커 서빙 라우트(`app/serwist/[path]`).
- **`/api/`는 백엔드 경로입니다.** 운영에서는 프론트와 백엔드가 한 도메인을 같이 쓰고, 프록시가 `/api/`로 시작하는 요청을 경로 그대로 Spring으로 보냅니다. 나머지는 모두 Next.js로 갑니다([`deployment.md`](./deployment.md) 참고).
    - 주소가 `/api/`로 시작하는 라우트(페이지, Route Handler 등)는 만들지 않습니다. 운영에서 그 주소로 온 요청은 Next에 닿지 않습니다. 폴더가 아니라 URL로 판단합니다. 라우트 그룹은 URL에 나타나지 않아서 `app/(group)/api/...`도 주소는 `/api/...`가 됩니다.
    - 나중에 만들 헬스체크(`/healthz`)처럼 프론트 전용 주소는 `/api/` 밖에 둡니다.
- **프론트는 보여 주고, 판단은 백엔드가 합니다.** 계산·검증·권한·한도(레시피 생성 횟수, 소비기한 상태, 식비 집계 등)는 백엔드가 정합니다. 프론트는 받은 값을 보여 주고, 입력 형식(필수·길이)만 검증합니다.
- **서버 컴포넌트가 기본입니다.** 페이지와 레이아웃은 서버 컴포넌트로 둡니다. `"use client"`는 상호작용이나 서버 데이터(TanStack Query, §5)가 필요한 가장 작은 말단 컴포넌트에만 붙입니다.

## 2. 기술 스택

| 영역            | 기술                                                                             | 쓰는 이유                                                                           |
| --------------- | -------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| 프레임워크      | Next.js (App Router) + TypeScript                                                | 라우트별 화면 분리, 화면만 담당                                                     |
| 패키지 매니저   | pnpm                                                                             | 빠른 설치, lockfile로 7명 의존성 버전 통일                                          |
| 스타일          | CSS Modules + CSS 변수                                                           | 별도 라이브러리 없이 컴포넌트 단위 스타일, 디자인 토큰 공유                         |
| 서버 데이터     | TanStack Query                                                                   | 서버 데이터 캐싱·다시 불러오기                                                      |
| 클라이언트 상태 | Zustand                                                                          | 로그인 정보 등 최소한만                                                             |
| URL 상태        | nuqs                                                                             | 필터·탭·정렬·월 선택을 URL에 저장                                                   |
| API 연동        | openapi-typescript + openapi-fetch                                               | Spring OpenAPI에서 타입 자동 생성, 타입 안전한 호출                                 |
| 폼              | React Hook Form + Zod                                                            | 온보딩, 커스텀 레시피 입력, 재료 수정 검증                                          |
| 날짜            | dayjs                                                                            | 날짜 표시, 식단표 달력                                                              |
| 아이콘          | @solar-icons/react (Solar)                                                       | 트리 셰이킹되는 React 컴포넌트, 오프라인에서도 동작                                 |
| 폰트            | Pretendard(`pretendard` 패키지, dynamic subset) + GmarketSans(`next/font/local`) | Pretendard: 한글 UI 폰트, 화면에 나온 글자 묶음만 받음. GmarketSans: 로고 글자 전용 |
| PWA             | Serwist                                                                          | 서비스 워커, 오프라인 화면. 알림은 앱 안에서만(기기 푸시 없음)                      |
| 스토어 배포     | PWABuilder (TWA)                                                                 | 안드로이드 앱 패키지 생성                                                           |
| 웹 배포         | `output: "standalone"` + nginx                                                   | Node 서버를 nginx 뒤에서 실행                                                       |
| 코드 품질       | ESLint + Prettier, Husky + lint-staged                                           | 7명 코드 스타일 통일, 커밋 전 자동 검사                                             |
| API 목업        | MSW                                                                              | 백엔드 완성 전 더미 응답으로 화면 개발                                              |

## 3. 폴더 구조

```
frontend/
├─ app/                  # 라우트. 각 라우트 폴더에 그 라우트만 쓰는 코드를 함께 둔다 (3.1 참고)
│  ├─ layout.tsx         # 루트 레이아웃, globals.css
│  ├─ fonts/             # GmarketSans 파일(woff2) + fonts.ts (next/font/local) + pretendard.css(생성 파일)
│  ├─ page.tsx           # 스플래시 (/)
│  ├─ public/            # /public/... 라우트 (로그인 전, §4)
│  ├─ user/              # /user/... 라우트 (로그인 후, §4). 예: user/fridge/ingredients/ — 3.1 참고
│  ├─ admin/             # /admin/... 라우트 (운영자, ADMIN만, §4 "어드민 화면")
│  ├─ sw.ts              # 서비스 워커 (캐싱)
│  ├─ serwist/[path]/    # 서비스 워커 서빙 라우트
│  ├─ ~offline/          # 오프라인 대체 화면
│  ├─ dev/               # 화면 컨테이너의 개발 전용 상태 미리보기 (3.1의 7번, 운영에서는 404)
│  └─ manifest.ts        # PWA 매니페스트
├─ components/           # 여러 라우트에서 재사용하는 UI, 도메인을 모름 (Button, Input, BottomSheet ...)
├─ hooks/                # 여러 라우트에서 재사용하는 React 훅, 도메인을 모름 (useDebounce ...)
├─ lib/
│  ├─ api/               # client.ts(openapi-fetch), schema.d.ts(자동 생성)
│  ├─ queries/           # 여러 라우트가 함께 쓰는 queryKey + Query 훅, 도메인별 파일 하나
│  ├─ events/            # (예정) 분석 이벤트 트래커와 화면 코드 상수 (§11 참고)
│  ├─ query-client.ts
│  ├─ dayjs.ts           # ko 로케일 + 플러그인 설정된 dayjs
│  └─ env.ts             # 환경 변수는 여기서만 읽는다
├─ mocks/                # MSW 핸들러
├─ providers/            # 전역 Provider (Serwist, TanStack Query, MSW)
├─ stores/               # Zustand 스토어
└─ public/               # 정적 파일, mockServiceWorker.js, (예정) .well-known/assetlinks.json, icons/
```

### 3.1 코로케이션 규칙

코드는 그 코드를 쓰는 페이지나 컴포넌트 옆에 둡니다. 여러 라우트에서 재사용하는 코드만 공용 폴더(`components/`, `hooks/`, `lib/`)로 올립니다.

```
app/user/fridge/ingredients/
├─ page.tsx                     # 얇게: _components를 조합하고 _lib/queries를 호출
├─ page.module.css
├─ _components/                 # 이 라우트(와 하위 라우트)에서만 쓰는 컴포넌트
│  └─ IngredientCard/
│     ├─ IngredientCard.tsx
│     ├─ IngredientCard.module.css
│     └─ useIngredientCard.ts   # 이 컴포넌트만 쓰는 훅
├─ _hooks/                      # 이 라우트에서만 쓰는 훅
├─ _lib/
│  ├─ queries.ts                # 이 라우트의 queryKey + useQuery/useMutation
│  ├─ schema.ts                 # Zod 스키마
│  └─ utils.ts                  # 순수 함수 (예: 필터 상태 → 쿼리 파라미터 변환)
└─ [id]/
   └─ edit/
      └─ page.tsx
```

1. **private 폴더.** `_`로 시작하는 폴더는 Next.js private 폴더라서 라우트가 되지 않습니다. 라우트의 화면 외 코드는 모두 `_components/`, `_hooks/`, `_lib/`에 둡니다. 이 안에 `page.tsx`나 `route.ts`를 두지 않습니다.
2. **라우트 단위 고립.** 라우트는 자기 폴더와 상위 라우트 폴더의 코로케이션 코드만 import할 수 있습니다 (예: `app/user/fridge/ingredients/[id]/edit`는 `app/user/fridge/ingredients/_components`를 쓸 수 있음). 형제 라우트나 관련 없는 라우트의 코드는 import하지 않습니다.
3. **컴포넌트 단위 고립.** 컴포넌트마다 폴더 하나를 두고 `.tsx`, `.module.css`, 그 컴포넌트만 쓰는 훅·유틸을 함께 둡니다. 다른 컴포넌트의 CSS Module이나 내부 파일을 import하지 않습니다.
4. **공유되면 올리기.** 관련 없는 두 라우트가 같은 코드를 필요로 하면 위로 올립니다 — UI 컴포넌트 → `components/<이름>/`, React 훅 → `hooks/`, queryKey와 Query 훅 → `lib/queries/<도메인>.ts`, 그 밖의 함수 → `lib/`. 공용 코드는 특정 페이지를 알면 안 됩니다.
5. **import 경로.** 코로케이션 코드는 상대 경로(`./_components/...`, `../_lib/...`)로, 공용 코드는 `@/`(`@/components/Button`)로 import합니다. `@/app/...`에서는 import하지 않습니다 (ESLint로 막음).
6. **화면 컨테이너.** 화면마다 최상위 컴포넌트 하나를 `<화면>Container`라는 이름으로 그 라우트의 `_components/`에 둡니다 (예: `LoginContainer`). `page.tsx`는 `metadata`를 내보내고 그 컨테이너만 그립니다. `...Screen` 이름은 쓰지 않습니다. 손으로 만들기 번거로운 상태(저장된 값, 열린 드로워)는 기본값이 있는 컨테이너 props로 받아서 개발용 미리보기(7번)가 그릴 수 있게 합니다. 실제 라우트는 기본값을 씁니다.
7. **개발용 미리보기(`app/dev/`).** `/dev/p<N>`은 피그마 영역 하나의 상태 목록이고, `/dev/p<N>/<상태>`는 화면 컨테이너에 props를 넘겨 상태 하나를 그립니다. `app/dev/` 아래 모든 페이지는 `env.isDev`가 아니면 `notFound()`를 불러서 운영에서는 404입니다. 이 폴더만 2번 규칙의 예외로, 라우트의 컨테이너(와 그 `_lib` 타입·예시 값)를 상대 경로로 import할 수 있습니다. 다른 곳에서 `app/dev/`를 import하지 않습니다. 컨테이너 기본값은 API를 붙이기 전 실제 라우트가 보일 값으로, 빈 값이거나 서버 값이면 불러오는 중 상태입니다. 예시 값(피그마 그대로의 값)은 `app/dev/`에서만 넘기고 컨테이너 기본값으로 쓰지 않습니다. `dev`는 개발 서버에 배포되기 때문입니다. 운영에서는 `generateStaticParams`가 자리표시 값 하나만 돌려줘서(Cache Components에서는 빈 배열이 빌드 오류) 미리보기 경로를 만들지 않습니다.

공용 컴포넌트 구성:

```
components/Button/
├─ Button.tsx
├─ Button.module.css
└─ index.ts          # export { Button } from "./Button";
```

### 3.2 네이밍

위 폴더 구조가 우선입니다. 컴포넌트 폴더·파일은 PascalCase, 훅 파일은 훅 이름 그대로 씁니다.

| 대상                                     | 규칙                                 | 예                                  |
| ---------------------------------------- | ------------------------------------ | ----------------------------------- |
| 컴포넌트 폴더·파일 (CSS Module 포함)     | PascalCase                           | `IngredientCard/IngredientCard.tsx` |
| 훅 파일                                  | 훅 이름                              | `useIngredientCard.ts`              |
| 그 밖의 파일·폴더, 라우트 폴더           | kebab-case                           | `query-client.ts`, `meal-plan/`     |
| 동적 세그먼트·슬롯                       | camelCase                            | `[userId]`, `@sidePanel`            |
| 컴포넌트·페이지·타입·Context·클래스·enum | PascalCase                           | `UserProfilePage`, `AuthContext`    |
| 함수·변수                                | camelCase                            | `formatDate`                        |
| 훅                                       | `use` + camelCase                    | `useAuth`                           |
| 상수                                     | UPPER_SNAKE_CASE                     | `MAX_RECIPE_PER_DAY`                |
| 약어                                     | 단어 하나로 취급                     | `ApiClient`, `api-client.ts`        |
| 이벤트 핸들러                            | `handle` + 동작, props는 `on` + 동작 | `handleSubmit`, `onSubmit`          |
| boolean                                  | `is` / `has` / `can` 접두사          | `isOpen`, `hasError`                |

## 4. 화면 / 라우트

출처: WBS v2 3장 "프론트 URL"(2026-11-22). 형식은 `/{역할자}/{기능}/{자원}`입니다. 역할자는 `public`(로그인 전), `user`(로그인 후), `admin`(운영자, ADMIN role만. 11/26 추가, 아래 "어드민 화면") 셋입니다. 예외는 스플래시 `/` 하나뿐입니다.

- 시트·모달·드로워·로딩·실패는 따로 주소가 없고, 그 화면 안의 상태입니다.
- 로그인하지 않았으면 모든 `/user/...` 라우트에서 `/public/auth/login`으로 보냅니다. 로그인했으면 `/`와 로그인 화면에서 `/user/fridge/ingredients`(인덱스)로 보냅니다.
- 온보딩은 첫 로그인 직후 필수입니다. 건너뛰기 버튼은 없습니다. 가구원 수는 입력해야 넘어가고, 알레르기는 0개여도 됩니다.

| 라우트                                       | 화면(피그마)                      | 비고                                                                     |
| -------------------------------------------- | --------------------------------- | ------------------------------------------------------------------------ |
| `/`                                          | 스플래시                          | 로그인 여부를 보고 로그인 또는 냉장고로                                  |
| `/public/auth/login`                         | p1 로그인                         | 카카오·네이버·구글, "최근 로그인" 말풍선, 약관 드로워는 이 화면 안       |
| `/public/auth/callback/[provider]`           | (화면 없음)                       | 소셜 인증 되돌아오는 주소. API 1(로그인) 또는 API 10(네이버 재인증) 호출 |
| `/user/onboarding/household`                 | p1.1 가구원 수                    | 1~50, 숫자만. 입력 전 "다음" 비활성                                      |
| `/user/onboarding/allergies`                 | p1.2 알레르기 선택                | 칩 19개, 0개 가능. 온보딩 값 두 개를 한 번에 저장(API 4)                 |
| `/user/setting/profile`                      | p1.3 설정                         | 로그아웃 모달, 계정 삭제 모달이 이 화면 안                               |
| `/user/setting/household`                    | p1.3.1 가구원 설정                |                                                                          |
| `/user/setting/excluded-ingredients`         | p1.3.2 알레르기 재료 제외         | 0개 가능                                                                 |
| `/user/setting/subscription`                 | p1.3.4 구독 관리(구독 전·구독 중) | 토스페이먼츠 카드 등록                                                   |
| `/user/setting/subscription/billing-success` | (화면 없음)                       | 토스가 돌려보내는 주소. 구독 시작(API 59) 후 구독 관리로                 |
| `/user/setting/subscription/billing-fail`    | (화면 없음)                       | 토스 실패·취소 때 돌아오는 주소. 토스트 후 구독 관리로                   |
| `/user/setting/account-deletion`             | p1.3.3 이메일 인증번호            | 카카오·구글만. 네이버는 재인증                                           |
| `/user/fridge/ingredients`                   | p2 냉장고(인덱스)                 | 상태 카드, 위치·종류 시트, 사용 여부 시트                                |
| `/user/fridge/ingredients/new`               | p2.2 재료 추가                    |                                                                          |
| `/user/fridge/ingredients/[id]/edit`         | p2.1 재료 수정                    | 값 변경 시트, 삭제 아이콘                                                |
| `/user/receipt/receipts`                     | p3 영수증                         | 월 선택, 주차 묶음                                                       |
| `/user/receipt/receipts/[id]`                | p3.2 영수증 상세                  | 삭제 모달이 이 화면 안                                                   |
| `/user/receipt/scan`                         | p3.1 카메라·이미지 업로드         | 파일 입력으로 기본 카메라 앱, 앱 안 카메라 없음(domain-rules 3)          |
| `/user/receipt/scan/result`                  | p3.3 OCR 결과                     | 기다리는 동안 스켈레톤. OCR 실패는 이 주소 안의 상태                     |
| `/user/receipt/scan/items/new`               | p3.3.2 OCR 재료 추가              |                                                                          |
| `/user/receipt/scan/items/[index]/edit`      | p3.3.1 OCR 재료 수정              | 고친 값은 등록할 때까지 화면 상태로만 둠                                 |
| `/user/receipt/patterns`                     | p3.4 나의 소비                    |                                                                          |
| `/user/receipt/patterns/discarded`           | p3.4.1 이번 달 버린 식재료        | p3.4 "N월 버린 식비"를 누르면 들어옴. `?month=2026-11`                   |
| `/user/recipe/saved-recipes`                 | p4.1 레시피(메인)                 | 금주 TOP 3, 저장 목록, 빠른 생성 모달이 이 화면 안                       |
| `/user/recipe/custom`                        | p4.1.2 커스텀 레시피              | 선택식만, 자유 입력 없음                                                 |
| `/user/recipe/results/[id]`                  | p4.1.3 생성 결과                  | 로딩·실패·횟수 소진은 이 주소 안의 상태                                  |
| `/user/recipe/recipes/[id]`                  | p4.1.1 레시피 상세                |                                                                          |
| `/user/recipe/recipes/[id]/cook`             | p4.1.1.2 요리 완료 재료 차감      |                                                                          |
| `/user/recipe/shared-recipes`                | p4.1.4 금주의 인기 레시피         | 랭킹, 검색                                                               |
| `/user/recipe/shared-recipes/new`            | p4.1.1.1 레시피 공유 작성         |                                                                          |
| `/user/recipe/shared-recipes/[id]`           | p4.1.4.1 공유 레시피 상세         | 신고 시트가 이 화면 안                                                   |
| `/user/meal-plan/calendar`                   | p5 식단표                         | 빈 화면, 월 달력, 날짜 시트, ⋮ 시트(삭제·교체)                           |
| `/user/meal-plan/meal-plans/new`             | p5 식단표 생성 설정               | 겹침 모달, 횟수 소진이 이 화면 안. 생성은 비동기(§5)                     |
| `/user/notification/notifications`           | p6 알림                           |                                                                          |
| `/user/cart/cart-items`                      | p7 장바구니                       | 수량 시트, 삭제 모드                                                     |

아직 위치가 정해지지 않은 화면(화면 대기): 이번 달 버린 식재료 금액, 레시피 추천 여부.

### 어드민 화면(11/26 운영자 페이지)

출처: 피그마 '운영자 페이지'(노드 2005:4)와 11/26 기획 WBS. 데스크톱 1440. 어드민 프론트 기능 이름은 사이드 메뉴 묶음 `monitoring`·`operation`입니다.

- 모달·토스트·빈 화면·오류는 그 화면 안 상태입니다. a3 탭은 주소에 넣어 대시보드 이상 징후 "기능별 지표 보기"에서 바로 열 수 있게 했습니다.
- 모든 `/admin/...` 라우트는 공통 레이아웃(사이드 메뉴·처리 대기 신고 숫자(API 65)·운영자 계정·"서비스로 돌아가기"(`/`로))을 씁니다.
- 프론트는 `GET /user/profile`(API 5)의 `role`로 먼저 판단하고, ADMIN이 아니면 서버가 403 `FORBIDDEN`을 줍니다. 어느 쪽이든 a0 권한 없음 화면을 보여 줍니다. a0 로딩·불러오기 실패는 각 어드민 화면 안 상태입니다.

| 라우트                                      | 화면(피그마)           | 비고                                                                                   |
| ------------------------------------------- | ---------------------- | -------------------------------------------------------------------------------------- |
| `/admin`                                    | (화면 없음)            | `/admin/monitoring/dashboard`로 이동                                                   |
| `/admin/monitoring/dashboard`               | a1 대시보드            | 이상 징후·데이터 없음이 이 화면 안. API 61                                             |
| `/admin/monitoring/funnels`                 | a2 퍼널                | API 62                                                                                 |
| `/admin/monitoring/metrics/[tab]`           | a3 기능별 지표(탭 8개) | `tab` = registration·consumption·ai·meal-plan-cart·shared·retention·cost·error. API 63 |
| `/admin/monitoring/events`                  | a6 이벤트 로그         | 검색 결과 없음이 이 화면 안. API 64                                                    |
| `/admin/operation/reports`                  | a4 신고 관리           | 처리 대기·처리 완료·빈 화면이 이 화면 안. API 65                                       |
| `/admin/operation/reports/[sharedRecipeId]` | a4.1 신고 상세         | 확인 모달 3개·처리 끝남·토스트·이미 처리됨·4차 안내가 이 화면 안. API 66, 67           |
| `/admin/operation/users`                    | a5 사용자 관리         | 상세 패널·횟수 조정 모달·상태 6개가 이 화면 안. API 68, 69, 70                         |
| `/admin/operation/weekly-rewards`           | a7 랭킹·보상           | 보상 없음·3개 미만이 이 화면 안. API 71                                                |

## 5. 데이터 흐름

```
Spring API ──(OpenAPI 문서)──> pnpm gen:api ──> lib/api/schema.d.ts
                                                     │
화면(page.tsx) ─> _components ─> _lib/queries.ts 또는 @/lib/queries (useQuery) ─> lib/api/client.ts ─> Spring API
                                                     │
                              개발 중 NEXT_PUBLIC_API_MOCKING=true ─> MSW 더미 응답
```

- **API 주소**: 운영 이미지는 `NEXT_PUBLIC_API_BASE_URL`을 빈 값으로 빌드합니다. 그래서 `lib/api/client.ts`가 같은 도메인의 상대 경로(`/api/...`)로 호출하고, 프록시가 Spring으로 넘깁니다. 로그인 쿠키도 CORS 설정 없이 동작합니다. 로컬 개발에서는 기본값 `http://localhost:8080`을 씁니다(출처가 달라서 백엔드에 CORS 설정이 필요합니다).
    - 상대 경로는 브라우저에서만 동작합니다. API는 Client Component(TanStack Query)에서 호출합니다. Server Component에서 백엔드를 불러야 하면 먼저 물어봅니다. 실행할 때 넘기는 서버용 주소가 따로 필요합니다.
- **서버 데이터**는 전부 TanStack Query로 관리합니다. Zustand에 서버 데이터를 복사하지 않습니다.
- **OCR과 AI 레시피 생성**은 각각 동기 요청 한 번이고, 응답에 결과가 담깁니다. 기다리는 동안 로딩 화면을 보여 줍니다(시간 제한: OCR 30초, AI 레시피 60초). 작업 ID, polling, 스트리밍은 없습니다.
- **식단표 생성**은 비동기입니다. API 38이 202로 바로 응답하면 냉장고(인덱스) 화면으로 이동해 토스트 "식단을 완성하면 알림을 보내드려요"를 띄우고, 결과는 앱 안 알림으로 옵니다. 생성 중 화면은 없습니다.
- **queryKey**는 Query 훅과 같은 곳에 둡니다. 라우트의 `_lib/queries.ts`, 공유되면 `lib/queries/<도메인>.ts`. 예: `fridgeKeys.all`, `fridgeKeys.detail(id)`
- **API 클라이언트**: 모든 호출은 `lib/api/client.ts`를 거칩니다. 쿠키를 함께 보냅니다(`credentials: "include"`). 타임아웃은 `AbortSignal.timeout`으로 겁니다. 개발 중에는 넉넉하게 기본 30초, OCR 60초, AI 레시피 생성 120초로 둡니다. 프론트 타임아웃은 백엔드 제한(위 참고)보다 길어야 합니다. 그래야 프론트가 먼저 끊지 않고 서버의 `OCR_TIMEOUT`·`AI_TIMEOUT` 응답을 화면에서 처리할 수 있습니다. 최종 값은 개발 막바지에 #122에서 정합니다.
- **재시도**: GET만 1회 재시도합니다. POST·PUT·PATCH·DELETE는 재시도하지 않습니다. TanStack Query는 기본으로 3회 재시도하므로 query는 1, mutation은 0으로 설정합니다.
- **캐싱**: 자주 안 바뀌는 데이터(카테고리, 마트 목록, 알레르기 목록, 금주의 레시피)는 `staleTime`을 길게(수 분~1시간) 둡니다. 자주 바뀌는 데이터(냉장고 재료, 장바구니)는 짧게 두고, 바꾼 뒤에는 `invalidateQueries`를 합니다.
- **에러**: 백엔드 에러 `code`를 사용자 문구로 바꾸는 표는 `lib/error-messages.ts` 하나로 관리합니다(§10 에러).
- **401 처리** (상태 코드가 아니라 `code`로 판단합니다. [`errors.md`](../agent/spec/errors.md), 영문):
    - `TOKEN_EXPIRED`(30분이 지나 액세스 쿠키 만료): `POST /api/v1/public/auth/refresh`(API 2)를 한 번 부르고, 원래 요청을 한 번 다시 보냅니다. 사용자에게는 아무것도 보여 주지 않습니다. 여러 요청이 함께 만료되면 재발급은 한 번만 하고 나머지는 그 결과를 기다립니다.
    - `REFRESH_TOKEN_INVALID`(재발급 실패: 14일이 지났거나 지워졌거나 위조된 리프레시 토큰)와 `UNAUTHORIZED`(액세스 쿠키가 없거나 위조됨): TanStack Query 캐시를 비우고 `/public/auth/login`으로 보낸 뒤, 로그인하면 원래 주소로 돌아갑니다.
    - 이 처리는 `lib/api/client.ts` 한 곳에서 합니다. 화면에서는 이 코드들을 다루지 않습니다.
- **Zustand**에는 진짜 전역 상태(로그인 사용자 요약, UI 설정)와, props로 내리면 3단계를 넘는 상태만 둡니다.
- **URL 상태**: 필터·탭·정렬·월 선택은 nuqs로 URL에 둡니다. 바텀시트·모달·드로어는 URL에 넣지 않고 로컬 상태로 관리합니다(뒤로가기: §7.1).
- **환경 변수**: 브라우저에 노출해도 되는 값만 `NEXT_PUBLIC_`을 붙입니다. 비밀 값은 서버에서만 씁니다.

## 6. 스타일 규칙

- Tailwind나 CSS-in-JS 라이브러리를 쓰지 않습니다. **CSS Modules(`*.module.css`) + `globals.css`의 CSS 변수**만 씁니다.
- 아이콘은 `@solar-icons/react`의 스타일별 경로에서 import합니다. 예: `import { HomeIcon } from "@solar-icons/react/linear"`. 기본 크기·색·선 두께는 `globals.css`의 `--solar-*` 변수로 정합니다.
- 디자인 토큰은 [`design-tokens.md`](./design-tokens.md)를 따릅니다. 피그마 디자인 시스템(`0. 기초`)의 변수·스타일을 이름 그대로 옮긴 것이고, CSS 변수 이름은 피그마 Dev Mode의 코드 이름과 같습니다(`var(--color-text-strong)`, `var(--space-16)`, `font: var(--font-content)`). `design/tokens/build_tokens.py`가 만들고, 결과를 `app/tokens.css`로 복사해 `globals.css`에서 불러옵니다. `app/tokens.css`는 손으로 고치지 않습니다. 피그마에서 변수·스타일이 걸린 값은 같은 이름의 토큰을, 걸리지 않은 값은 프레임 값을 그대로 씁니다.
- 모바일 우선입니다. 기준 폭은 360px입니다. 기본 레이아웃 최대 폭은 `--layout-max-width`(480px), 좌우 여백은 `--layout-gutter`입니다. 480px보다 넓은 화면은 480px 컨테이너를 가운데 정렬해 보여 줍니다. 최상위 `app/layout.tsx`가 모든 화면을 컨테이너 하나(`app/layout.module.css`)로 감싸므로 화면마다 최대 폭을 걸지 않습니다. 컨테이너 배경은 `bg/surface`이고, 그 바깥은 회색(`body`의 `--layout-outside-bg`, #ececec)입니다. 컨테이너는 `overflow: clip`이라 그림자처럼 넘치는 것이 바깥에 보이지 않습니다(`hidden`은 스크롤 영역을 만들어 헤더의 `position: sticky`가 깨지므로 쓰지 않음). 그 컨테이너 밖에 있는 요소 — `position: fixed` 고정 영역과 `body`에 포털로 붙는 요소(하단 고정 영역, 시트, 모달, 드로워) — 는 각자 `max-width: var(--layout-max-width)`를 겁니다. 시트·모달 뒤의 딤도 마찬가지라서, 겹쳐 뜨는 것이 컨테이너 바깥 회색 영역을 덮지 않습니다.
- 라이트 모드만 있습니다. 다크 모드는 없습니다.
- 단위는 rem입니다(1rem = 16px, 루트 font-size는 바꾸지 않음). 피그마 px 값은 16으로 나눠 씁니다(10px → 0.625rem). 테두리 같은 선만 px로 씁니다.
- 폰트: 파일은 woff2만 씁니다.
    - Pretendard(본문): `pretendard` 패키지의 dynamic subset 파일을 씁니다(굵기마다 글자 묶음별로 `unicode-range`로 나뉘어 있어 화면에 나온 글자가 든 묶음만 받습니다. 통파일은 굵기마다 약 770KB). `app/fonts/pretendard.css`는 `pnpm gen:fonts`(`scripts/gen-pretendard-css.mjs`)가 만들고 `app/layout.tsx`에서 불러옵니다. 손으로 고치지 않습니다. 굵기는 쓰는 것만 400/500/700/800입니다(400은 `Remake/content-regular`·`Remake/error` 텍스트 스타일, 800은 마트 배지). 굵기를 더하려면 스크립트의 `WEIGHTS`를 고치고 다시 실행합니다. 글꼴 이름은 `Pretendard`이고, `tokens.css`의 `--font-family-base`가 이 이름을 씁니다.
    - GmarketSans(로고 글자 전용): `app/fonts/fonts.ts`에서 `next/font/local`로 불러오고 `app/layout.tsx`의 `<html>`에 붙입니다. 굵기 300/500/700, `preload: false`. `font-family: var(--font-family-logo)`(`globals.css`에 정의)로 씁니다. 다른 글자에는 쓰지 않습니다.
- 화면 전체 높이는 `100dvh`, 하단 고정 요소는 `env(safe-area-inset-bottom)`을 더합니다.
- 대부분 플랫하게 만들고, 글래스 효과는 피그마에서 글래스가 걸린 요소에만 씁니다.
- 두 줄 이상으로 줄바꿈되는 글은 줄 간격을 150%로 줍니다(`font` 토큰 뒤에 `line-height: 1.5`). 피그마 텍스트 스타일이 140%여도 마찬가지입니다. 한 줄짜리 글은 토큰의 줄 간격을 그대로 씁니다.
- 상호작용을 나타내는 색(파란색 `--color-text-primary`·`--color-brand-primary`와 링크 스타일)은 버튼과 링크에만 씁니다. 일반 글자·숫자·꾸밈에는 쓰지 않아서, 파란색이 늘 "누를 수 있다"는 뜻이 되게 합니다.
- 플레이스홀더를 레이블 대신 쓰지 않습니다. 모든 입력칸에는 보이는 레이블(또는 스크린 리더가 읽는 `<label>`)을 두고, 플레이스홀더는 예시 값만 보여 줍니다.
- 선택지 묶음(라디오, 세그먼트 컨트롤, 칩, 선택 시트, 휠)에는 기본 옵션을 지정합니다. 아무것도 고르지 않은 상태로 시작하지 않고, 가장 많이 쓰거나 가장 안전한 옵션을 미리 골라 둡니다. 예외: 사용자가 그냥 지나쳤을 때 잘못 미리 고른 값이 그대로 저장되는 선택지(예: 재료 카테고리. 단위 찾기, 카테고리 이미지·필터, 이벤트 `category_code`에 쓰임)는 비워 둡니다. 사용자가 고르기 전에는 저장 버튼이 꺼져 있습니다.
- 스타일 파일은 컴포넌트와 같은 폴더에 같은 이름으로 둡니다. 예: `IngredientCard.tsx` + `IngredientCard.module.css`

### 6.1 시맨틱 HTML

- 태그는 모양이 아니라 의미와 용도에 맞게 선택합니다. 이동에는 `href`가 있는 `a`, 실행·저장에는 `button`을 쓰고, 클릭 가능한 `div`로 대신하지 않습니다. 예: 상세 이동은 링크, 저장 동작은 버튼입니다.
- 목록은 `ul`·`ol`과 `li`, 표 형태의 데이터는 제목 셀을 갖춘 `table`로 표현합니다. 단순 배치에는 의미 없는 컨테이너를 사용하고 모든 영역을 `section`으로 만들지 않습니다. `section`을 쓸 때는 제목을 두고 `aria-labelledby`로 연결합니다.
- 입력은 기본 폼 요소와 연결된 `label`을 사용합니다. placeholder만으로 이름을 대신하지 않습니다. 제출에 실패하면 첫 번째 오류 입력으로 포커스를 옮깁니다. 정보 이미지에는 의미 있는 `alt`, 장식 이미지에는 빈 `alt`를 제공합니다.

내용의 관계를 적절한 요소로 표현하는 기준은 [W3C 페이지 구조 가이드](https://www.w3.org/WAI/tutorials/page-structure/)를 따릅니다.

### 6.2 랜드마크 및 키보드 접근성

- 페이지 공통 머리말은 `header`, 주요 탐색은 `nav`, 본문은 `main`, 공통 꼬리말이 있으면 `footer`로 구분합니다. 활성 본문 `main`은 하나로 유지하고 레이아웃과 페이지에서 중복 생성하지 않습니다. 글 내부의 `header`·`footer`는 페이지 전체 랜드마크와 구분합니다. 같은 종류의 탐색 영역이 여러 개라면 이름으로 구별합니다.
- 반복 탐색을 건너뛰는 ‘본문 바로가기’를 제공하고, DOM 순서와 읽기·포커스 순서를 일치시킵니다. 양수 `tabindex`로 순서를 강제하지 않고 포커스 표시가 보이도록 유지합니다.
- 모든 조작을 키보드로 수행할 수 있게 합니다. 예: Tab·Shift+Tab으로 이동하고 기본 버튼은 Enter·Space로 실행합니다. 모달은 열릴 때 내부로 포커스를 옮기고 내부에서 순환시키며, 닫기 동작·Escape를 제공하고 닫힌 뒤 실행 요소로 복귀시킵니다. 모달·바텀시트·드로어에는 `role="dialog"`, `aria-modal="true"`, `aria-labelledby`를 붙이고, 뒤 배경은 `inert`로 만듭니다.
- 상태·오류를 색상만으로 구분하지 않고 문구도 제공합니다(예: 카테고리 색 + 카테고리 이름). 확대나 좁은 화면에서도 내용과 조작 요소가 가려지지 않게 합니다.
- 터치 영역은 최소 44×44px, 글자 색 대비는 4.5:1 이상입니다.
- `prefers-reduced-motion: reduce`이면 애니메이션을 줄입니다.
- 검사: `pnpm lint`에서 jsx-a11y 규칙(`eslint-config-next`에 포함)을 통과해야 합니다. 화면을 완성하면 axe나 Lighthouse 접근성 검사로 확인합니다.

랜드마크는 [W3C 페이지 구조 가이드](https://www.w3.org/WAI/tutorials/page-structure/), 키보드 조작은 [W3C 키보드 접근성 가이드](https://www.w3.org/WAI/WCAG22/Understanding/keyboard.html)를 참고합니다.

### 6.3 WAI-ARIA 사용 기준

기본 HTML의 의미와 동작을 우선 사용하고, 이것만으로 전달하기 어려운 이름·상태·변경 알림에 ARIA를 보완합니다. 기본 역할을 중복 지정하거나 실제와 다른 상태를 선언하지 않습니다. ARIA를 붙이는 것만으로 키보드 동작이 생기지는 않습니다. [W3C ARIA 적용 원칙](https://www.w3.org/WAI/ARIA/apg/practices/read-me-first/)

| 속성                                                    | 적용 기준과 간단한 예시                                                                                                                                                                                                                                            |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `aria-label`                                            | 보이는 이름이 없는 아이콘 버튼 등에 ‘닫기’처럼 목적을 부여합니다. 보이는 텍스트나 `label`이 있으면 우선 사용하고 접근 가능한 이름에 해당 문구를 포함합니다.                                                                                                        |
| `aria-expanded`                                         | 사용자 정의 펼침 버튼에 실제 열림·닫힘 상태를 반영합니다. 필요하면 `aria-controls`로 대상 ID를 연결합니다. 예: 접힌 상태는 `false`, 펼친 상태는 `true`.                                                                                                            |
| `aria-live`                                             | 포커스를 옮기지 않는 저장 결과·로딩 완료 안내(토스트, OCR 결과, 생성 완료)에 사용합니다. 예: 미리 마련한 `polite` 알림 영역의 문구를 ‘저장되었습니다’로 갱신합니다. 긴급한 알림에만 `assertive` 또는 `role="alert"`를 사용하고 같은 안내를 중복 낭독하지 않습니다. |
| `aria-describedby`·`aria-invalid`                       | 입력의 도움말·오류 문구를 연결하고 검증 오류 상태를 반영합니다. 예: 입력값은 유지하면서 해당 필드의 오류 이유를 안내합니다.                                                                                                                                        |
| `aria-hidden`                                           | 장식 아이콘에 `aria-hidden="true"`를 붙입니다. 아이콘만 있는 버튼에는 그래도 `aria-label`이 필요합니다.                                                                                                                                                            |
| `role="tablist"`·`"tab"`·`"tabpanel"`와 `aria-selected` | 탭. 선택된 탭에 `aria-selected="true"`를 붙입니다.                                                                                                                                                                                                                 |
| `aria-pressed`                                          | 토글 버튼. 예: 즐겨찾기 토글이 켜져 있으면 `true`.                                                                                                                                                                                                                 |
| `aria-busy`                                             | 내용을 불러오는 동안 그 영역에 `true`를 붙입니다.                                                                                                                                                                                                                  |

펼침 상태는 [W3C Disclosure 패턴](https://www.w3.org/WAI/ARIA/apg/patterns/disclosure/), 동적 안내는 [W3C 상태 메시지 가이드](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)를 따릅니다.

### 6.4 기본 SEO

- 화면 내용을 식별하는 고유하고 간결한 `title`과 실제 내용을 요약한 `meta description`을 제공합니다. 예: `화면 이름 | REMAKE`. 제목과 설명에 개인 정보는 넣지 않습니다.
- 페이지 대표 제목은 프로젝트 기준으로 하나의 `h1`을 사용하고 하위 내용을 `h2`~`h6`로 논리적으로 구성합니다. 글자 크기를 맞추려고 제목 단계를 선택하거나 건너뛰지 않습니다.
- 링크 문구는 목적지를 설명하고 이미지 대체 텍스트는 내용의 의미를 전달합니다. 제목·설명에 무관한 키워드를 반복하지 않습니다.
- 로그인해야 쓰는 화면(냉장고·영수증·식단표 등)은 검색에 노출할 내용이 없으니 `robots: { index: false }`로 둡니다. 색인 대상은 로그인·소개 화면 같은 공개 화면뿐입니다. REMAKE는 로그인 없이 쓸 수 없어서 SEO 효과도 공개 화면에 한정됩니다. 비공개 데이터 접근은 인증·권한으로 보호하며 검색 제외 설정을 접근 제어 수단으로 사용하지 않습니다.
- 루트 `layout.tsx`에 `metadata`(title template, description, openGraph, twitter, icons, manifest)와 `viewport`(themeColor, `width=device-width`, `viewportFit: "cover"`)를 둡니다. 페이지마다 자기 `metadata`(title, description)를 둡니다.
- `app/sitemap.ts`, `app/robots.ts`, 기본 `app/opengraph-image`를 둡니다.
- 데이터 기반 메타데이터(예: 공유 레시피의 OG 제목·이미지를 만드는 `generateMetadata`)는 서버에서 백엔드를 불러야 합니다. 먼저 물어봅니다(§5).

제목·설명 등 기본 검색 표현은 [Google SEO 기본 가이드](https://developers.google.com/search/docs/fundamentals/seo-starter-guide), 제목 계층은 [W3C 페이지 구조 가이드](https://www.w3.org/WAI/tutorials/page-structure/)를 참고합니다.

## 7. PWA · TWA

- 서비스 워커는 `pnpm build && pnpm start`에서만 켜집니다. 개발 모드와 MSW 사용 중에는 꺼집니다(같은 scope 충돌 방지).
- 서비스 워커(`/serwist/sw.js`)는 빌드할 때 미리 생성되어야 합니다(`pnpm build` 결과에서 `●`). standalone 운영 서버에는 esbuild와 `app/sw.ts`가 없어서, 요청할 때 생성되는 `ƒ` 라우트가 되면 500 오류가 납니다.
    - `cacheComponents`가 켜져 있어서 라우트 세그먼트 설정(`dynamic`, `revalidate`, `dynamicParams`)은 무시되거나 빌드 오류가 납니다. `createSerwistRoute`가 주는 이 값들을 다시 내보내지 않습니다.
    - `app/serwist/[path]/route.ts`는 워커 번들링을 `"use cache"` 함수 안에서 합니다. 그래서 esbuild가 오래 걸려도 항상 미리 생성됩니다. 정적이어야 하는 다른 `GET` Route Handler도 같은 방식을 씁니다.
    - 그 함수는 `cacheLife("max")`를 호출합니다. `sw.js`는 빌드할 때만 바뀌는데, 기본 프로필(15분마다 다시 생성)을 쓰면 운영 서버가 뒤에서 번들링을 다시 시도합니다. esbuild가 없어 실패하고, 시도할 때마다 에러 로그가 남습니다. 어느 경우든 기존 `sw.js`는 계속 응답됩니다.
    - 그 결과 `sw.js`에는 `Cache-Control: s-maxage=2592000`(30일)이 붙습니다. 브라우저는 `s-maxage`를 무시하지만, 앱 앞에 공유 캐시(CDN이나 프록시 캐시)를 두면 배포 뒤에도 예전 서비스 워커가 나갈 수 있습니다. `/serwist/` 경로는 공유 캐시에서 제외합니다.
    - 서비스 워커나 이 라우트를 바꾼 뒤에는 `pnpm build` 결과에 `/serwist/sw.js`가 `●`로 나오는지, `.next/server/app/serwist/sw.js.body` 파일이 있는지 확인합니다.
- 기기 푸시는 없습니다. 알림은 앱 안에서만 보여 줍니다: 모든 탭 화면 상단의 알림 띠(API 43)와 알림 화면(API 44). `app/sw.ts`의 Web Push 처리와 `NEXT_PUBLIC_VAPID_PUBLIC_KEY`는 지워야 할 남은 코드입니다.
- TWA 배포 전 준비물: 192/512 PNG 아이콘, `public/.well-known/assetlinks.json`(앱 서명 SHA-256), HTTPS 도메인

### 7.1 뒤로가기로 시트 닫기

- 바텀시트·모달·드로어는 방문 기록을 건드리지 않습니다. URL도 바꾸지 않고 `history.pushState`도 쓰지 않습니다(열림 여부는 로컬 상태).
- 닫기는 공통 훅 `useBackToClose(isOpen, onClose)`(`hooks/`, `useDialogLayer`가 씀) 하나로 처리합니다. 컴포넌트에서 `popstate`를 직접 다루지 않습니다.
- 크롬 계열(TWA 포함)은 `CloseWatcher`로 안드로이드 뒤로 버튼과 Esc를 닫기 요청으로 받아, 페이지 이동 없이 열린 시트만 닫습니다. `CloseWatcher`는 방문 기록을 쓰지 않습니다.
- `CloseWatcher`가 없는 브라우저(사파리, 파이어폭스)는 Esc로 닫습니다. 뒤로가기(iOS 밀어서 뒤로)는 평소처럼 이전 화면으로 갑니다.
- 시트가 열린 화면이 숨겨지면(시트 안에서 다른 화면으로 이동, 뒤로가기) 시트를 닫습니다. `cacheComponents`라 이전 화면은 상태를 가진 채 남아 있어서, 닫지 않으면 돌아왔을 때 시트가 열린 채 보입니다.
- 시트·모달이 열려 있는 동안 뒤 화면은 스크롤되지 않습니다.
- `onClose`에는 닫힌 상태만 반영합니다. 화면이 숨겨질 때도 불리기 때문입니다. 취소 기록 같은 다른 동작은 넣지 않고 취소 버튼의 처리 함수에 넣습니다. 확인 뒤 다른 화면으로 갈 때는 먼저 닫고 이동합니다.
- 시트가 겹쳐 열려 있으면 가장 위의 것만 닫습니다.

## 8. 개발 흐름

1. 백엔드 API가 없으면 `mocks/handlers.ts`에 더미 응답을 먼저 만들고 화면을 개발합니다.
2. 백엔드 API가 나오면 `pnpm gen:api`로 타입을 다시 생성하고 MSW 핸들러를 실제 스펙에 맞춥니다.
3. PR 전에 `pnpm lint && pnpm typecheck && pnpm build`가 통과해야 합니다.

TypeScript는 `strict` 모드이고 `any`는 쓰지 않습니다(ESLint로 막음).

## 9. 남은 결정 사항

- [x] 인증 방식과 토큰 갱신 흐름 — **[#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42) A 확정**: 액세스(30분)·리프레시(14일) JWT를 모두 HttpOnly 쿠키(`Secure; SameSite=Lax`)로 둡니다. 로그인·재발급 응답에 `accessToken`을 넣지 않습니다. `/api/...` 호출은 `credentials: 'include'`, `Authorization: Bearer`와 JS에서 읽을 수 있는 토큰 저장은 쓰지 않습니다. 로컬(출처가 다름)에서는 백엔드가 credentials CORS를 허용해야 합니다.
- [ ] 공용 컴포넌트 목록 확정 (디자인 시안 기준)
- [x] 프론트 CI — `.github/workflows/frontend-ci.yml`이 `dev`·`main` 대상 PR에서 `pnpm lint` · `pnpm typecheck` · `pnpm build`를 돌립니다([`ci.md`](./ci.md)).
- [x] 배포 환경 — `compose.prod.yml`의 `frontend-blue`·`frontend-green` 컨테이너를 nginx(`deploy/nginx/`) 뒤에 두고, `.github/workflows/frontend-cd.yml`이 배포합니다([`deployment.md`](./deployment.md)).

## 10. 제품 정책에서 온 UI 규칙

출처: [`docs/agent/spec/domain-rules.md`](../agent/spec/domain-rules.md)(영문). 화면은 피그마를 따릅니다.

- **AI에 자유 입력 금지.** 레시피 커스텀(재료, 맛, 조리 시간, 도구, 방식, 요리 유형, 제외 알레르기)과 식단표 설정(시작일, 목적, 끼니, 인원)은 모두 선택식입니다. ID와 enum을 보냅니다.
- **소비기한 상태**: 서버가 준 `status`와 `dDay`를 보여 줍니다(DANGER = 지났거나 D-DAY, SOON = D-1~D-3, OK = 그 이후). 기기 시계로 다시 계산하지 않습니다. 예외: 등록 전 OCR 결과(p3.3)는 저장 전이고 사용자가 소비기한을 고치므로 서버 값이 없어, KST 날짜로 계산합니다(`app/user/receipt/scan/_lib/scan.ts`의 `getExpiry`).
- **오늘 날짜**: 화면에서 오늘 날짜가 필요하면(월 고르기, 미래 날짜 막기, D-N) `dayjs()` 대신 `lib/dayjs.ts`의 `todayInKst()`를 씁니다. 서버는 KST로 날짜를 세므로, 다른 시간대 기기에서는 자정 근처에 하루가 어긋납니다.
- **문구**: "AI가 생성한 레시피" 문구와 "참고용 식단" 문구는 넣지 않습니다(정책정의서 v4: 둘 다 피그마에 없음). AI 결과 면책은 이용 약관 제5조에만 둡니다(11/24 약관 결정).
- **영수증 흐름**: 촬영 → 로딩 화면(동기 대기) → 결과·확인 화면 → 저장. 사용자가 확정하기 전에는 아무것도 저장하지 않습니다. 결과 화면을 나가면 OCR 결과를 버리고 `receipt_result_abandoned`를 보냅니다. 확정한 영수증은 수정할 수 없고 삭제만 됩니다. 재료 정보는 나중에 냉장고에서 고칩니다.
- **수량 입력**: 수량 버튼 간격은 g·ml 50, kg·L 0.5, EA/PACK/BUNCH/PIECE/SHEET 1. 직접 입력은 숫자와 소수만 받고 분수는 받지 않습니다. 냉장고는 단위 9개(G, KG, ML, L, EA, PACK, BUNCH, PIECE, SHEET)만, 레시피는 14개 모두 쓰고 레시피 수량은 1/2, 1/3, 1/4로 보여 줄 수 있습니다.
- **공유**: 사진 딱 1장(필수)과 제목(필수). 둘 다 있어야 공유 버튼이 켜집니다. 확인 모달은 없습니다. 같은 레시피는 한 주에 한 번 공유할 수 있습니다. 작성자 정보는 어디에도 보여 주지 않습니다.
- **식단표**: 월간 달력, 끼니 행을 누르면 레시피 상세. 빈 칸에 직접 넣는 기능은 없습니다("+"는 식단표 추가 시트 → 생성 설정). 끼니 ⋮ 시트에는 "식단 삭제"(그 칸만 지움)와 "레시피 교체"(새 레시피, 레시피 하루 횟수 1회 사용)가 있습니다. 오늘 이후만 되고, 지난 날짜는 `PAST_DATE_NOT_ALLOWED` 토스트를 띄웁니다. 남은 횟수는 `GET /api/v1/user/usage-quotas` 값으로 보여 줍니다.
- **장바구니**: 항목마다 한 행이고 출처 레시피명을 함께 보여 줍니다(같은 재료라도 출처 레시피가 다르면 따로). 항목은 자동으로만 담깁니다(식단표 부족 재료, 재료 보충으로 만든 내 레시피 저장, 공유 레시피 저장). 사용자는 수량 변경, 선택 삭제, 선택 항목 냉장고에 추가를 할 수 있습니다. 결제나 외부 링크는 없습니다. API 45의 저장된 행은 수량이 null이 아닌 양수입니다. 레시피 저장 취소·식단 배정 삭제 시 장바구니 행과 수정 수량은 유지하며, 장바구니 선택 삭제는 레시피 저장·식단 배정에 영향을 주지 않습니다. nullable 최초 출처 슬롯 FK는 서버 메타데이터이며 새 응답 필드가 아닙니다. 영수증 등록은 명시적으로 선택한 행만 차감하고 0이 되어 삭제된 행 수를 반환합니다. 세 담기 경로 모두 `amount=null`, PINCH, TO_TASTE 재료는 장바구니에서 제외하고 레시피에는 유지하며 임의 수량으로 대체하지 않습니다. [PR #216의 장바구니 담당자 결정](https://github.com/likelion-backendschool-9th/REMAKE/pull/216#issuecomment-7123595032)으로 확정됐으며 미리보기 데이터로 API 수량의 nullable 여부를 판단하지 않습니다. 미리보기의 방어적인 null 표시는 유지해도 됩니다.
- **알림**: 앱 안에서만(기기 푸시 없음). 종류는 `EXPIRY_SOON`, `EXPIRED`, `MEAL_PLAN_COMPLETED`, `MEAL_PLAN_FAILED`. 모든 탭 화면 상단 알림 띠에 최신 문구와 안 읽은 개수를 보여 줍니다. 소비기한 알림은 눌러도 동작이 없고, `MEAL_PLAN_COMPLETED`는 그 식단표 시작일의 식단표로, `MEAL_PLAN_FAILED`는 식단표 생성 설정으로 갑니다.
- **프리미엄**: 토스페이먼츠 카드 자동결제로 실제 구독합니다(`/user/setting/subscription`, API 57~60). 토스 시크릿 키와 자동결제 키(billingKey)는 프론트에 보내거나 저장하지 않습니다.
- **에러**: 메시지와 동작은 `message`가 아니라 `code`([`docs/agent/spec/errors.md`](../agent/spec/errors.md), 영문)로 정합니다. 모르는 코드는 `INTERNAL_ERROR` 화면으로 처리합니다. `traceId`는 "문의하기" 상세에만 보여 주고 주 메시지로 쓰지 않습니다.
- **표기 형식**: API는 날짜를 `2026-11-19`, 시각을 `+09:00`이 붙은 ISO 8601, 금액을 원 단위 정수로 보냅니다. 날짜와 금액은 `lib/format.ts`의 공통 함수(`@/lib/dayjs` 기반)로만, 정책정의서의 표기 형식대로 표시합니다. 사용자에게 보이는 문구는 한국어입니다.

## 11. 이벤트 트래킹

계약: [`docs/agent/spec/events.md`](../agent/spec/events.md)(영문). 백엔드 쪽: [`backend-convention.md`](backend-convention.md) §이벤트 발행.

- 공용 트래커 하나를 씁니다(`lib/events/`에 만들 예정). 컴포넌트에서 `POST /api/v1/user/events`를 직접 부르지 않습니다.
- API: `track(eventName, properties?)`. 트래커가 `screen`, `sessionId`(30분 동안 아무 동작이 없으면 새 세션), `occurredAt`을 붙이고, 최대 50개씩 모아서 일정 주기, `visibilitychange`/`pagehide`(`navigator.sendBeacon` 사용), 로그아웃 때 보냅니다.
- 이벤트 정의서에서 출처가 CLIENT인 이벤트만 보냅니다. SERVER 이벤트는 백엔드가 발행하고, 같은 행동을 양쪽에서 기록하지 않습니다.
- 트래킹 실패는 조용히 넘깁니다. 토스트를 띄우지 않고, 재시도는 작은 횟수까지만 합니다.
- 속성에 개인정보나 자유 텍스트를 넣지 않습니다. 코드, 개수, 참거짓(`category_code`, `item_count`)을 보냅니다. 속성은 평평하게, 25개 이하로 둡니다.
- 화면 코드(`fridge_list`, `receipt_result`, `recipe_detail`, `meal_plan_calendar` 등)는 이벤트 정의서의 화면 코드 탭을 따르고, `lib/events/screens.ts`에 상수로 둡니다.
- `screen_viewed`는 페이지마다 직접 넣지 않고 공용 레이아웃 훅 한 곳에서 보냅니다.

## 12. 성능

- 이미지: `next/image`를 씁니다(sharp는 Next가 설치). `sizes`를 꼭 지정하고, `priority`는 첫 화면의 큰 이미지에만 붙입니다. 외부 이미지 주소는 `images.remotePatterns`에 등록합니다. 사용자가 올리는 사진(영수증·조리 사진)은 업로드 전에 줄입니다.
- 무거운 컴포넌트(차트, 바텀시트 내부)는 `next/dynamic`으로 나눠 불러옵니다.
- 라우트마다 `loading.tsx`(스켈레톤)와 `error.tsx`를 두고, 전역 `not-found.tsx`를 하나 둡니다. 큰 영역은 `Suspense`로 나눠 스트리밍합니다.
- 긴 목록(랭킹 TOP 100 등)은 무한 스크롤로 하고 가상화를 검토합니다.
- 번들 크기: `pnpm next analyze`(Turbopack 번들 분석기, Next 16.1 이상). `@next/bundle-analyzer`는 webpack 빌드에서만 동작합니다.
