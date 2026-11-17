# Git Convention

> Korean version for developers: [`docs/developer/git-convention.md`](../developer/git-convention.md)

## 1. Branches

- `main`: stable, release-ready version.
- `dev`: integrates and tests the team's work.
- Work branches: features, bug fixes, configuration, and documentation.

Create work branches by task type. Name format: `<type>/<task-description>`, where the description is short lowercase kebab-case English.

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

Agents: the examples use `git add .` for brevity. Stage only the files related to the change (see `AGENTS.md` §2).

## 5. PR and merge rules

- Every work branch opens its PR against `dev`.
- The PR title uses the same type format as commits.
- The PR body states the purpose, the main changes, and the test results. Fill every section of the [PR template](../../.github/pull_request_template.md) (변경 내용, 관련 이슈, 테스트, 리뷰 요청).
- PRs into `dev` or `main` must pass the `Backend CI required` and `Frontend CI required` checks before merging.
- These checks report a result on every PR. The backend or frontend build jobs are skipped when that area is unchanged; do not require those individual jobs in branch rulesets.
- Merge after team review and confirmation.
- After merging, delete the work branch both remotely and locally.

| Work branch | PR base |
| --- | --- |
| `feat/*` | `dev` |
| `fix/*` | `dev` |
| `chore/*` | `dev` |
| `docs/*` | `dev` |

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
- Do not add AI co-author trailers (`Co-Authored-By: Claude ...`) or `Generated with ...` bylines to commits or PR bodies. The `PR Attribution Check` CI fails on them.

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
