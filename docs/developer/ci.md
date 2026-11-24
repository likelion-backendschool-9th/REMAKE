# CI (PR 검사)

> 에이전트용 영문판: [`docs/agent/ci.md`](../agent/ci.md)

`dev` 또는 `main`으로 PR을 올리면 GitHub Actions가 바뀐 영역을 검사한다. 검사를 통과하고 리뷰 승인을 받아야 머지할 수 있다. `dev`에 머지된 뒤의 배포(CD)는 [배포 문서](deployment.md)를 본다.

## PR에서 도는 워크플로

| 워크플로 | 검사하는 PR | 하는 일 | 필수 검사 |
| --- | --- | --- | --- |
| Backend CI | `backend/`(`README.md` 같은 마크다운 파일만 바뀐 경우는 제외), `compose.prod.yml`, `deploy/prod.env.example`, `.github/workflows/backend-ci.yml`, `scripts/backend_test_summary.py`, `scripts/check_migrations.py`가 바뀐 PR | `./gradlew build`(컴파일, 테스트, jar. 테스트가 PostgreSQL 컨테이너를 직접 띄움), 테스트 요약, Flyway 마이그레이션 파일 검사, 백엔드 Docker 이미지 빌드와 운영 설정으로 띄워 보기 | `Backend CI required` |
| Frontend CI | `frontend/`(`AGENTS.md`, `blueprint.md` 같은 마크다운 파일만 바뀐 경우는 제외), `.github/workflows/frontend-ci.yml`이 바뀐 PR | `pnpm lint`, `pnpm typecheck`, `pnpm build`, 프론트 Docker 이미지 빌드 | `Frontend CI required` |
| Deploy CI | 배포 파일(`compose.prod.yml`, `deploy/`) 또는 워크플로 파일(`.github/workflows/`)이 바뀐 PR | 배포 파일: 셸 스크립트, Compose, 프록시 Nginx 설정 검사. 워크플로 파일: actionlint | `Deploy CI required` |
| PR Attribution Check | 모든 PR | 커밋과 PR 본문의 AI 자동 표기 검사 | — |
| Auto Assign PR Author | 모든 PR | PR 작성자를 담당자로 지정 | — |
| Notify Campfire | 모든 PR (초안 제외) | PR 알림 전송 | — |

## 필수 검사

- 브랜치 룰셋(`protect-dev`, `protect-main`)에는 `Backend CI required`, `Frontend CI required`, `Deploy CI required` 세 개만 필수 검사로 등록한다.
- 세 검사는 모든 PR에 결과를 보고한다. 해당 영역이 바뀌지 않았으면 실제 검사 작업은 건너뛰고 통과로 보고한다. 그래서 개별 작업(예: `Backend build and test`)은 룰셋에 등록하지 않는다. 등록하면 그 영역을 바꾸지 않은 PR이 결과를 기다리며 머지되지 않는다.

## 백엔드 테스트 요약

Backend CI는 테스트가 끝나면 테스트 시간과 Spring 앱 기동 횟수를 표로 남긴다. PR의 **Checks** 탭에서 Backend CI 실행을 열면 맨 위 요약(Summary)에 "백엔드 테스트 요약"이 있다.

| 항목 | 뜻 |
| --- | --- |
| 테스트 클래스 / 테스트 | 실행한 테스트 클래스 수와 테스트 수 |
| 실패한 테스트 | 실패하거나 오류가 난 테스트 수 |
| 테스트 전체 시간 | 모든 테스트 클래스의 시간 합 |
| Spring 앱 기동 횟수 | 테스트 중 Spring 앱이 새로 뜬 횟수와 그 시간의 합 |
| 느린 테스트 클래스 상위 5개 | 클래스별 시간, 테스트 수, 그 클래스에서 Spring 앱이 뜬 횟수 |

Spring은 설정이 같은 테스트 클래스끼리 한 번 띄운 앱을 재사용한다. 클래스마다 설정이 다르면(예: 클래스마다 다른 `@MockitoBean`, `@TestPropertySource`, 테스트 컨테이너) 앱을 매번 새로 띄우고, 테스트 클래스가 늘수록 CI가 그만큼 느려진다. Spring 앱 기동 횟수는 이것을 일찍 알아채기 위한 숫자다.

- **노란 경고**: Spring 앱 기동이 1회를 넘거나(통합 테스트는 앱 하나를 같이 쓴다. [백엔드 테스트 문서](backend-testing.md)) 테스트 전체 시간이 180초를 넘으면 PR 화면에 노란 경고가 뜬다. 머지는 막지 않는다.
- **경고가 뜨면**: 느린 테스트 클래스 표에서 Spring 기동이 1인 클래스들을 보고, 그 클래스의 테스트 설정이 다른 클래스와 왜 다른지 확인한다. 설정을 맞추면 앱을 재사용한다.
- **기준 바꾸기**: `.github/workflows/backend-ci.yml`의 `--max-spring-boots`, `--max-test-seconds` 값을 바꾼다. 새 테스트 부모 클래스를 만드는 PR은 같은 PR에서 `--max-spring-boots`를 올린다.
- 요약 단계는 보조 기능이라, 요약 스크립트에 문제가 생겨도 CI를 실패시키지 않는다. 테스트가 실패하면 CI는 그대로 실패한다.
- **테스트가 실패하면**: 실행 화면 아래 **Artifacts**에서 `backend-test-report`를 내려받아 압축을 풀고 `index.html`을 열면 실패한 테스트와 출력을 볼 수 있다. 7일간 보관한다.

## 마이그레이션 검사

백엔드 테스트는 빈 DB에서 시작해 마이그레이션을 처음부터 순서대로 적용한다. 그래서 개발 서버에서는 실패할 마이그레이션도 테스트는 통과한다. 서버 DB에는 `dev`에 머지된 마이그레이션이 이미 모두 적용되어 있기 때문이다. `Backend migration check` 작업(`scripts/check_migrations.py`)은 PR을 연 시점이 아니라 **검사가 도는 시점**의 base 브랜치와 PR을 비교해서, 아래 경우에 실패한다.

| 경우 | 서버에서 실패하는 이유 | 메시지 |
| --- | --- | --- |
| 새 파일 이름이 `V<번호>__<설명>.sql` 형식이 아님 (번호는 0으로 시작하지 않는 정수, 설명은 영문·숫자·밑줄) | Flyway가 파일을 무시하거나 `V01`과 `V1`을 같은 번호로 읽는다 | `파일 이름은 V<번호>__<설명>.sql 형식이어야 합니다` |
| 새 파일 번호가 base 브랜치의 마지막 번호보다 크지 않음 | 같은 번호면 앱이 뜨지 않는다. 더 작은 번호는 테스트는 통과하지만, 서버 DB가 이미 더 큰 번호를 적용했기 때문에 거부된다 | `dev의 마지막 번호가 V<n>입니다. V<n+1> 이상으로 바꾸세요` |
| PR 안의 새 파일끼리 번호가 같음 | 위와 같다 | `...와 번호(V<n>)가 같습니다` |
| base 브랜치에 이미 있는 파일을 고치거나 지우거나 이름을 바꿈 | 서버 DB는 적용한 파일마다 체크섬을 저장해 두고, 바뀐 파일을 거부한다. 테스트는 통과한다 | `dev에 이미 있는(서버에 적용된) 파일은 고치거나 지우거나 이름을 바꿀 수 없습니다` |

- 실패하면 메시지에 나온 번호로 내 새 파일의 이름을 바꾼다. 이미 적용된 파일에 바꿀 내용이 있으면 새 번호 파일로 추가한다.
- 검사 결과는 그 순간의 기록이다. 내 검사가 통과한 뒤 마이그레이션이 있는 다른 PR이 먼저 머지돼도 내 검사는 초록불로 남는다. **마이그레이션을 추가하는 PR은 머지 직전에 Backend CI를 다시 실행한다**: PR의 **Checks** 탭에서 Backend CI를 열고 **Re-run all jobs**를 누른다. 다시 실행은 푸시가 아니라서 승인이 그대로 유지되고, 최신 `dev`와 다시 비교한다.
- 로컬에서는 저장소 루트에서 실행한다: `git fetch origin dev && python3 scripts/check_migrations.py origin/dev HEAD`.

### 경고: 되돌리기 어려운 문장

같은 작업이 새 마이그레이션 파일을 읽어서, 이전 버전을 깨거나 되돌릴 수 없는 문장이 있으면 그 줄에 노란 경고를 띄운다. 경고는 실행 요약("되돌리기 어려운 마이그레이션")에도 나온다. 머지는 막지 않는다.

| 문장 | 배포 때 위험한 이유 |
| --- | --- |
| `DROP TABLE`, `DROP COLUMN`, `RENAME` | 새 슬롯이 마이그레이션을 적용한 뒤에도 이전 버전이 수십 초 동안 같은 DB로 서비스하는데, 지워지거나 이름이 바뀐 테이블·열을 쓰다가 오류가 난다. 이전 이미지가 바뀐 구조에서 뜨지 않아 백엔드 롤백도 실패한다 |
| `ALTER COLUMN ... TYPE`, `SET NOT NULL` | 이전 버전이 바뀐 열이 더는 받지 않는 값을 쓸 수 있다 |
| `DELETE FROM`, `TRUNCATE` | 지운 행은 백업 없이는 되돌릴 수 없다 |

- `UPDATE`와 `DROP INDEX`는 경고하지 않는다. 자주 쓰이고 이전 버전을 깨지 않기 때문이다. 주석은 무시한다. 글자로 찾는 검사라 흔하지 않은 형태(`COLUMN` 없이 쓴 `DROP` 등)는 놓칠 수 있다.
- 데이터가 있는 테이블에 경고가 뜨면 머지 전에 배포 담당과 순서를 정한다. 예: 먼저 그 열을 쓰지 않는 코드를 배포하고, 열은 다음 PR에서 지운다.
- 서버는 새 마이그레이션을 적용하기 직전에 DB를 백업한다. [배포 문서](deployment.md#새-마이그레이션-전-db-백업)를 본다.

## 운영 설정 기동 확인

테스트는 외부 연동을 가짜로 바꾸고 테스트 설정으로 돌기 때문에, 운영 설정으로는 앱이 뜨지 않아도 통과한다. 예: #132는 CI를 통과했지만, 서버에서는 진짜 `SocialOAuthAdapter`가 필요로 하는 `RestClient.Builder`의 의존성이 빠져 있었다. 새 슬롯이 뜨지 않았고, #141이 머지될 때까지 배포가 막혔다.

그래서 `Backend Docker build and start` 작업은 방금 만든 이미지를 서버의 `deploy/deploy.sh`가 새 슬롯을 띄우는 것과 같은 방법으로 띄워 본다.

- `compose.prod.yml`과 `prod` 프로필: PostgreSQL을 먼저 띄우고, `backend-blue`를 `--no-deps --no-build --wait`로 띄운 뒤 같은 헬스체크(`/actuator/health/readiness`)가 통과할 때까지 최대 180초 기다린다.
- 값은 `deploy/prod.env.example`의 빈 값을 임시 값으로 채워 쓴다. `JWT_SECRET`에는 앱이 요구하는 32바이트 이상의 임시 값을 넣는다.
- CI 러너 안에서만 띄우고, 서버나 외부 서비스에는 접속하지 않는다. PR에서만 돈다. 머지 뒤에는 CD가 서버에서 앱을 띄운다.

| 잡는 것 | 못 잡는 것 (대신 막는 곳) |
| --- | --- |
| 운영 설정에서만 만들어지는 Bean을 만들지 못함 (#132 사례) | 서버 `.env`에 값이 빠짐 (Deploy CI의 새 환경 변수 경고) |
| `application-prod.yml`의 실수 (이름이나 형식 오류) | 서버에 이미 있는 데이터 때문에만 실패하는 마이그레이션. CI는 빈 DB에서 시작한다 (`deploy/deploy.sh`가 이전 버전을 유지) |
| `application-prod.yml`이 기동할 때 필요로 하는 값을 `compose.prod.yml`이 넘기지 않음 | 앱이 뜬 뒤 특정 요청에서 나는 오류 (테스트) |
| 기동할 때 드러나는 이미지 문제 | 외부 서비스 연결. 앱은 기동할 때 외부 서비스에 접속하지 않는다 |

- 실패하면 작업 로그에 `운영 설정으로 백엔드가 뜨지 않음`과 그 아래 앱의 마지막 로그가 나온다. `APPLICATION FAILED TO START` 아래 부분을 읽고, 검사가 아니라 앱을 고친다(예: 빠진 의존성을 `build.gradle`에 추가).
- 형식이 정해진 필수 값(숫자, 주소 등)이 새로 생기면, 같은 PR에서 `.github/workflows/backend-ci.yml`의 `Start backend with production settings` 단계에 그 형식에 맞는 임시 값을 넣는다.

## 배포 파일·워크플로 검사 (Deploy CI)

| 바뀐 파일 | 검사 |
| --- | --- |
| `deploy/`의 셸 스크립트 | `bash -n`(문법), `shellcheck`(흔한 실수) |
| `compose.prod.yml` | `deploy/prod.env.example`의 빈 값을 임시 값으로 채워 `docker compose config`. `compose.prod.yml`에 필수 변수(`${VAR:?...}`)를 추가하고 `deploy/prod.env.example`에 빠뜨리면 실패한다. `compose.prod.yml`에 새로 생긴 변수는 그 줄에 노란 경고를 띄우고 실행 요약에 (필수 여부와 함께) 목록으로 남긴다. 머지 전에 배포 담당자가 서버 `.env`에 넣어야 한다([배포 문서](deployment.md#배포-구성-때문에-앱에-필요한-것)). 경고는 머지를 막지 않는다 |
| `deploy/nginx/` | `deploy/deploy.sh`가 서버에서 하는 것과 같은 방식의 `nginx -t` |
| `.github/workflows/` | actionlint: 문법, 그 자리에서 쓸 수 없는 값, 없는 작업을 가리키는 `needs`, `run` 스크립트(shellcheck), 외부 입력(PR 제목 등)을 스크립트에 그대로 넣는 위험 |

워크플로 파일을 고쳤다면 PR을 올리기 전에 저장소 루트에서 같은 검사를 돌려 볼 수 있다.

```bash
docker run --rm -v "$PWD:/repo:ro" -w /repo rhysd/actionlint:1.7.12 -color .github/workflows/*.yml
```

## 캐시

CI는 아래를 GitHub 캐시에 저장해 다음 실행에서 다시 쓴다. 캐시는 `dev`에 머지될 때(push)만 저장하고, PR은 `dev`에서 저장한 캐시를 읽기만 한다. PR이 저장한 캐시는 다른 PR이 쓰지 못하고 저장 한도(10GB)만 채우기 때문이다.

| 캐시 | 다시 쓰는 것 | 새로 만드는 때 |
| --- | --- | --- |
| Gradle | Gradle 본체와 라이브러리 | `gradle/actions/setup-gradle`이 관리 |
| pnpm | 내려받은 패키지 | `frontend/pnpm-lock.yaml`이 바뀔 때 |
| Next.js 빌드 | `.next/cache`(Turbopack 빌드 결과) | `frontend/pnpm-lock.yaml`이 바뀔 때. 그 외에는 가장 최근 것을 꺼내 쓴다 |
| Docker 레이어 | 이미지 빌드 단계 | Docker 빌드가 관리 |
| 백엔드 Docker 빌드의 Gradle | Dockerfile 캐시 마운트(`/root/.gradle`)의 Gradle 본체와 라이브러리. 없으면 Docker 빌드가 매번 Maven Central에서 다시 받다가 요청 제한(429)에 걸릴 수 있다 | `backend/build.gradle`, `backend/settings.gradle`, Gradle wrapper 버전이 바뀔 때. 이전 것을 꺼내 쓰고 새 라이브러리만 받는다. 그 `dev` 실행에서는 Gradle이 실제로 캐시를 채우도록 Dockerfile의 build 단계를 레이어 캐시 없이 다시 돌린다 |

캐시 때문에 빌드가 이상하게 실패한다고 의심되면, 저장소의 **Actions** 탭 → 왼쪽 **Caches**에서 해당 캐시를 지우고 다시 실행한다. 캐시가 없으면 처음부터 빌드할 뿐 결과는 같다.

## `dev`에 머지된 뒤

- Backend CI와 Frontend CI가 `dev`에서 한 번 더 돌아 캐시를 저장한다. Backend CI는 이때 Docker 이미지 빌드만 하고(빌드·테스트는 CD가 한다) 운영 설정 기동 확인은 건너뛴다(CD가 서버에서 앱을 띄운다).
- 서버 배포는 Backend CD, Frontend CD가 한다. CD도 배포 전에 다시 검사한다. 자세한 내용은 [배포 문서](deployment.md#지금-상태)를 본다.
