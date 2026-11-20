# CI (PR checks)

> Korean version for developers: [`docs/developer/ci.md`](../developer/ci.md)

When a PR targets `dev` or `main`, GitHub Actions checks the changed areas. A PR can be merged only after the checks pass and a review approves it. For deployment (CD) after a merge into `dev`, see the [deployment document](deployment.md).

## Workflows that run on PRs

| Workflow | PRs it checks | What it does | Required check |
| --- | --- | --- | --- |
| Backend CI | PRs that change `backend/`, `.github/workflows/backend-ci.yml`, or `scripts/backend_test_summary.py` | Runs `./gradlew build` (compile, tests, jar; the tests start their own PostgreSQL container), writes the test summary, builds the backend Docker image | `Backend CI required` |
| Frontend CI | PRs that change `frontend/` or `.github/workflows/frontend-ci.yml` | `pnpm lint`, `pnpm typecheck`, `pnpm build`, builds the frontend Docker image | `Frontend CI required` |
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

## Deploy file and workflow checks (Deploy CI)

| Changed files | Check |
| --- | --- |
| Shell scripts in `deploy/` | `bash -n` (syntax), `shellcheck` (common mistakes) |
| `compose.prod.yml` | `docker compose config` with the empty values of `deploy/prod.env.example` filled with placeholder values. Adding a required variable (`${VAR:?...}`) to `compose.prod.yml` without adding it to `deploy/prod.env.example` fails this check |
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

If a cache seems to cause an odd build failure, delete it under the repository's **Actions** tab → **Caches** on the left and run again. Without the cache the build starts from scratch and produces the same result.

## After the merge into `dev`

- Backend CI and Frontend CI run once more on `dev` to save the caches. At that point Backend CI only builds the Docker image (CD runs the build and tests).
- Backend CD and Frontend CD deploy to the server. CD checks again before deploying. See the [deployment document](deployment.md#current-state).
