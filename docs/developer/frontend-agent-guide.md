# REMAKE 프론트엔드 — AI 에이전트 가이드 (한글 번역)

> [`frontend/AGENTS.md`](../../frontend/AGENTS.md)의 한글 번역본입니다. AI 에이전트는 영문 원본을 읽고, 개발자는 이 문서로 에이전트가 프론트엔드에서 어떤 규칙으로 작업하는지 확인합니다. 두 문서는 항상 같은 내용을 유지합니다.
>
> 원본 맨 위의 `nextjs-agent-rules` 블록은 `next dev`가 자동으로 넣는 안내문입니다. 이 버전의 Next.js는 기존 지식과 다를 수 있으니 코드를 쓰기 전에 `node_modules/next/dist/docs/`의 문서를 확인하라는 내용입니다.

AI 코딩 에이전트(Claude Code, Codex 등)가 `frontend/`에서 작업할 때 따르는 규칙입니다. 루트 `AGENTS.md`와 함께 따릅니다.

## 먼저 읽기

- 작업 전에 [blueprint.md](../../frontend/blueprint.md)(한글판: [frontend-blueprint.md](frontend-blueprint.md))를 읽고 기술 스택, 폴더 구조, 라우트, 데이터 흐름을 확인합니다.
- blueprint.md와 다른 구조나 라이브러리가 필요하면 코드를 바꾸기 전에 사람에게 먼저 묻습니다.

## 필수 규칙

- 패키지 매니저는 **pnpm**만 씁니다. `npm`, `yarn`, `npx`를 쓰지 않습니다 (`pnpm dlx`, `pnpm exec` 사용).
- 명령은 `frontend/` 폴더에서 실행합니다. git 루트는 한 단계 위(`REMAKE/`)입니다.
- **Tailwind를 쓰지 않습니다.** 스타일은 CSS Modules(`*.module.css`)와 `app/globals.css`의 CSS 변수(디자인 토큰)로만 작성합니다. Tailwind, shadcn/ui, styled-components, emotion 같은 스타일 라이브러리를 추가하지 않습니다.
- 서버 데이터는 TanStack Query로만 다룹니다. Zustand에는 로그인 정보 같은 최소한의 클라이언트 상태만 둡니다.
- API 호출은 `lib/api/client.ts`(openapi-fetch)를 씁니다. `lib/api/schema.d.ts`는 `pnpm gen:api`로 생성하는 파일이므로 직접 수정하지 않습니다.
- 백엔드 기능을 Next Route Handler나 Server Action으로 대신 구현하지 않습니다. 비즈니스 로직은 Spring 백엔드 담당입니다.
- 환경 변수는 `lib/env.ts`에서만 읽습니다. 새 변수를 추가하면 `.env.example`에도 추가합니다.
- 아이콘은 Solar(`@solar-icons/react/<스타일>`)만 씁니다. lucide 등 다른 아이콘 라이브러리를 추가하지 않습니다.
- 날짜는 `lib/dayjs.ts`에서 export한 `dayjs`를 씁니다 (ko 로케일 설정됨).
- 폼은 React Hook Form + Zod(`@hookform/resolvers/zod`)로 만듭니다.
- 백엔드 API가 아직 없으면 `mocks/handlers.ts`에 MSW 핸들러를 추가해서 개발합니다.

## 코드 위치

- 라우트(`app/**/page.tsx`)는 얇게 두고, 실제 UI·로직은 `features/<기능>/`에 둡니다.
- 한 기능에서만 쓰는 코드는 그 기능 폴더에, 두 기능 이상에서 쓰면 `components`·`hooks`·`lib`로 올립니다.
- 다른 기능 폴더의 내부 파일을 직접 import하지 않고 그 기능의 `index.ts`를 통해 씁니다.
- import 경로는 `@/` 별칭을 씁니다 (`@/*` → `./*`).

## 작업 후 확인

변경 후 아래 명령이 모두 통과해야 합니다.

```bash
pnpm lint
pnpm typecheck
pnpm build
```

- ESLint 규칙은 `eslint.config.mjs`에 있습니다 (import 정렬, type import, dayjs·process.env 직접 사용 금지, 다른 feature 내부 import 금지 등). 규칙을 끄는 `eslint-disable` 주석은 이유를 함께 적을 때만 씁니다.
- 포맷은 Prettier가 담당합니다. 커밋 시 Husky + lint-staged가 자동으로 실행됩니다. `--no-verify`로 훅을 건너뛰지 않습니다.
- 브랜치·커밋·PR은 루트 `AGENTS.md`의 Git 컨벤션을 따릅니다 (커밋 유형은 `feat`, `fix`, `chore`, `docs`만 사용, 모든 커밋에 본문 작성).
