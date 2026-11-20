# REMAKE 프론트엔드 설계 (Blueprint, 한글 번역)

> [`frontend/blueprint.md`](../../frontend/blueprint.md)의 한글 번역본입니다. AI 에이전트는 영문 원본을 읽고, 개발자는 이 문서를 봅니다. 두 문서는 항상 같은 내용을 유지합니다.

냉장고 속 식재료를 관리하고, AI 맞춤 레시피와 소비기한 알림으로 식재료 낭비를 줄이는 서비스의 프론트엔드 설계 문서입니다.
모바일 우선 PWA로 만들고, PWABuilder(TWA)로 안드로이드 앱으로 패키징합니다.

> 화면 목록과 라우트는 초안입니다. 기획·디자인이 확정되면 이 문서를 먼저 고친 뒤 코드를 바꿉니다.

## 1. 역할 분담

- **프론트(Next.js)**: 화면과 사용자 상호작용만 담당합니다. 비즈니스 로직과 데이터 저장은 하지 않습니다.
- **백엔드(Spring Boot)**: 인증, 데이터, OCR·AI 처리, 푸시 발송을 담당합니다.
- 프론트는 Spring REST API만 호출합니다. Next의 Route Handler·Server Action으로 백엔드 기능을 대신 만들지 않습니다.
  예외: 서비스 워커 서빙 라우트(`app/serwist/[path]`).
- **`/api/`는 백엔드 경로입니다.** 운영에서는 프론트와 백엔드가 한 도메인을 같이 쓰고, 프록시가 `/api/`로 시작하는 요청을 경로 그대로 Spring으로 보냅니다. 나머지는 모두 Next.js로 갑니다([`deployment.md`](./deployment.md) 참고).
  - 주소가 `/api/`로 시작하는 라우트(페이지, Route Handler 등)는 만들지 않습니다. 운영에서 그 주소로 온 요청은 Next에 닿지 않습니다. 폴더가 아니라 URL로 판단합니다. 라우트 그룹은 URL에 나타나지 않아서 `app/(group)/api/...`도 주소는 `/api/...`가 됩니다.
  - 나중에 만들 헬스체크(`/healthz`)처럼 프론트 전용 주소는 `/api/` 밖에 둡니다.

## 2. 기술 스택

| 영역            | 기술                                   | 쓰는 이유                                                   |
| --------------- | -------------------------------------- | ----------------------------------------------------------- |
| 프레임워크      | Next.js (App Router) + TypeScript      | 라우트별 화면 분리, 화면만 담당                             |
| 패키지 매니저   | pnpm                                   | 빠른 설치, lockfile로 7명 의존성 버전 통일                  |
| 스타일          | CSS Modules + CSS 변수                 | 별도 라이브러리 없이 컴포넌트 단위 스타일, 디자인 토큰 공유 |
| 서버 데이터     | TanStack Query                         | 서버 데이터 캐싱·다시 불러오기                              |
| 클라이언트 상태 | Zustand                                | 로그인 정보 등 최소한만                                     |
| API 연동        | openapi-typescript + openapi-fetch     | Spring OpenAPI에서 타입 자동 생성, 타입 안전한 호출         |
| 폼              | React Hook Form + Zod                  | 온보딩, 커스텀 레시피 입력, 재료 수정 검증                  |
| 날짜            | dayjs                                  | 소비기한 계산, 식단표 주간 캘린더                           |
| 아이콘          | @solar-icons/react (Solar)             | 트리 셰이킹되는 React 컴포넌트, 오프라인에서도 동작         |
| PWA·푸시        | Serwist + Web Push                     | 서비스 워커, 오프라인 화면, 소비기한 알림                   |
| 스토어 배포     | PWABuilder (TWA)                       | 안드로이드 앱 패키지 생성                                   |
| 웹 배포         | `output: "standalone"` + nginx         | Node 서버를 nginx 뒤에서 실행                               |
| 코드 품질       | ESLint + Prettier, Husky + lint-staged | 7명 코드 스타일 통일, 커밋 전 자동 검사                     |
| API 목업        | MSW                                    | 백엔드 완성 전 더미 응답으로 화면 개발                      |

## 3. 폴더 구조

```
frontend/
├─ app/                  # 라우트. 각 라우트 폴더에 그 라우트만 쓰는 코드를 함께 둔다 (3.1 참고)
│  ├─ layout.tsx         # 루트 레이아웃, globals.css, 홈 page.tsx
│  ├─ fridge/            # 라우트 예시 — 3.1 참고
│  ├─ sw.ts              # 서비스 워커 (캐싱, 푸시 수신)
│  ├─ serwist/[path]/    # 서비스 워커 서빙 라우트
│  ├─ ~offline/          # 오프라인 대체 화면
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
app/fridge/
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
│  └─ utils.ts                  # 순수 함수 (예: 소비기한 D-day 계산)
└─ [id]/
   └─ page.tsx
```

1. **private 폴더.** `_`로 시작하는 폴더는 Next.js private 폴더라서 라우트가 되지 않습니다. 라우트의 화면 외 코드는 모두 `_components/`, `_hooks/`, `_lib/`에 둡니다. 이 안에 `page.tsx`나 `route.ts`를 두지 않습니다.
2. **라우트 단위 고립.** 라우트는 자기 폴더와 상위 라우트 폴더의 코로케이션 코드만 import할 수 있습니다 (예: `app/fridge/[id]`는 `app/fridge/_components`를 쓸 수 있음). 형제 라우트나 관련 없는 라우트의 코드는 import하지 않습니다.
3. **컴포넌트 단위 고립.** 컴포넌트마다 폴더 하나를 두고 `.tsx`, `.module.css`, 그 컴포넌트만 쓰는 훅·유틸을 함께 둡니다. 다른 컴포넌트의 CSS Module이나 내부 파일을 import하지 않습니다.
4. **공유되면 올리기.** 관련 없는 두 라우트가 같은 코드를 필요로 하면 위로 올립니다 — UI 컴포넌트 → `components/<이름>/`, React 훅 → `hooks/`, queryKey와 Query 훅 → `lib/queries/<도메인>.ts`, 그 밖의 함수 → `lib/`. 공용 코드는 특정 페이지를 알면 안 됩니다.
5. **import 경로.** 코로케이션 코드는 상대 경로(`./_components/...`, `../_lib/...`)로, 공용 코드는 `@/`(`@/components/Button`)로 import합니다. `@/app/...`에서는 import하지 않습니다 (ESLint로 막음).

공용 컴포넌트 구성:

```
components/Button/
├─ Button.tsx
├─ Button.module.css
└─ index.ts          # export { Button } from "./Button";
```

## 4. 화면 / 라우트 (초안)

| 라우트           | 화면           | 주요 기능                                        |
| ---------------- | -------------- | ------------------------------------------------ |
| `/`              | 홈             | 소비기한 임박 재료, 추천 레시피 요약             |
| `/login`         | 로그인         | 소셜 로그인                                      |
| `/onboarding`    | 온보딩         | 가구원 수 → 알레르기 (단계마다 건너뛰기 가능)    |
| `/fridge`        | 내 냉장고      | 재료 목록, 보관 위치·소비기한 정렬               |
| `/fridge/[id]`   | 재료 상세·수정 | 수량·소비기한 수정                               |
| `/fridge/add`    | 재료 등록      | 직접 입력 / 영수증 촬영 선택                     |
| `/receipt`       | 영수증 OCR     | 촬영 → 로딩(요청 한 번) → 품목 확인·수정 → 확정  |
| `/recipes`       | 레시피 추천    | 보유 재료 기반 AI 추천                           |
| `/recipes/[id]`  | 레시피 상세    | 재료·조리 순서, 부족 재료                        |
| `/recipes/new`   | 커스텀 레시피  | 고른 옵션으로 레시피 생성 (자유 입력 없음)       |
| `/meal-plan`     | 식단표         | 월간 달력, AI 식단표 생성                        |
| `/notifications` | 알림           | 소비기한 알림 목록, 푸시 권한 설정               |
| `/mypage`        | 마이페이지     | 프로필, 설정                                     |

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
- **OCR과 AI 생성**은 각각 동기 요청 한 번이고, 응답에 결과가 담깁니다. 기다리는 동안 로딩 화면을 보여 줍니다(시간 제한: OCR 30초, AI 60초). 작업 ID, polling, 스트리밍은 없습니다.
- **queryKey**는 Query 훅과 같은 곳에 둡니다. 라우트의 `_lib/queries.ts`, 공유되면 `lib/queries/<도메인>.ts`. 예: `fridgeKeys.all`, `fridgeKeys.detail(id)`

## 6. 스타일 규칙

- Tailwind나 CSS-in-JS 라이브러리를 쓰지 않습니다. **CSS Modules(`*.module.css`) + `globals.css`의 CSS 변수**만 씁니다.
- 아이콘은 `@solar-icons/react`의 스타일별 경로에서 import합니다. 예: `import { HomeIcon } from "@solar-icons/react/linear"`. 기본 크기·색·선 두께는 `globals.css`의 `--solar-*` 변수로 정합니다.
- 색, 간격, 글자 크기, radius는 `globals.css`의 토큰(`--color-*`, `--space-*`, `--font-size-*`, `--radius-*`)을 씁니다. 하드코딩한 값을 새로 만들지 않습니다.
- 모바일 우선입니다. 기본 레이아웃 최대 폭은 `--layout-max-width`(480px), 좌우 여백은 `--layout-gutter`입니다.
- 스타일 파일은 컴포넌트와 같은 폴더에 같은 이름으로 둡니다. 예: `IngredientCard.tsx` + `IngredientCard.module.css`

### 6.1 시맨틱 HTML

- 태그는 모양이 아니라 의미와 용도에 맞게 선택합니다. 이동에는 `href`가 있는 `a`, 실행·저장에는 `button`을 쓰고, 클릭 가능한 `div`로 대신하지 않습니다. 예: 상세 이동은 링크, 저장 동작은 버튼입니다.
- 목록은 `ul`·`ol`과 `li`, 표 형태의 데이터는 제목 셀을 갖춘 `table`로 표현합니다. 단순 배치에는 의미 없는 컨테이너를 사용하고 모든 영역을 `section`으로 만들지 않습니다.
- 입력은 기본 폼 요소와 연결된 `label`을 사용합니다. placeholder만으로 이름을 대신하지 않습니다. 정보 이미지에는 의미 있는 `alt`, 장식 이미지에는 빈 `alt`를 제공합니다.

내용의 관계를 적절한 요소로 표현하는 기준은 [W3C 페이지 구조 가이드](https://www.w3.org/WAI/tutorials/page-structure/)를 따릅니다.

### 6.2 랜드마크 및 키보드 접근성

- 페이지 공통 머리말은 `header`, 주요 탐색은 `nav`, 본문은 `main`, 공통 꼬리말이 있으면 `footer`로 구분합니다. 활성 본문 `main`은 하나로 유지하고 레이아웃과 페이지에서 중복 생성하지 않습니다. 글 내부의 `header`·`footer`는 페이지 전체 랜드마크와 구분합니다. 같은 종류의 탐색 영역이 여러 개라면 이름으로 구별합니다.
- 반복 탐색을 건너뛰는 ‘본문 바로가기’를 제공하고, DOM 순서와 읽기·포커스 순서를 일치시킵니다. 양수 `tabindex`로 순서를 강제하지 않고 포커스 표시가 보이도록 유지합니다.
- 모든 조작을 키보드로 수행할 수 있게 합니다. 예: Tab·Shift+Tab으로 이동하고 기본 버튼은 Enter·Space로 실행합니다. 모달은 열릴 때 내부로 포커스를 옮기고 내부에서 순환시키며, 닫기 동작·Escape를 제공하고 닫힌 뒤 실행 요소로 복귀시킵니다.
- 상태·오류를 색상만으로 구분하지 않고 문구도 제공합니다. 확대나 좁은 화면에서도 내용과 조작 요소가 가려지지 않게 합니다.

랜드마크는 [W3C 페이지 구조 가이드](https://www.w3.org/WAI/tutorials/page-structure/), 키보드 조작은 [W3C 키보드 접근성 가이드](https://www.w3.org/WAI/WCAG22/Understanding/keyboard.html)를 참고합니다.

### 6.3 WAI-ARIA 사용 기준

기본 HTML의 의미와 동작을 우선 사용하고, 이것만으로 전달하기 어려운 이름·상태·변경 알림에 ARIA를 보완합니다. 기본 역할을 중복 지정하거나 실제와 다른 상태를 선언하지 않습니다. ARIA를 붙이는 것만으로 키보드 동작이 생기지는 않습니다. [W3C ARIA 적용 원칙](https://www.w3.org/WAI/ARIA/apg/practices/read-me-first/)

| 속성 | 적용 기준과 간단한 예시 |
| --- | --- |
| `aria-label` | 보이는 이름이 없는 아이콘 버튼 등에 ‘닫기’처럼 목적을 부여합니다. 보이는 텍스트나 `label`이 있으면 우선 사용하고 접근 가능한 이름에 해당 문구를 포함합니다. |
| `aria-expanded` | 사용자 정의 펼침 버튼에 실제 열림·닫힘 상태를 반영합니다. 필요하면 `aria-controls`로 대상 ID를 연결합니다. 예: 접힌 상태는 `false`, 펼친 상태는 `true`. |
| `aria-live` | 포커스를 옮기지 않는 저장 결과·로딩 완료 안내에 사용합니다. 예: 미리 마련한 `polite` 알림 영역의 문구를 ‘저장되었습니다’로 갱신합니다. 긴급한 알림에만 `assertive` 또는 `role="alert"`를 사용하고 같은 안내를 중복 낭독하지 않습니다. |
| `aria-describedby`·`aria-invalid` | 입력의 도움말·오류 문구를 연결하고 검증 오류 상태를 반영합니다. 예: 입력값은 유지하면서 해당 필드의 오류 이유를 안내합니다. |

펼침 상태는 [W3C Disclosure 패턴](https://www.w3.org/WAI/ARIA/apg/patterns/disclosure/), 동적 안내는 [W3C 상태 메시지 가이드](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)를 따릅니다.

### 6.4 기본 SEO

- 화면 내용을 식별하는 고유하고 간결한 `title`과 실제 내용을 요약한 `meta description`을 제공합니다. 예: `화면 이름 | REMAKE`. 제목과 설명에 개인 정보는 넣지 않습니다.
- 페이지 대표 제목은 프로젝트 기준으로 하나의 `h1`을 사용하고 하위 내용을 `h2`~`h6`로 논리적으로 구성합니다. 글자 크기를 맞추려고 제목 단계를 선택하거나 건너뛰지 않습니다.
- 링크 문구는 목적지를 설명하고 이미지 대체 텍스트는 내용의 의미를 전달합니다. 제목·설명에 무관한 키워드를 반복하지 않습니다.
- 검색 공개 여부는 페이지 정책이 확정되면 정합니다. 비공개 데이터 접근은 인증·권한으로 보호하며 검색 제외 설정을 접근 제어 수단으로 사용하지 않습니다.

제목·설명 등 기본 검색 표현은 [Google SEO 기본 가이드](https://developers.google.com/search/docs/fundamentals/seo-starter-guide), 제목 계층은 [W3C 페이지 구조 가이드](https://www.w3.org/WAI/tutorials/page-structure/)를 참고합니다.

## 7. PWA · 푸시 · TWA

- 서비스 워커는 `pnpm build && pnpm start`에서만 켜집니다. 개발 모드와 MSW 사용 중에는 꺼집니다(같은 scope 충돌 방지).
- 서비스 워커(`/serwist/sw.js`)는 빌드할 때 미리 생성되어야 합니다(`pnpm build` 결과에서 `●`). standalone 운영 서버에는 esbuild와 `app/sw.ts`가 없어서, 요청할 때 생성되는 `ƒ` 라우트가 되면 500 오류가 납니다.
  - `cacheComponents`가 켜져 있어서 라우트 세그먼트 설정(`dynamic`, `revalidate`, `dynamicParams`)은 무시되거나 빌드 오류가 납니다. `createSerwistRoute`가 주는 이 값들을 다시 내보내지 않습니다.
  - `app/serwist/[path]/route.ts`는 워커 번들링을 `"use cache"` 함수 안에서 합니다. 그래서 esbuild가 오래 걸려도 항상 미리 생성됩니다. 정적이어야 하는 다른 `GET` Route Handler도 같은 방식을 씁니다.
  - 그 함수는 `cacheLife("max")`를 호출합니다. `sw.js`는 빌드할 때만 바뀌는데, 기본 프로필(15분마다 다시 생성)을 쓰면 운영 서버가 뒤에서 번들링을 다시 시도합니다. esbuild가 없어 실패하고, 시도할 때마다 에러 로그가 남습니다. 어느 경우든 기존 `sw.js`는 계속 응답됩니다.
  - 그 결과 `sw.js`에는 `Cache-Control: s-maxage=2592000`(30일)이 붙습니다. 브라우저는 `s-maxage`를 무시하지만, 앱 앞에 공유 캐시(CDN이나 프록시 캐시)를 두면 배포 뒤에도 예전 서비스 워커가 나갈 수 있습니다. `/serwist/` 경로는 공유 캐시에서 제외합니다.
  - 서비스 워커나 이 라우트를 바꾼 뒤에는 `pnpm build` 결과에 `/serwist/sw.js`가 `●`로 나오는지, `.next/server/app/serwist/sw.js.body` 파일이 있는지 확인합니다.
- 푸시: 프론트가 `NEXT_PUBLIC_VAPID_PUBLIC_KEY`로 구독을 만들어 백엔드에 저장하고, 백엔드가 발송합니다. 수신·클릭 처리는 `app/sw.ts`에서 합니다.
- TWA 배포 전 준비물: 192/512 PNG 아이콘, `public/.well-known/assetlinks.json`(앱 서명 SHA-256), HTTPS 도메인

## 8. 개발 흐름

1. 백엔드 API가 없으면 `mocks/handlers.ts`에 더미 응답을 먼저 만들고 화면을 개발합니다.
2. 백엔드 API가 나오면 `pnpm gen:api`로 타입을 다시 생성하고 MSW 핸들러를 실제 스펙에 맞춥니다.
3. PR 전에 `pnpm lint && pnpm typecheck && pnpm build`가 통과해야 합니다.

## 9. 남은 결정 사항

- [x] 인증 방식과 토큰 갱신 흐름 — **[#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42) A 확정**: 액세스(30분)·리프레시(14일) JWT를 모두 HttpOnly 쿠키(`Secure; SameSite=Lax`)로 둡니다. 로그인·재발급 응답에 `accessToken`을 넣지 않습니다. `/api/...` 호출은 `credentials: 'include'`, `Authorization: Bearer`와 JS에서 읽을 수 있는 토큰 저장은 쓰지 않습니다. 로컬(출처가 다름)에서는 백엔드가 credentials CORS를 허용해야 합니다.
- [ ] 공용 컴포넌트 목록과 디자인 토큰 확정 (디자인 시안 기준)
- [ ] 프론트 CI (GitHub Actions: lint · typecheck · build)
- [ ] 배포 환경 (nginx + Node 컨테이너 구성)

## 10. 제품 정책에서 온 UI 규칙

출처: [`docs/agent/spec/domain-rules.md`](../agent/spec/domain-rules.md)(영문). 화면은 피그마를 따릅니다.

- **AI에 자유 입력 금지.** 레시피 커스텀(재료, 맛, 조리 시간, 도구, 방식, 요리 유형, 제외 알레르기)과 식단표 설정(시작일, 목적, 끼니, 인원)은 모두 선택식입니다. ID와 enum을 보냅니다.
- **소비기한 상태**: 서버가 준 `status`와 `dDay`를 보여 줍니다(DANGER = 지났거나 D-DAY, SOON = D-1~D-3, OK = 그 이후). 기기 시계로 다시 계산하지 않습니다.
- **문구**: AI 레시피 상세와 공유 레시피 상세에 "AI가 생성한 레시피"를, 식단표에 "참고용" 문구를 보여 줍니다.
- **영수증 흐름**: 촬영 → 로딩 화면(동기 대기) → 결과·확인 화면 → 저장. 사용자가 확정하기 전에는 아무것도 저장하지 않습니다. 결과 화면을 나가면 OCR 결과를 버리고 `receipt_result_abandoned`를 보냅니다. 확정한 영수증은 수정할 수 없고 삭제만 됩니다. 재료 정보는 나중에 냉장고에서 고칩니다.
- **수량 입력**: 수량 버튼 간격은 g·ml 50, kg·L 0.5, EA/PACK/BUNCH/PIECE/SHEET 1. 직접 입력은 숫자와 소수만 받고 분수는 받지 않습니다. 냉장고는 단위 9개(G, KG, ML, L, EA, PACK, BUNCH, PIECE, SHEET)만, 레시피는 14개 모두 쓰고 레시피 수량은 1/2, 1/3, 1/4로 보여 줄 수 있습니다.
- **공유**: 제목과 조리 사진 1장 이상이 필수이고, 먼저 확인 모달을 띄웁니다. 작성자 정보는 어디에도 보여 주지 않습니다.
- **식단표**: 월간 달력, 칸을 누르면 레시피 상세. 식단 추가는 저장한 레시피를 빈 칸에 넣는 것입니다. 삭제와 칸 재생성은 없습니다. "남은 N회"는 `GET /api/v1/user/usage-quotas` 값으로 보여 줍니다.
- **장바구니**: 레시피별로 묶습니다. 항목은 자동으로만 담기고(식단표 생성, 레시피 저장), 사용자는 수량 변경과 선택 삭제만 할 수 있습니다. 결제나 외부 링크는 없습니다.
- **알림**: `EXPIRY_SOON`, `EXPIRED`만 있습니다. "영수증 분석 완료" 알림은 없습니다.
- **프리미엄**: 설정의 구독 화면은 UI만 만듭니다.
- **에러**: 메시지와 동작은 `message`가 아니라 `code`([`docs/agent/spec/errors.md`](../agent/spec/errors.md), 영문)로 정합니다. 모르는 코드는 `INTERNAL_ERROR` 화면으로 처리합니다. `traceId`는 "문의하기" 상세에만 보여 주고 주 메시지로 쓰지 않습니다.
- **표기 형식**: API는 날짜를 `2026-11-19`, 시각을 `+09:00`이 붙은 ISO 8601, 금액을 원 단위 정수로 보냅니다. 날짜는 `@/lib/dayjs`로 표시합니다. 사용자에게 보이는 문구는 한국어입니다.

## 11. 이벤트 트래킹

계약: [`docs/agent/spec/events.md`](../agent/spec/events.md)(영문). 백엔드 쪽: [`backend-convention.md`](backend-convention.md) §이벤트 발행.

- 공용 트래커 하나를 씁니다(`lib/events/`에 만들 예정). 컴포넌트에서 `POST /api/v1/user/events`를 직접 부르지 않습니다.
- API: `track(eventName, properties?)`. 트래커가 `screen`, `sessionId`(30분 동안 아무 동작이 없으면 새 세션), `occurredAt`을 붙이고, 최대 50개씩 모아서 일정 주기, `visibilitychange`/`pagehide`(`navigator.sendBeacon` 사용), 로그아웃 때 보냅니다.
- 이벤트 정의서에서 출처가 CLIENT인 이벤트만 보냅니다. SERVER 이벤트는 백엔드가 발행하고, 같은 행동을 양쪽에서 기록하지 않습니다.
- 트래킹 실패는 조용히 넘깁니다. 토스트를 띄우지 않고, 재시도는 작은 횟수까지만 합니다.
- 속성에 개인정보나 자유 텍스트를 넣지 않습니다. 코드, 개수, 참거짓(`category_code`, `item_count`)을 보냅니다. 속성은 평평하게, 25개 이하로 둡니다.
- 화면 코드(`fridge_list`, `receipt_result`, `recipe_detail`, `meal_plan_calendar` 등)는 이벤트 정의서의 화면 코드 탭을 따르고, `lib/events/screens.ts`에 상수로 둡니다.
- `screen_viewed`는 페이지마다 직접 넣지 않고 공용 레이아웃 훅 한 곳에서 보냅니다.
