# Git Convention

> Korean version for developers: [`docs/developer/git-convention.md`](../developer/git-convention.md)

## 1. Branches

- `main`: stable, release-ready version.
- `dev`: integrates and tests the team's work.
- Work branches: features, bug fixes, configuration, and documentation.

Create work branches by task type. Name format: `<type>/<task-description>`, where the description is short lowercase kebab-case English. If the work has an issue, put the issue number first: `<type>/<issue-number>-<task-description>` (for example `feat/12-fridge-list`), so the branch list shows which issue each branch belongs to. Small `chore` or `docs` work without an issue has no number.

| Type | Purpose | Example |
| --- | --- | --- |
| `feat` | New feature | `feat/login-implementation` |
| `fix` | Bug fix | `fix/login-error` |
| `chore` | Configuration and other maintenance | `chore/eslint-config` |
| `docs` | Writing or editing documentation | `docs/git-convention` |

## 2. Workflow

1. Switch to the latest `dev` and pull.
2. Create a work branch of the matching type.
3. Do the work and commit following the commit convention.
4. Push the work branch to the remote.
5. Open a PR from the work branch into `dev`.
6. Review and test the changes, then merge into `dev`.
7. When development is complete, merge `dev` into `main`.

## 3. Rules

- Do not commit directly to `main` or `dev`.
- Choose the branch type that matches the work.
- One branch carries one main task.
- Branch names follow the branch convention.
- Commit messages follow the commit convention.
- Match the branch type and the commit type where possible.
- Every work branch opens its PR against `dev`.
- Review and test changes before merging.
- Delete work branches after they are merged.
- No `release` or `hotfix` branches (short project, ends 2026-12-10).
- Before starting a feature or bug fix, register it as an issue (see §7). One issue = one branch = one PR.

## 4. Examples

### 4-1. New feature (feat)

```bash
git switch dev
git pull origin dev
git switch -c feat/login-implementation

# commit after the work
git add .
git commit -m "feat: 로그인 구현"

# push to the remote
git push -u origin feat/login-implementation
```

### 4-2. Bug fix (fix)

```bash
git switch dev
git pull origin dev
git switch -c fix/login-error

# commit after the work
git add .
git commit -m "fix: 로그인 오류 수정"

# push to the remote
git push -u origin fix/login-error
```

### 4-3. Configuration and maintenance (chore)

```bash
git switch dev
git pull origin dev
git switch -c chore/eslint-config

# commit after the work
git add .
git commit -m "chore: ESLint 설정 변경"

# push to the remote
git push -u origin chore/eslint-config
```

### 4-4. Documentation (docs)

```bash
git switch dev
git pull origin dev
git switch -c docs/git-convention

# commit after the work
git add .
git commit -m "docs: Git 컨벤션 작성"

# push to the remote
git push -u origin docs/git-convention
```

Agents: the examples use `git add .` for brevity. Stage only the files related to the change (see `AGENTS.md` §2). The Korean version tells developers the same.

## 5. PR and merge rules

- Every work branch opens its PR against `dev`.
- The PR title uses the same type format as commits.
- The PR body states the purpose, the main changes, and the test results. Fill every section of the [PR template](../../.github/pull_request_template.md) (변경 내용, 관련 이슈, 테스트, 리뷰 요청).
- PRs into `dev` or `main` must pass the `Backend CI required`, `Frontend CI required`, and `Deploy CI required` checks before merging.
- These checks report a result on every PR. The backend, frontend, deploy config, or workflow check jobs are skipped when that area is unchanged; do not require those individual jobs in branch rulesets. See [`ci.md`](ci.md) for what each check runs.
- Merge after team review and confirmation.
- After merging, delete the work branch both remotely and locally.

| Work branch | PR base |
| --- | --- |
| `feat/*` | `dev` |
| `fix/*` | `dev` |
| `chore/*` | `dev` |
| `docs/*` | `dev` |

- If the work has an issue, write `Closes #<number>` under 관련 이슈 in the PR body. The issue closes automatically when the PR is merged into `dev`.

## 6. Commit convention

### Format

```
<type>: <description>
```

Separate type and description with a colon (`:`) followed by exactly one space.

Put one blank line under the subject, then the body. Every commit has a body.

```
<type>: <description>

- what changed and why
- one line per notable change
```

### Types

| Type | Purpose | Example |
| --- | --- | --- |
| `feat` | New feature | `feat: 로그인 구현` |
| `fix` | Bug fix | `fix: 로그인 오류 수정` |
| `chore` | Configuration and other maintenance | `chore: ESLint 설정` |
| `docs` | Writing or editing documentation | `docs: Git 컨벤션 작성` |

### Rules

- Use exactly one of `feat`, `fix`, `chore`, `docs`.
- Keep the description short and clear. Write it in Korean.
- One commit carries one main change.
- Do not use vague descriptions such as `수정`, `작업`, or `update`.
- Write the body as a Korean bullet list (`- `) of the changes and their reasons. Do not repeat the subject.
- Do not add AI co-author trailers (`Co-Authored-By: Claude ...`) or `Generated with ...` / `Made with ...` bylines (e.g. `Made with Cursor`) to commits or PR bodies. The `PR Attribution Check` CI fails on them.

### Examples

```
feat: 프로젝트 등록 기능 구현
fix: 로그인 오류 수정
chore: ESLint 설정 변경
docs: Git Flow 문서 작성
```

With a body:

```
fix: 소비기한 D-day 계산 오류 수정

- 자정 기준이 아니라 현재 시각 기준으로 계산해 하루씩 밀리던 문제 수정
- dayjs startOf('day')로 날짜만 비교하도록 변경
```

## 7. Issues and labels

### When to open an issue

- Open an issue before you start a feature, a bug fix, or a decision the team must make. A small `chore` or `docs` change may go straight to a PR.
- One issue = one branch = one PR. Split an issue that needs several PRs into several issues.
- Put the issue number in the branch name: `<type>/<issue-number>-<task-description>` (§1, for example `feat/12-fridge-list`). Link the issue from the PR with `Closes #<number>`.
- Assign the issue to the person who does the work.

### Templates

Create issues from the YAML issue forms in `.github/ISSUE_TEMPLATE/`. Blank issues are turned off. Make the situation, requirements, confirmed decisions, scope, concrete work, and open questions distinguishable. Keep area, part, priority, and assignee in GitHub metadata rather than repeating them as body sections.

| Template | Title | Use for |
| --- | --- | --- |
| 기능 개발 | `[feat] <Korean description>` | Use 구현할 것 for the concrete API, server, screen, integration, and test changes |
| 버그 | `[fix] <Korean description>` | Include the actual problem, impact, environment, and reproduction in 배경; expected results in 요구사항; investigation, changes, and regression checks in 구현할 것 |
| 결정 요청 | `[decision] <Korean description>` | Use 결정할 것 for one question, options and effects, a proposal, decision owner, needed timing, and affected documents/issues. After deciding, record the result, rationale, and owner in 정한 것 |
| 설정·문서 작업 | `[chore] <Korean description>` or `[docs] <Korean description>` | Use 할 일 for the concrete configuration, CI, deployment, or paired-document changes and checks |

- Use the shared order **배경 → 요구사항 → 정한 것 → 작업 범위 → 구현할 것/결정할 것/할 일 → 확인할 것 → 참고**. Require 배경, 요구사항, 작업 범위, and the work field; other fields are optional.
- In 배경, explain the current situation and why it needs changing. In 요구사항, list observable outcomes and failure/boundary conditions that determine completion. In 정한 것, record confirmed policy, API contracts, or technical choices with source sections or decision links; omit it if none are confirmed.
- In 작업 범위, distinguish included work from exclusions assigned to other issues. In the work field, use a concrete checklist for implementation tasks, including relevant errors, events, and tests; use option analysis for a decision task. Do not copy the whole spec or repeat the title.
- In 확인할 것, list unresolved questions, source conflicts, and prerequisites with the owner, needed timing, and blocked work. Keep these separate from confirmed decisions and from acceptance criteria. In 참고, link source sections, API definitions, Figma frame names/URLs, and relevant issues/PRs; include tables, `code`, `traceId`, or logs only when useful.
- Name each API by its function, HTTP method, and full path, for example `남은 생성 횟수 조회 — GET /api/v1/user/usage-quotas`. Do not use specification ordinals such as `API 27` as the API name. State the exact behavior and responsibility; keep field names and error/event codes alongside their human-readable meaning when needed.
- Use a system actor for server jobs. Split independent work into separate issues. Do not require unrelated empty fields or identical generic completion checklists.
- Link unresolved policy to a decision issue. Do not turn a proposal or a source conflict into a confirmed requirement while rewriting an issue.
- Template changes affect new issues only. Before migrating existing issues, prepare representative rewritten examples and a before/after snapshot; agree on the format first, then preserve comments, progress, assignees, and subsequent edits when applying it.

- Use the type words of the commit convention (§6) in titles. The description is Korean, short and specific.
- Write the Figma frame name exactly as it appears in Figma (for example `p2.1-재료 수정`). Frame numbers are being reorganized; when a frame is renamed, update open issues that use it.
- Never paste personal data, tokens, or secrets into an issue, even in error logs.

### Follow-up work

A merged PR is closed and easy to forget. Work that must continue after a PR is kept as an issue.

1. Before the PR is merged, its author opens an issue for each follow-up task. "Follow-up" is not a type: choose the template and type by what the task is (table below), and add the `follow-up` label.
2. Assign the issue to the person who does the next task (usually the owner of that area) and mention them (`@login`) in the issue body.
3. Write the originating PR number in the issue's 참고 field (for example `이어지는 PR: #38`). Do this for every template. Put the follow-up owner in Assignees and mention them in 참고; do not add a separate top-level 이어지는 PR section.
4. In the PR body, write `후속: #<number>` under 관련 이슈. Do not use `Closes`: it would close the follow-up issue when this PR is merged.
5. The PR that does the follow-up task closes the issue with `Closes #<number>`.

| Follow-up task | Type | Template | Example |
| --- | --- | --- | --- |
| Correct something that no longer matches this change | `fix` | 버그 | Code or a document that does not match a changed rule, a wrong path or number |
| Something the team must decide first | `decision` | 결정 요청 | Policy confirmation, two documents that disagree |
| New work that builds on this change | `feat` | 기능 개발 | A screen on top of an API this PR added, the remaining screens |
| Configuration, CI, or documentation | `chore` or `docs` | 설정·문서 작업 | Creating labels, writing a mapping table |

### Labels

| Group | Labels | Rule |
| --- | --- | --- |
| Type | `feat`, `fix`, `chore`, `docs`, `decision` | Exactly one. Same as the title prefix |
| Area | `area:1-auth`, `area:2-fridge`, `area:3-receipt`, `area:4-recipe`, `area:5-meal-plan`, `area:6-notification-admin`, `area:7-share-cart`, `area:common` | One or more. Area numbers match the WBS areas |
| Part | `BE`, `FE`, `infra` | One or more |
| Priority | `P0`, `P1`, `P2` | One. P0 = needed for the first release demo, P1 = should have, P2 = if time allows |
| Status | `needs-decision`, `blocked` | Only while it applies. Remove it when resolved |
| Origin | `follow-up` | On issues opened as PR follow-ups (see "Follow-up work"). Keep it after the issue is closed |

- Give the PR the same type, area, and part labels as its issue.
- A small but urgent correction caused by a document change uses type `fix`, not `docs`: for example code or another document that no longer matches a changed rule, a wrong path, code, or number, or a broken link. Use the 버그 template, the `fix` label, a `fix/` branch, and `fix:` commits. `docs` is for planned documentation work.
