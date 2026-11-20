# REMEKE — AI 에이전트 가이드 (한글 번역)

> 루트 [`AGENTS.md`](../../AGENTS.md)의 한글 번역본입니다. AI 에이전트는 영문 원본을 읽고, 개발자는 이 문서로 에이전트가 어떤 규칙으로 작업하는지 확인합니다. 두 문서는 항상 같은 내용을 유지합니다.

AI 코딩 에이전트(Claude Code, Codex 등)가 이 저장소에서 작업할 때 따르는 규칙입니다.
브랜치를 만들거나 옮길 때, 커밋할 때, Push하거나 PR을 만들 때, 문서를 고칠 때마다 **매번** 이 규칙을 확인합니다.

## 저장소 구조

| 경로 | 내용 | 세부 규칙 |
| --- | --- | --- |
| `backend/` | Spring Boot API 서버 | [`backend-convention.md`](backend-convention.md) (컨벤션), [`erd.md`](erd.md) (ERD), [`backend-testing.md`](backend-testing.md) (테스트) |
| `frontend/` | Next.js PWA | `frontend/AGENTS.md` (있는 경우) |
| `deploy/`, `compose.prod.yml` | 배포 설정 | [`deployment.md`](deployment.md) |
| `docs/agent/` | AI 에이전트용 문서 (영문) | 5장 참고 |
| `docs/developer/` | 개발자용 문서 (한글) | 5장 참고 |
| `.github/` | CI 워크플로, PR·이슈 템플릿 | — |

하위 폴더에 `AGENTS.md`가 있으면 그 폴더에서 작업할 때 이 문서와 함께 따릅니다.

`backend/`나 `frontend/`에서 작업할 때는 [배포 구성 때문에 앱에 필요한 것](deployment.md#배포-구성-때문에-앱에-필요한-것)도 확인합니다. 팀 규칙이 아니라 배포 구성에서 나온 조건을 모아 둔 표입니다(예: Spring Security를 추가할 때 헬스체크 경로를 인증 없이 허용, 새 환경 변수는 `compose.prod.yml`에도 추가하고 머지 전에 배포 담당자가 서버 `.env`에 넣기).

---

## 1. 브랜치

자세한 내용: [`git-convention.md`](git-convention.md)

- `main`은 배포 가능한 안정 버전, `dev`는 통합·테스트 브랜치이고, 나머지는 작업 브랜치입니다.
- 작업 브랜치 이름은 `<유형>/<짧은-설명>` 형식이고, 설명은 영어 소문자 kebab-case로 씁니다. 이슈가 있는 작업이면 이슈 번호를 앞에 붙입니다: `<유형>/<이슈번호>-<짧은-설명>` (예: `feat/12-fridge-list`).
  - `feat/` 신규 기능 (`feat/login-implementation`)
  - `fix/` 버그 수정 (`fix/login-error`)
  - `chore/` 환경 설정·유지보수 (`chore/eslint-config`)
  - `docs/` 문서 작업 (`docs/git-convention`)
- 2026년 12월 10일에 끝나는 짧은 프로젝트이므로 `release`, `hotfix` 브랜치는 쓰지 않습니다.
- **`main`, `dev`에 직접 커밋하지 않습니다.** 둘 중 하나에 있으면 먼저 작업 브랜치를 만듭니다.
- 작업 브랜치는 항상 최신 `dev`에서 만듭니다: `git switch dev && git pull origin dev && git switch -c <유형>/<이름>`
- 브랜치 하나에서는 주요 작업 하나만 합니다. 관련 없는 작업은 브랜치를 나눕니다.
- 가능하면 브랜치 유형과 커밋 유형을 맞춥니다.
- 브랜치를 옮겨야 하는데 커밋하지 않은 변경이 있으면 사용자에게 알리고 묻습니다. 임의로 `stash`, `reset`, `checkout -- .`을 하지 않습니다.

## 2. 커밋

- 형식은 제목 한 줄, 빈 줄, 본문 순서입니다. **모든 커밋에 본문을 씁니다.**

  ```
  <유형>: <작업내용>

  - <무엇을 왜 바꿨는지 한국어로>
  - <눈에 띄는 변경마다 한 줄씩>
  ```

- 제목은 `<유형>: <작업내용>` 형식이고, 콜론 뒤에 공백을 한 칸 둡니다.
- 유형은 `feat`, `fix`, `chore`, `docs` 넷 중 하나만 씁니다. `refactor`, `style`, `test` 등 다른 유형은 쓰지 않습니다.
- 작업 내용은 **한국어로** 짧고 구체적으로 씁니다.
  - 좋은 예: `fix: 소비기한 D-day 계산 오류 수정`, `chore: Prettier 줄 길이 100자로 변경`
  - 나쁜 예(모호함): `fix: 수정`, `chore: update`, `feat: 작업`
- 본문은 한국어 목록(`- `)으로 무엇을 왜 바꿨는지 씁니다. 바뀐 파일, 동작, 리뷰어가 알아야 할 점을 적고, 제목을 반복하지 않습니다. `git commit -F <파일>`이나 `-m`을 여러 번 써서 작성합니다.
- Push 전에 브랜치의 모든 커밋에 본문이 있는지 확인합니다: `git log --format='%h %s%n%b' origin/dev..HEAD`. 빠진 커밋이 있으면 Push 전에 채웁니다(마지막 커밋은 `git commit --amend`, 이전 커밋은 `git rebase --exec`). 이미 Push한 커밋을 고치려면 `git push --force-with-lease`가 필요하므로 사용자에게 먼저 묻습니다.
- 커밋 하나에는 주요 변경 하나만 담습니다. 성격이 다른 변경이 섞여 있으면 파일을 골라서 스테이징하고 여러 번 커밋합니다. 관련 없는 파일을 `git add .`로 함께 넣지 않습니다.
- 커밋 전에 `git status`, `git diff --staged`로 확인합니다.
- `.env`, 비밀 값, API 키, 빌드 산출물은 커밋하지 않습니다.
- pre-commit 훅(Husky)이 실패하면 원인을 고쳐서 다시 커밋합니다. `--no-verify`는 절대 쓰지 않습니다.

### AI 표시 금지

`PR Attribution Check` 워크플로(`scripts/check_pr_attribution.py`)는 아래 내용이 있으면 PR을 실패시킵니다.

- 커밋 메시지에 AI 에이전트(Claude, Codex, Copilot, ChatGPT, Gemini 등)를 적은 `Co-Authored-By:` 줄
- PR 본문의 `🤖 Generated with ...` 문구나 `claude-session:` 링크

커밋 작성자는 사용자 본인의 로컬 git 설정을 그대로 쓰고, 에이전트 기본 설정이 넣으라고 해도 이런 줄은 넣지 않습니다.

## 3. Push와 PR

**작업 브랜치를 Push할 때마다, 그 브랜치에 PR 템플릿 양식으로 본문을 쓴 `dev` 대상 PR이 있도록 합니다.**

1. Push: `git push -u origin <브랜치>`. 작업 브랜치에만 Push하고, `main`·`dev`에는 직접 Push하지 않습니다. `--force`는 사용자가 명시적으로 요청할 때만 씁니다.
2. 열린 PR이 있는지 확인: `gh pr list --head <브랜치> --state open`
3. 없으면 새로 만듭니다:
   `gh pr create --base dev --title "<유형>: <작업내용>" --body-file <파일>`
4. 이미 있으면 브랜치의 모든 커밋이 반영되도록 본문을 갱신합니다:
   `gh pr edit <번호> --body-file <파일>`
5. 사용자에게 PR 주소를 알려줍니다.

PR 규칙:

- 대상 브랜치는 항상 `dev`입니다 (`feat/*`, `fix/*`, `chore/*`, `docs/*` 모두).
- 제목은 커밋 형식을 따릅니다. 예: `feat: 로그인 구현`. 브랜치의 주요 변경을 제목으로 씁니다.
- 본문은 **한국어로**, [`.github/pull_request_template.md`](../../.github/pull_request_template.md) 양식을 그대로 따릅니다. 네 개의 제목을 순서대로 두고 모두 채웁니다.

  ```markdown
  ## 변경 내용
  - 작업 목적과 주요 변경 사항 (커밋 단위로 요약)

  ## 관련 이슈
  - Closes #번호  (없으면 "없음")
  - 후속: #번호  (이어서 할 작업 이슈, 없으면 생략)

  ## 테스트
  - 실행한 검사와 결과 (예: pnpm lint 통과, ./gradlew build 통과)
  - 실행하지 못한 검사가 있으면 이유와 함께 그대로 적기

  ## 리뷰 요청
  - 리뷰어가 중점적으로 봐야 할 부분
  ```

- 템플릿의 HTML 주석은 지웁니다. AI 표시 문구는 넣지 않습니다 (2장 참고).
- 에이전트가 PR을 직접 Merge하거나 `dev`를 `main`에 Merge하지 않습니다. 리뷰 후 팀이 합니다.
- 이슈가 있는 작업이면 관련 이슈에 `Closes #번호`를 적고, PR에 이슈와 같은 유형·영역·파트 라벨을 붙입니다. 이슈 템플릿·제목·라벨: [`git-convention.md`](git-convention.md) 7장.
- PR 뒤에 이어서 할 작업이 있으면 머지 전에 후속 이슈를 만들고, 그 작업을 할 사람을 담당자로 지정하고, 관련 이슈에 `후속: #번호`를 적습니다(7장 "후속 작업"). 에이전트가 사용자 대신 PR을 쓸 때는 후속 작업 목록을 보여 주고, 이슈를 만들기 전에 누구를 지정할지 사용자에게 묻습니다.

## 4. 커밋·Push 전 검사

변경한 영역의 폴더에서 아래 검사를 실행합니다.

| 영역 | 명령 |
| --- | --- |
| `frontend/` | `pnpm lint`, `pnpm typecheck`, `pnpm build` |
| `backend/` | `./gradlew build` (Docker가 켜져 있어야 함. 테스트가 PostgreSQL을 컨테이너로 띄움) |
| `.github/workflows/` | `docker run --rm -v "$PWD:/repo:ro" -w /repo rhysd/actionlint:1.7.12 -color .github/workflows/*.yml` (저장소 루트에서) |

PR에서 CI가 무엇을 검사하고 결과를 어떻게 보는지는 [`docs/developer/ci.md`](ci.md)를 봅니다.

실행하지 못한 검사가 있으면 사용자에게 알리고, PR의 `## 테스트`에 적습니다.

## 5. 문서 (`docs/agent` ↔ `docs/developer`)

문서는 두 벌로 나란히 관리합니다.

| 폴더 | 독자 | 언어 | 작성 방식 |
| --- | --- | --- | --- |
| `docs/agent/` | AI 에이전트 | **영어** | 명령형 규칙, 정확한 명령어·경로, 군더더기 없이 |
| `docs/developer/` | 팀 개발자 | **한국어** | 같은 내용을 자연스러운 한국어로 설명 |

규칙:

- 모든 문서는 두 폴더에 **같은 파일 이름으로** 있습니다. 예: `docs/agent/git-convention.md` ↔ `docs/developer/git-convention.md`
- Markdown 문서를 만들거나, 고치거나, 이름을 바꾸거나, 지울 때는 짝 문서에도 같은 변경을 **같은 커밋에서** 반영합니다. 개발자용 문서는 요약이 아니라 충실한 한글 번역이어야 합니다.
- 팀원이 한국어로 쓴 문서는 `docs/developer/`에 두고, 영문판을 `docs/agent/`에 작성합니다.
- `AGENTS.md` 파일은 에이전트용 문서(영문)이고, 한글 번역은 `docs/developer/`에 둡니다.
  - 루트 `AGENTS.md` ↔ `docs/developer/agent-guide.md`
  - `<폴더>/AGENTS.md` ↔ `docs/developer/<폴더>-agent-guide.md`
- 에이전트가 제자리에서 읽어야 해서 `docs/` 밖에 있는 문서(예: `frontend/blueprint.md`)도 같은 방식입니다. 원래 위치에는 영문을 두고, 한글 번역은 `docs/developer/<폴더>-<이름>.md`에 둡니다 (예: `docs/developer/frontend-blueprint.md`).
- `CLAUDE.md` 파일에는 `@AGENTS.md` 한 줄만 둡니다. Claude Code가 같은 규칙을 읽게 하기 위한 파일이라 다른 내용을 넣지 않습니다.
- 대상이 아닌 문서(한글로만 관리, 영문판 없음): `README.md` 파일들, `.github/` 템플릿
- 링크는 각 문서 위치에서 동작하는 상대 경로로 씁니다.

현재 문서 짝:

| 에이전트용 (영문) | 개발자용 (한글) |
| --- | --- |
| `AGENTS.md` | `docs/developer/agent-guide.md` |
| `docs/agent/git-convention.md` | `docs/developer/git-convention.md` |
| `docs/agent/deployment.md` | `docs/developer/deployment.md` |
| `docs/agent/ci.md` | `docs/developer/ci.md` |
| `docs/agent/backend-convention.md` | `docs/developer/backend-convention.md` |
| `docs/agent/erd.md` | `docs/developer/erd.md` |
| `docs/agent/backend-testing.md` | `docs/developer/backend-testing.md` |
| `frontend/AGENTS.md` | `docs/developer/frontend-agent-guide.md` |
| `frontend/blueprint.md` | `docs/developer/frontend-blueprint.md` |

새 짝을 추가하면 이 표와 `AGENTS.md`의 표에 모두 한 줄씩 추가합니다.
