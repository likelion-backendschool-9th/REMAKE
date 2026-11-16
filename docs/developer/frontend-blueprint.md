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

## 2. 기술 스택

| 영역            | 기술                                   | 쓰는 이유                                                   |
| --------------- | -------------------------------------- | ----------------------------------------------------------- |
| 프레임워크      | Next.js (App Router) + TypeScript      | 라우트별 화면 분리, 화면만 담당                             |
| 패키지 매니저   | pnpm                                   | 빠른 설치, lockfile로 7명 의존성 버전 통일                  |
| 스타일          | CSS Modules + CSS 변수                 | 별도 라이브러리 없이 컴포넌트 단위 스타일, 디자인 토큰 공유 |
| 서버 데이터     | TanStack Query                         | 캐싱, OCR·식단표 진행 상태 polling                          |
| 클라이언트 상태 | Zustand                                | 로그인 정보 등 최소한만                                     |
| API 연동        | openapi-typescript + openapi-fetch     | Spring OpenAPI에서 타입 자동 생성, 타입 안전한 호출         |
| 폼              | React Hook Form + Zod                  | 온보딩, 커스텀 레시피 입력, 재료 수정 검증                  |
| 날짜            | dayjs                                  | 소비기한 계산, 식단표 주간 캘린더                           |
| 아이콘          | @solar-icons/react (Solar)             | 트리 셰이킹되는 React 컴포넌트, 오프라인에서도 동작         |
| PWA·푸시        | Serwist + Web Push                     | 서비스 워커, 오프라인 화면, 소비기한·재구매 알림            |
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
| `/onboarding`    | 온보딩         | 가족 구성, 식습관·알레르기 입력 (RHF + Zod)      |
| `/fridge`        | 내 냉장고      | 재료 목록, 보관 위치·소비기한 정렬               |
| `/fridge/[id]`   | 재료 상세·수정 | 수량·소비기한 수정                               |
| `/fridge/add`    | 재료 등록      | 직접 입력 / 영수증 촬영 선택                     |
| `/receipt`       | 영수증 OCR     | 업로드 → 처리 상태 polling → 인식 결과 확인·수정 |
| `/recipes`       | 레시피 추천    | 보유 재료 기반 AI 추천                           |
| `/recipes/[id]`  | 레시피 상세    | 재료·조리 순서, 부족 재료                        |
| `/recipes/new`   | 커스텀 레시피  | 레시피 직접 작성 (RHF + Zod)                     |
| `/meal-plan`     | 식단표         | 주간 캘린더, AI 식단 생성 상태 polling           |
| `/notifications` | 알림           | 소비기한·재구매 알림 목록, 푸시 권한 설정        |
| `/mypage`        | 마이페이지     | 프로필, 설정                                     |

## 5. 데이터 흐름

```
Spring API ──(OpenAPI 문서)──> pnpm gen:api ──> lib/api/schema.d.ts
                                                     │
화면(page.tsx) ─> _components ─> _lib/queries.ts 또는 @/lib/queries (useQuery) ─> lib/api/client.ts ─> Spring API
                                                     │
                              개발 중 NEXT_PUBLIC_API_MOCKING=true ─> MSW 더미 응답
```

- **서버 데이터**는 전부 TanStack Query로 관리합니다. Zustand에 서버 데이터를 복사하지 않습니다.
- **오래 걸리는 작업**(OCR, AI 식단 생성)은 백엔드가 작업 ID를 주고, 프론트는 `refetchInterval`로 상태를 polling합니다. 완료되면 polling을 멈춥니다.
- **queryKey**는 Query 훅과 같은 곳에 둡니다. 라우트의 `_lib/queries.ts`, 공유되면 `lib/queries/<도메인>.ts`. 예: `fridgeKeys.all`, `fridgeKeys.detail(id)`

## 6. 스타일 규칙

- Tailwind나 CSS-in-JS 라이브러리를 쓰지 않습니다. **CSS Modules(`*.module.css`) + `globals.css`의 CSS 변수**만 씁니다.
- 아이콘은 `@solar-icons/react`의 스타일별 경로에서 import합니다. 예: `import { HomeIcon } from "@solar-icons/react/linear"`. 기본 크기·색·선 두께는 `globals.css`의 `--solar-*` 변수로 정합니다.
- 색, 간격, 글자 크기, radius는 `globals.css`의 토큰(`--color-*`, `--space-*`, `--font-size-*`, `--radius-*`)을 씁니다. 하드코딩한 값을 새로 만들지 않습니다.
- 모바일 우선입니다. 기본 레이아웃 최대 폭은 `--layout-max-width`(480px), 좌우 여백은 `--layout-gutter`입니다.
- 스타일 파일은 컴포넌트와 같은 폴더에 같은 이름으로 둡니다. 예: `IngredientCard.tsx` + `IngredientCard.module.css`

## 7. PWA · 푸시 · TWA

- 서비스 워커는 `pnpm build && pnpm start`에서만 켜집니다. 개발 모드와 MSW 사용 중에는 꺼집니다(같은 scope 충돌 방지).
- 푸시: 프론트가 `NEXT_PUBLIC_VAPID_PUBLIC_KEY`로 구독을 만들어 백엔드에 저장하고, 백엔드가 발송합니다. 수신·클릭 처리는 `app/sw.ts`에서 합니다.
- TWA 배포 전 준비물: 192/512 PNG 아이콘, `public/.well-known/assetlinks.json`(앱 서명 SHA-256), HTTPS 도메인

## 8. 개발 흐름

1. 백엔드 API가 없으면 `mocks/handlers.ts`에 더미 응답을 먼저 만들고 화면을 개발합니다.
2. 백엔드 API가 나오면 `pnpm gen:api`로 타입을 다시 생성하고 MSW 핸들러를 실제 스펙에 맞춥니다.
3. PR 전에 `pnpm lint && pnpm typecheck && pnpm build`가 통과해야 합니다.

## 9. 남은 결정 사항

- [ ] 인증 방식 (쿠키 세션 / JWT)과 토큰 갱신 흐름
- [ ] 공용 컴포넌트 목록과 디자인 토큰 확정 (디자인 시안 기준)
- [ ] 프론트 CI (GitHub Actions: lint · typecheck · build)
- [ ] 배포 환경 (nginx + Node 컨테이너 구성)
