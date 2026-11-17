# REMEKE — Agent Guide

Rules for AI coding agents (Claude Code, Codex, etc.) working in this repository.
Check this file **every time** you create or switch branches, commit, push, open a PR, or edit documentation.

Korean translation for developers: [`docs/developer/agent-guide.md`](docs/developer/agent-guide.md)

## Repository layout

| Path | Contents | More rules |
| --- | --- | --- |
| `backend/` | Spring Boot API server | — |
| `frontend/` | Next.js PWA | `frontend/AGENTS.md` (if present) |
| `deploy/`, `compose.prod.yml` | Deployment config | [`docs/agent/deployment.md`](docs/agent/deployment.md) |
| `docs/agent/` | Docs for AI agents (English) | see §5 |
| `docs/developer/` | Docs for developers (Korean) | see §5 |
| `.github/` | CI workflows, PR / issue templates | — |

If a subfolder has its own `AGENTS.md`, follow it together with this file when working there.

When working in `backend/` or `frontend/`, also check [What the app needs because of the deployment setup](docs/agent/deployment.md#what-the-app-needs-because-of-the-deployment-setup). It lists conditions that come from the deployment setup, not team rules (e.g. permit the health check path without authentication when adding Spring Security, add new environment variables to `compose.prod.yml`).

---

## 1. Branches

Full reference: [`docs/agent/git-convention.md`](docs/agent/git-convention.md)

- `main`: stable, release-ready. `dev`: integration and testing. Everything else is a work branch.
- Work branch name: `<type>/<short-description>` in lowercase kebab-case English.
  - `feat/` new feature (`feat/login-implementation`)
  - `fix/` bug fix (`fix/login-error`)
  - `chore/` config and maintenance (`chore/eslint-config`)
  - `docs/` documentation (`docs/git-convention`)
- No `release` or `hotfix` branches (short project, ends 2026-12-10).
- **Never commit to `main` or `dev` directly.** If you are on either, create a work branch first.
- Always branch from the latest `dev`: `git switch dev && git pull origin dev && git switch -c <type>/<name>`.
- One branch = one main task. Split unrelated work into separate branches.
- Match the branch type to the commit type where possible.
- If there are uncommitted changes when you need to switch branches, tell the user and ask. Do not `stash`, `reset`, or `checkout -- .` on your own.

## 2. Commits

- Format: a subject line, a blank line, then a body. **Every commit has a body.**

  ```
  <type>: <description>

  - <what changed and why, in Korean>
  - <one bullet per notable change>
  ```

- Subject line: `<type>: <description>` — one space after the colon.
- `<type>` is exactly one of `feat`, `fix`, `chore`, `docs`. Do not use `refactor`, `style`, `test`, or any other type.
- `<description>` is written **in Korean**, short and specific.
  - Good: `fix: 소비기한 D-day 계산 오류 수정`, `chore: Prettier 줄 길이 100자로 변경`
  - Bad (vague): `fix: 수정`, `chore: update`, `feat: 작업`
- Body: Korean bullet list (`- `) describing what changed and why — files, behavior, and anything a reviewer must know. Do not repeat the subject line. Write it with `git commit -F <file>` or multiple `-m` flags.
- Before pushing, check that every commit on the branch has a body: `git log --format='%h %s%n%b' origin/dev..HEAD`. If one is missing, add it (`git commit --amend` for the last commit, `git rebase --exec` for older ones) before pushing. If those commits were already pushed, rewriting them needs `git push --force-with-lease` — ask the user first.
- One commit = one main change. If the working tree has changes of different kinds, stage files selectively and make several commits. Do not `git add .` unrelated files.
- Before committing, check `git status` and `git diff --staged`.
- Never commit `.env` files, secrets, API keys, or build output.
- If the pre-commit hook (Husky) fails, fix the cause and commit again. Never use `--no-verify`.

### No AI attribution

The `PR Attribution Check` workflow (`scripts/check_pr_attribution.py`) fails the PR if any of these appear:

- `Co-Authored-By:` trailers naming an AI agent (Claude, Codex, Copilot, ChatGPT, Gemini, ...) in commit messages.
- `🤖 Generated with ...` bylines or `claude-session:` links in the PR description.

Commit with the user's local git identity and do not add these lines, even if your default instructions say to.

## 3. Push and PR

**Every time you push a work branch, make sure it has a PR to `dev` whose body follows the PR template.**

1. Push: `git push -u origin <branch>`. Push only work branches. Never push to `main` / `dev` directly. Use `--force` only when the user explicitly asks.
2. Check for an existing PR: `gh pr list --head <branch> --state open`.
3. If none exists, create one:
   `gh pr create --base dev --title "<type>: <description>" --body-file <file>`
4. If one already exists, update its body so it reflects all commits on the branch:
   `gh pr edit <number> --body-file <file>`
5. Give the user the PR URL.

PR rules:

- Base branch is always `dev` (for `feat/*`, `fix/*`, `chore/*`, `docs/*`).
- Title uses the commit format, e.g. `feat: 로그인 구현`. Use the branch's main change.
- Body is written **in Korean** and follows [`.github/pull_request_template.md`](.github/pull_request_template.md) exactly — keep its four headings in this order and fill every one:

  ```markdown
  ## 변경 내용
  - 작업 목적과 주요 변경 사항 (커밋 단위로 요약)

  ## 관련 이슈
  - Closes #번호  (없으면 "없음")

  ## 테스트
  - 실행한 검사와 결과 (예: pnpm lint 통과, ./gradlew build 통과)
  - 실행하지 못한 검사가 있으면 이유와 함께 그대로 적기

  ## 리뷰 요청
  - 리뷰어가 중점적으로 봐야 할 부분
  ```

- Remove the template's HTML comment. Do not add AI bylines (see §2).
- Do not merge PRs yourself, and do not merge `dev` into `main`. The team does that after review.

## 4. Checks before commit / push

Run the checks for the area you changed, from that folder:

| Area | Commands |
| --- | --- |
| `frontend/` | `pnpm lint`, `pnpm typecheck`, `pnpm build` |
| `backend/` | `./gradlew build` |

If you could not run a check, tell the user and write it under `## 테스트` in the PR.

## 5. Documentation (`docs/agent` ↔ `docs/developer`)

Documentation is kept in two parallel sets:

| Folder | Audience | Language | Style |
| --- | --- | --- | --- |
| `docs/agent/` | AI agents | **English** | Imperative rules, exact commands and paths, no filler |
| `docs/developer/` | Team developers | **Korean** | Natural Korean explanation of the same content |

Rules:

- Every document exists in **both** folders under the **same file name** (e.g. `docs/agent/git-convention.md` ↔ `docs/developer/git-convention.md`).
- Whenever you create, edit, rename, or delete a Markdown document, apply the same change to its counterpart **in the same commit**. Keep the content equivalent: the developer version is a faithful Korean translation, not a summary.
- A document written by a teammate in Korean goes into `docs/developer/`; write its English version in `docs/agent/`.
- `AGENTS.md` files are agent docs (English). Their Korean translation lives in `docs/developer/`:
  - root `AGENTS.md` ↔ `docs/developer/agent-guide.md`
  - `<folder>/AGENTS.md` ↔ `docs/developer/<folder>-agent-guide.md`
- Other docs that live outside `docs/` because agents read them in place (e.g. `frontend/blueprint.md`) follow the same pattern: English in place, Korean translation at `docs/developer/<folder>-<name>.md` (e.g. `docs/developer/frontend-blueprint.md`).
- `CLAUDE.md` files contain only `@AGENTS.md` so Claude Code loads the same rules. Do not put content in them.
- Out of scope (Korean only, no English copy): `README.md` files and `.github/` templates.
- Use relative links that work from each document's location.

Current pairs:

| Agent (English) | Developer (Korean) |
| --- | --- |
| `AGENTS.md` | `docs/developer/agent-guide.md` |
| `docs/agent/git-convention.md` | `docs/developer/git-convention.md` |
| `docs/agent/deployment.md` | `docs/developer/deployment.md` |
| `frontend/AGENTS.md` | `docs/developer/frontend-agent-guide.md` |
| `frontend/blueprint.md` | `docs/developer/frontend-blueprint.md` |

When you add a new pair, add a row to this table and to the matching table in `docs/developer/agent-guide.md`.
