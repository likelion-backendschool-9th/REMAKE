# CI (PR checks)

> Korean version for developers: [`docs/developer/ci.md`](../developer/ci.md)

When a PR targets `dev` or `main`, GitHub Actions checks the changed areas. A PR can be merged only after the checks pass and a review approves it. For deployment (CD) after a merge into `dev`, see the [deployment document](deployment.md).

## Workflows that run on PRs

| Workflow | PRs it checks | What it does | Required check |
| --- | --- | --- | --- |
| Backend CI | PRs that change `backend/` (except changes to Markdown files only, such as `README.md`), `compose.prod.yml`, `deploy/prod.env.example`, `.github/workflows/backend-ci.yml`, `scripts/backend_test_summary.py`, or `scripts/check_migrations.py` | Runs `./gradlew build` (compile, tests, jar; the tests start their own PostgreSQL container), writes the test summary, checks the Flyway migration files, builds the backend Docker image and starts it with the production settings | `Backend CI required` |
| Frontend CI | PRs that change `frontend/` (except changes to Markdown files only, such as `AGENTS.md` or `blueprint.md`) or `.github/workflows/frontend-ci.yml` | `pnpm lint`, `pnpm typecheck`, `pnpm build`, builds the frontend Docker image | `Frontend CI required` |
| Deploy CI | PRs that change the deploy files (`compose.prod.yml`, `deploy/`) or the workflow files (`.github/workflows/`) | Deploy files: checks the shell scripts, Compose, and the proxy Nginx configuration. Workflow files: actionlint | `Deploy CI required` |
| PR Attribution Check | Every PR | Checks commits and the PR description for automatic AI attribution | — |
| Auto Assign PR Author | Every PR | Assigns the PR author | — |
| Notify Campfire | Every PR (except drafts) | Sends a PR notification | — |

## Required checks

- The branch rulesets (`protect-dev`, `protect-main`) require only three checks: `Backend CI required`, `Frontend CI required`, and `Deploy CI required`.
- These checks report a result on every PR. When an area is unchanged, its check jobs are skipped and the check reports success. Do not add individual jobs (for example `Backend build and test`) to the rulesets: PRs that do not change that area would wait for a result forever and could not be merged.

## Backend test summary

After the tests, Backend CI writes a table with the test time and the number of Spring application starts. Open the Backend CI run from the PR's **Checks** tab; "백엔드 테스트 요약" is at the top of the run summary.

| Item | Meaning |
| --- | --- |
| 테스트 클래스 / 테스트 | Number of test classes and tests run |
| 실패한 테스트 | Number of tests that failed or errored |
| 테스트 전체 시간 | Sum of the time of all test classes |
| Spring 앱 기동 횟수 | Number of times a Spring application started during the tests, and the total time of those starts |
| 느린 테스트 클래스 상위 5개 | Time, test count, and Spring application starts per class, for the five slowest classes |

Spring reuses a started application for test classes with the same configuration. When classes have different configurations (for example a different `@MockitoBean`, `@TestPropertySource`, or test container per class), the application is started again for each of them, and CI gets slower with every new test class. The Spring application start count is there to notice this early.

- **Yellow warning**: when Spring application starts exceed 1 (integration tests share one application; see the [backend testing document](backend-testing.md)) or the total test time exceeds 180 seconds, a yellow warning appears on the PR. It does not block the merge.
- **When the warning appears**: in the slowest test classes table, look at the classes whose Spring start count is 1 and check why their test configuration differs from the other classes. Aligning the configuration lets them reuse the application.
- **Changing the thresholds**: change `--max-spring-boots` and `--max-test-seconds` in `.github/workflows/backend-ci.yml`. A PR that adds a new test base class raises `--max-spring-boots` in the same PR.
- The summary step is auxiliary: a problem in the summary script does not fail CI. A failing test still fails CI.
- **When a test fails**: download `backend-test-report` from **Artifacts** at the bottom of the run page, unzip it, and open `index.html` to see the failed tests and their output. It is kept for 7 days.

## Migration check

The backend tests start from an empty DB and apply every migration in order, so they pass even when a migration fails on the development server. The server DB already has every migration merged into `dev` applied. The `Backend migration check` job (`scripts/check_migrations.py`) compares the PR with the base branch **as it is when the check runs**, not when the PR was opened, and fails in these cases:

| Case | Why it fails on the server | Message |
| --- | --- | --- |
| A new file name is not `V<number>__<description>.sql` (number without a leading zero; description of letters, digits, and underscores) | Flyway ignores the file or reads `V01` and `V1` as the same version | `파일 이름은 V<번호>__<설명>.sql 형식이어야 합니다` |
| A new file's number is not greater than the base branch's last number | The same number stops the app from starting. A smaller number is rejected because the server DB has already applied a larger one, even though the tests pass | `dev의 마지막 번호가 V<n>입니다. V<n+1> 이상으로 바꾸세요` |
| Two new files in the PR use the same number | Same as above | `...와 번호(V<n>)가 같습니다` |
| The PR changes, deletes, or renames a file that is already on the base branch (exception: see "If a duplicate or out-of-order number reached `dev` anyway" below) | The server DB stores each applied file's checksum and rejects a changed file, even though the tests pass | `dev에 이미 있는(서버에 적용된) 파일은 고치거나 지우거나 이름을 바꿀 수 없습니다` |

- When it fails, rename your new file to the number in the message. Put changes to an applied file in a new numbered file instead.
- The result is a snapshot: the check runs when the PR is opened or pushed. If another PR with a migration merges after your check passed, your check stays green until it runs again.
- **Automatic re-run**: when a migration lands on `dev`, the `Migration recheck` workflow (`.github/workflows/migration-recheck.yml`) re-runs `Backend migration check` on every open PR into `dev` that adds a migration file. GitHub also re-runs `Backend CI required`, which depends on it. A PR whose number is now taken turns red and cannot be merged. A re-run is not a push, so the approval stays. If the PR's Backend CI is still running, the workflow waits for it to finish first. The list of PRs it re-ran is in the run summary ("열린 PR 마이그레이션 검사 다시 돌리기").
- **Manual re-run**: if you are unsure whether the check is up to date (for example, the `Migration recheck` run failed), re-run it yourself right before merging: on the PR's **Checks** tab, open Backend CI and click **Re-run all jobs**. It compares with the latest `dev` again.
- **If a duplicate or out-of-order number reached `dev` anyway**: the check allows a PR to delete the file that arrived later (revert the PR) or rename it to a new number. The file that arrived first is still protected: it may already be applied on the server, and changing it would stop the app from starting there. If you try, the error message names the later file to change instead.

  | Case on `dev` | What happens | Message on the fix PR |
  | --- | --- | --- |
  | Same number twice (for example two `V17`) | The app cannot start from that merge on, so Backend CD stops at its tests and the server keeps the previous version. The later file was never applied | `::notice::...지우거나 새 번호로 옮기는 것을 허용합니다` |
  | A smaller number after a larger one (for example `V17` after `V18`) | The tests pass on an empty DB, but the server already applied `V18`, so Flyway rejects `V17`, the new slot never starts, and the previous version keeps serving. The later file is normally not applied | `::warning::...더 큰 번호(...)가 먼저 들어와 있어...` with a server query |

  For the out-of-order case, the deployment owner checks the server before the fix PR merges: `SELECT version FROM flyway_schema_history WHERE version = '<n>';` must return no row. If the larger number's deployment had failed before the smaller file merged, the next deployment applied both in order, so the smaller file is applied and must not be renamed. The script cannot see the server, so it does not decide this.
- Run it locally from the repository root: `git fetch origin dev && python3 scripts/check_migrations.py origin/dev HEAD`.

### Warning: statements that are hard to undo

The same job also reads each new migration file and raises a yellow warning on the line when it finds a statement that breaks the previous version or cannot be undone. The warnings are also listed in the run summary ("되돌리기 어려운 마이그레이션"). They do not block the merge.

| Statement | Why it is risky during a deployment |
| --- | --- |
| `DROP TABLE`, `DROP COLUMN`, `RENAME` | The previous version keeps serving on the same DB for a few tens of seconds after the new slot applies the migration, and fails when it uses the dropped or renamed table or column. A backend rollback also fails, because the previous image does not start on the new schema |
| `ALTER COLUMN ... TYPE`, `SET NOT NULL` | The previous version may write values that the changed column no longer accepts |
| `DELETE FROM`, `TRUNCATE` | Deleted rows cannot be brought back without a backup |

- `UPDATE` and `DROP INDEX` are not warned about: they are common and do not break the previous version. Comments are ignored. It is a text search, so unusual forms (such as `DROP` without `COLUMN`) can slip through.
- When the warning appears for a table that has data, agree on the order with the deployment owner before merging. For example, first deploy code that no longer uses the column, then drop it in a later PR.
- The server backs up the DB right before applying a new migration. See the [deployment document](deployment.md#backup-before-a-new-migration).

## Production start check

The tests replace external integrations with fakes and run with the test settings, so they pass even when the app cannot start with the production settings. Example: #132 passed CI, but on the server the real `SocialOAuthAdapter` needed `RestClient.Builder`, whose dependency was missing. The new slot never started, and deployment stayed blocked until #141 was merged.

The `Backend Docker build and start` job therefore starts the image it just built the same way `deploy/deploy.sh` starts a new slot on the server:

- `compose.prod.yml` with the `prod` profile: PostgreSQL first, then `backend-blue` with `--no-deps --no-build --wait`, waiting up to 180 seconds for the same health check (`/actuator/health/readiness`).
- Values come from `deploy/prod.env.example` with its empty values filled with placeholders. `JWT_SECRET` gets a placeholder of at least 32 bytes, which the app requires.
- It runs only inside the CI runner and connects to no server or external service. It runs only on PRs; after the merge, CD starts the app on the server.

| Caught | Not caught (covered by) |
| --- | --- |
| A bean that only the production settings create cannot be created (the #132 case) | A value missing from the server's `.env` (the new-variable warning in Deploy CI) |
| A mistake in `application-prod.yml` (wrong name or format) | A migration that fails only because of data already on the server. CI starts from an empty DB (`deploy/deploy.sh` keeps the previous version) |
| A value that `application-prod.yml` needs at startup but `compose.prod.yml` does not pass | An error in a specific request after the app starts (tests) |
| An image problem that shows up at startup | External service connections. The app does not connect to them at startup |

- When it fails, the job log shows `운영 설정으로 백엔드가 뜨지 않음` followed by the app's last log lines. Read the part under `APPLICATION FAILED TO START` and fix the app (for example, add the missing dependency to `build.gradle`), not the check.
- If a new required value must have a specific format (a number, a URL), set a valid placeholder for it in the `Start backend with production settings` step of `.github/workflows/backend-ci.yml` in the same PR.

## Deploy file and workflow checks (Deploy CI)

| Changed files | Check |
| --- | --- |
| Shell scripts in `deploy/` | `bash -n` (syntax), `shellcheck` (common mistakes) |
| `compose.prod.yml` | `docker compose config` with the empty values of `deploy/prod.env.example` filled with placeholder values. Adding a required variable (`${VAR:?...}`) to `compose.prod.yml` without adding it to `deploy/prod.env.example` fails this check. A variable that is new in `compose.prod.yml` raises a yellow warning on that line and is listed in the run summary (required or not): the deployment owner must put it in the server's `.env` before merging (see the [deployment document](deployment.md#what-the-app-needs-because-of-the-deployment-setup)). The warning does not block the merge |
| `deploy/nginx/` | `nginx -t`, run the same way `deploy/deploy.sh` runs it on the server |
| `.github/workflows/` | actionlint: syntax, values not available in that context, `needs` pointing to a missing job, `run` scripts (shellcheck), and external input (such as the PR title) placed directly in a script |

After editing workflow files, run the same check from the repository root before opening the PR:

```bash
docker run --rm -v "$PWD:/repo:ro" -w /repo rhysd/actionlint:1.7.12 -color .github/workflows/*.yml
```

## Caches

CI stores the following in the GitHub cache and reuses them in later runs. Caches are saved only when changes land on `dev` (push); PRs only read the caches saved from `dev`. A cache saved by a PR cannot be used by other PRs and only fills the storage limit (10GB).

| Cache | What is reused | When it starts fresh |
| --- | --- | --- |
| Gradle | The Gradle distribution and libraries | Managed by `gradle/actions/setup-gradle` |
| pnpm | Downloaded packages | When `frontend/pnpm-lock.yaml` changes |
| Next.js build | `.next/cache` (Turbopack build output) | When `frontend/pnpm-lock.yaml` changes. Otherwise the most recent one is restored |
| Docker layers | Image build steps | Managed by the Docker build |
| Gradle in the backend Docker build | The Gradle distribution and libraries in the Dockerfile's cache mount (`/root/.gradle`). Without it, every Docker build downloads them again from Maven Central and can hit its request limit (429) | When `backend/build.gradle`, `backend/settings.gradle`, or the Gradle wrapper version changes. The previous one is restored and only new libraries are downloaded. On that `dev` run, the Dockerfile's build stage runs without the layer cache so Gradle actually fills the cache |

If a cache seems to cause an odd build failure, delete it under the repository's **Actions** tab → **Caches** on the left and run again. Without the cache the build starts from scratch and produces the same result.

## After the merge into `dev`

- Backend CI and Frontend CI run once more on `dev` to save the caches. At that point Backend CI only builds the Docker image (CD runs the build and tests) and skips the production start check (CD starts the app on the server).
- Backend CD and Frontend CD deploy to the server. CD checks again before deploying. See the [deployment document](deployment.md#current-state).
