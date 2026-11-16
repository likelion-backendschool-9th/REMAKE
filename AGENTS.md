# REMEKE — Agent Guide

AI 코딩 에이전트(Claude Code, Codex 등)가 이 저장소에서 작업할 때 따르는 규칙입니다.
특히 **브랜치를 만들거나 이동할 때, 커밋할 때, Push하거나 PR을 만들 때마다** 아래 Git 컨벤션을 확인하고 그대로 따릅니다.

## 저장소 구조

| 경로 | 내용 | 세부 규칙 |
| --- | --- | --- |
| `backend/` | Spring Boot API 서버 | - |
| `frontend/` | Next.js PWA | `frontend/AGENTS.md` |
| `deploy/`, `compose.prod.yml` | 배포 설정 | `docs/deployment.md` |
| `docs/` | 프로젝트 문서 | - |
| `.github/` | CI 워크플로, PR·이슈 템플릿 | - |

하위 폴더에 `AGENTS.md`가 있으면 그 폴더에서 작업할 때 이 문서와 함께 따릅니다.

---

## 1. 브랜치 컨벤션 (Git Flow)

### 브랜치 구성

- `main`: 최종 배포 가능한 안정 버전
- `dev`: 팀 개발 내용을 통합하고 테스트하는 브랜치
- 작업 브랜치: 기능 개발, 버그 수정, 환경 설정, 문서 작업을 하는 브랜치

2주 프로젝트이므로 `release`, `hotfix` 브랜치는 쓰지 않습니다.

### 작업 브랜치 이름

형식: `<유형>/<작업-내용>` — 작업 내용은 영어 소문자 kebab-case로 짧게 씁니다.

| 유형 | 사용 목적 | 예시 |
| --- | --- | --- |
| `feat` | 신규 기능 개발 | `feat/login-implementation` |
| `fix` | 버그 수정 | `fix/login-error` |
| `chore` | 환경 설정 및 기타 유지보수 | `chore/eslint-config` |
| `docs` | 문서 작성 및 수정 | `docs/git-convention` |

### 브랜치 규칙

- `main`, `dev`에 **직접 커밋하지 않습니다.** 현재 브랜치가 `main`이나 `dev`이면 작업 브랜치를 먼저 만듭니다.
- 작업 브랜치는 **항상 최신 `dev`에서** 만듭니다.
- 하나의 브랜치에서는 하나의 주요 작업만 합니다. 성격이 다른 작업이 섞이면 브랜치를 나눕니다.
- 작업 내용에 맞는 유형을 고르고, 가능하면 브랜치 유형과 커밋 유형을 일치시킵니다.
- Merge가 끝난 작업 브랜치는 원격·로컬 모두 삭제합니다.

## 2. 커밋 컨벤션

### 형식

```
<유형>: <작업내용>
```

- 유형과 작업 내용은 콜론(`:`)으로 구분하고, 콜론 뒤에 공백을 한 칸 둡니다.
- 유형은 `feat`, `fix`, `chore`, `docs` **넷 중 하나만** 씁니다. (`refactor`, `style`, `test` 등 다른 유형은 쓰지 않습니다.)
- 작업 내용은 한국어로 짧고 명확하게 씁니다.

| 유형 | 사용 목적 | 예시 |
| --- | --- | --- |
| `feat` | 신규 기능 개발 | `feat: 로그인 구현` |
| `fix` | 버그 수정 | `fix: 로그인 오류 수정` |
| `chore` | 환경 설정 및 기타 유지보수 | `chore: ESLint 설정 변경` |
| `docs` | 문서 작성 및 수정 | `docs: Git Flow 문서 작성` |

### 커밋 규칙

- 하나의 커밋에는 하나의 주요 변경 사항만 담습니다. 여러 성격의 변경이 있으면 파일 단위로 나눠서 여러 번 커밋합니다.
- `수정`, `작업`, `update`처럼 의미가 모호한 메시지는 쓰지 않습니다.
  - ❌ `fix: 수정`, `chore: update`, `feat: 작업`
  - ✅ `fix: 소비기한 D-day 계산 오류 수정`, `chore: Prettier 줄 길이 100자로 변경`
- 커밋 전에 `git status`와 `git diff --staged`로 의도한 파일만 들어갔는지 확인합니다. `git add .`로 관련 없는 파일을 함께 넣지 않습니다.
- `.env`, 비밀번호, API 키, 빌드 산출물은 커밋하지 않습니다.
- pre-commit 훅(Husky)이 실패하면 원인을 고쳐서 다시 커밋합니다. `--no-verify`로 건너뛰지 않습니다.

### AI 에이전트 표시 금지

`PR Attribution Check` CI(`scripts/check_pr_attribution.py`)가 아래 내용이 있는 PR을 실패시킵니다.

- 커밋 메시지에 `Co-Authored-By: Claude ...` 같은 AI 공동 작성자 줄을 넣지 않습니다.
- PR 본문에 `🤖 Generated with Claude Code` 같은 문구나 `claude-session:` 링크를 넣지 않습니다.
- 커밋 작성자는 로컬 git 설정(사용자 본인)을 그대로 씁니다.

## 3. 작업 흐름

에이전트는 작업을 시작할 때 아래 순서를 따릅니다.

```bash
# 1. 최신 dev로 이동해서 변경 사항 가져오기
git switch dev
git pull origin dev

# 2. 작업 유형에 맞는 브랜치 만들기
git switch -c feat/login-implementation

# 3. 작업 후 커밋 (변경 성격별로 나눠서)
git add <관련 파일>
git commit -m "feat: 로그인 구현"

# 4. 원격 저장소에 Push
git push -u origin feat/login-implementation

# 5. dev 대상으로 PR 생성
```

- 브랜치를 옮기기 전에 커밋하지 않은 변경이 있으면 사용자에게 먼저 알리고 어떻게 할지 묻습니다. 임의로 `stash`, `reset`, `checkout -- .` 하지 않습니다.
- Push는 작업 브랜치에만 합니다. `main`, `dev`에 직접 Push하지 않고, `--force`는 사용자가 명시적으로 요청할 때만 씁니다.
- `dev`를 `main`에 Merge하는 일은 개발이 끝난 뒤 팀이 결정합니다. 에이전트가 임의로 하지 않습니다.

## 4. PR 및 Merge 규칙

- 모든 작업 브랜치는 **`dev`를 대상으로** PR을 만듭니다.

  | 작업 브랜치 | PR 대상 |
  | --- | --- |
  | `feat/*`, `fix/*`, `chore/*`, `docs/*` | `dev` |

- PR 제목은 커밋 컨벤션과 같은 형식을 씁니다. 예: `feat: 로그인 구현`
- PR 본문은 `.github/pull_request_template.md` 양식(변경 내용, 관련 이슈, 테스트, 리뷰 요청)을 채웁니다. 작업 목적, 주요 변경 사항, 테스트 결과를 꼭 적습니다.
- 테스트하지 못한 부분이 있으면 PR 본문에 그대로 적습니다.
- 팀원 리뷰와 확인이 끝나면 Merge합니다. 에이전트가 직접 Merge하지 않습니다.

## 5. 커밋 전 확인

변경한 영역의 검사를 통과한 뒤 커밋·Push합니다.

| 영역 | 명령 (해당 폴더에서 실행) |
| --- | --- |
| `frontend/` | `pnpm lint`, `pnpm typecheck`, `pnpm build` |
| `backend/` | `./gradlew build` |

실행하지 못한 검사가 있으면 사용자에게 알리고, PR 본문에도 적습니다.
