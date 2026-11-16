# REMAKE Frontend

Next.js (App Router) + TypeScript 기반 PWA. 패키지 매니저는 **pnpm**만 사용합니다.
설계는 [blueprint.md](./blueprint.md) (한글판: [docs/developer/frontend-blueprint.md](../docs/developer/frontend-blueprint.md)), AI 에이전트 규칙은 [AGENTS.md](./AGENTS.md) (한글판: [docs/developer/frontend-agent-guide.md](../docs/developer/frontend-agent-guide.md))를 참고하세요.

## 시작하기

```bash
corepack enable          # package.json의 packageManager(pnpm 버전)를 자동으로 맞춤
cd frontend
pnpm install             # 설치 시 Husky pre-commit 훅도 자동 등록
cp .env.example .env.local
pnpm dev                 # http://localhost:3000
```

## 스크립트

| 명령                                | 설명                                                          |
| ----------------------------------- | ------------------------------------------------------------- |
| `pnpm dev`                          | 개발 서버                                                     |
| `pnpm build` / `pnpm start`         | 프로덕션 빌드 / 실행 (`output: "standalone"`)                 |
| `pnpm lint` / `pnpm lint:fix`       | ESLint                                                        |
| `pnpm format` / `pnpm format:check` | Prettier                                                      |
| `pnpm typecheck`                    | 라우트 타입 생성 + `tsc`                                      |
| `pnpm gen:api`                      | Spring OpenAPI(`/v3/api-docs`)에서 `lib/api/schema.d.ts` 생성 |

## 기술 스택

| 영역            | 기술                                   |
| --------------- | -------------------------------------- |
| 프레임워크      | Next.js (App Router) + TypeScript      |
| 패키지 매니저   | pnpm                                   |
| 스타일          | CSS Modules + CSS 변수                 |
| 아이콘          | Solar (@solar-icons/react)             |
| 서버 데이터     | TanStack Query                         |
| 클라이언트 상태 | Zustand                                |
| API 연동        | openapi-typescript + openapi-fetch     |
| 폼              | React Hook Form + Zod                  |
| 날짜            | dayjs                                  |
| PWA·푸시        | Serwist + Web Push                     |
| 스토어 배포     | PWABuilder (TWA)                       |
| 코드 품질       | ESLint + Prettier, Husky + lint-staged |
| API 목업        | MSW                                    |

## 폴더 구조

```
frontend/
├─ app/                 # 라우트(화면). sw.ts = 서비스 워커, serwist/ = SW 서빙 라우트, ~offline = 오프라인 화면
├─ components/          # 공용 UI 컴포넌트 (*.tsx + *.module.css)
├─ features/            # 기능별 코드 (예: features/fridge, features/recipe, features/meal-plan)
├─ hooks/               # 공용 훅
├─ lib/                 # api 클라이언트, query-client, dayjs, env 등
├─ mocks/               # MSW 핸들러
├─ providers/           # 전역 Provider (TanStack Query, Serwist, MSW)
├─ stores/              # Zustand 스토어
└─ public/              # 정적 파일, MSW 워커(mockServiceWorker.js)
```

## 참고

- `NEXT_PUBLIC_API_MOCKING=true`이면 MSW가 켜지고, 백엔드 없이 `mocks/handlers.ts`의 더미 응답으로 개발할 수 있습니다.
- 서비스 워커(Serwist)는 개발 모드와 MSW 사용 중에는 꺼집니다. PWA 동작은 `pnpm build && pnpm start`로 확인하세요.
- `pnpm gen:api`를 쓰려면 백엔드에 springdoc-openapi가 필요합니다.
- TWA 배포 시 `public/.well-known/assetlinks.json`을 추가해야 합니다 (PWABuilder에서 발급한 SHA-256 지문 사용).
