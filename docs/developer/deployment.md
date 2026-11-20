# 배포

> 에이전트용 영문판: [`docs/agent/deployment.md`](../agent/deployment.md)

`dev`의 변경을 GitHub Actions에서 검증한 후, SSH와 rsync로 서버에 전송하고 `deploy/deploy.sh`로 Blue/Green 무중단 배포를 한다. 백엔드와 프론트엔드는 각각 따로 배포한다.

## 배포 구조

```
사용자 → 호스트 Nginx(80/443, 도메인·TLS) → 127.0.0.1:APP_PORT → proxy 컨테이너 ─┬─ /api/ ─┬→ backend-blue:8080
                                                                                  │         └→ backend-green:8080
                                                                                  └─ 나머지 ┬→ frontend-blue:3000
                                                                                            └→ frontend-green:3000
                                                                      postgres (백엔드 두 슬롯이 공유, 호스트 포트 없음)
```

- `backend-blue`와 `backend-green`, `frontend-blue`와 `frontend-green`은 각각 같은 앱을 실행하는 두 슬롯이다. 한쪽이 서비스하는 동안 다른 쪽에 새 버전을 띄운다.
- 호스트 포트는 배정받은 포트 하나(`APP_PORT`)만 쓰고, 이를 `proxy` 컨테이너가 받는다. 프록시는 `/api/`로 시작하는 요청은 백엔드로, 나머지는 모두 프론트로 보낸다. 경로는 바꾸지 않고 그대로 넘긴다(`/api/items` → 백엔드의 `/api/items`). 슬롯과 DB는 도커 내부 네트워크로만 연결된다.
- 서버를 여러 팀이 함께 쓰므로 슬롯마다 메모리 상한을 둔다. 백엔드는 `mem_limit: 2g`이고 자바 힙은 그 75%까지 쓴다. 프론트는 `mem_limit: 1g`이고 Node가 그 절반을 힙 상한으로 자동으로 잡는다. 배포 중에는 그 앱의 두 슬롯이 함께 떠 있다. 상한을 바꾸면 그 앱의 다음 배포부터 적용되고, 무중단으로 바뀐다.
- 서버의 도커 기본 설정에는 로그 크기 제한이 없으므로, 모든 컨테이너에 로그 크기 제한을 둔다. 로그 파일이 10MB가 되면 새 파일로 넘기고 최근 3개만 남긴다(컨테이너당 최대 30MB). `docker logs`는 그대로 쓸 수 있다. 이 설정은 컨테이너를 새로 만들 때 적용된다. 슬롯은 배포마다 새로 만들어지지만, `postgres`와 `proxy`는 배포 중에 다시 만들지 않으므로 바꾼 뒤에는 서버에서 직접 다시 만들어야 한다.
- 앱별 활성 슬롯은 서버의 `proxy/backend.conf`, `proxy/frontend.conf`가 가리킨다. 배포 스크립트가 배포하는 앱의 파일을 바꾸고 프록시를 reload해서 전환한다. 배포 파일 동기화로 덮어쓰이지 않는 서버 전용 파일이다. 아직 한 번도 배포하지 않은 앱은 "준비 중" 자리를 가리키며, 그 앱의 경로는 503을 돌려준다.

## 지금 상태

- `dev`에 `backend/` 변경이 들어오면 `Backend CD`, `frontend/` 변경이 들어오면 `Frontend CD`가 실행된다. 공용 배포 파일(`compose.prod.yml`, `deploy/`)이 바뀌면 둘 다 실행된다. `backend/`, `frontend/` 아래의 마크다운 파일(`AGENTS.md`, `blueprint.md`, `README.md` 등)만 바뀐 경우는 배포하지 않는다. 앱이 바뀌지 않는데 다시 배포하면 롤백용으로 남겨 둔 이전 버전만 덮어쓰기 때문이다.
- 각 CD는 먼저 검사(백엔드 `./gradlew build`, 프론트 `pnpm lint`, `pnpm typecheck`, `pnpm build`)를 하고, 통과해야 서버에 배포한다.
- 배포 파일(`compose.prod.yml`, `deploy/`)을 바꾼 PR은 머지 전에 `Deploy CI`가 검사한다. `deploy/`의 셸 스크립트(`bash -n`, `shellcheck`), `deploy/prod.env.example`의 빈 값을 채워 넣은 `compose.prod.yml`(`docker compose config`), 그리고 `deploy/deploy.sh`가 서버에서 하는 것과 같은 방식의 `nginx -t`로 프록시 Nginx 설정을 검사한다. `compose.prod.yml`에 필수 변수(`${VAR:?...}`)를 추가하고 `deploy/prod.env.example`에 빠뜨리면 이 검사가 실패한다. 자세한 내용은 [`ci.md`](ci.md#배포-파일워크플로-검사-deploy-ci)를 본다.
- 개발 서버: <https://remake.newlecture.com> (HTTP로 접속하면 HTTPS로 이동한다).
- 2026-11-17부터 서버 배포가 켜져 있다(저장소 변수 `REMAKE_DEPLOY_ENABLED=true`). `dev`에 머지되면 위 규칙에 따라 바뀐 앱이 개발 서버에 자동으로 반영된다.
- `workflow_dispatch`로 수동 실행할 수도 있다. 실제 서버 배포 여부에는 같은 변수가 적용된다.

## 서버 준비

1. Ubuntu 서버에 Docker Engine, Docker Compose 플러그인, rsync, curl을 설치한다. 배포 계정을 `docker` 그룹에 넣어 sudo 없이 Docker를 실행할 수 있게 한다. (`sudo usermod -aG docker <계정>` 후 재접속)
2. REMAKE만 사용하는 디렉터리를 정하고 그 안에 `.env`를 만든다. [`deploy/prod.env.example`](../../deploy/prod.env.example)을 참고해 DB 비밀번호를 채우고 파일 권한을 `600`으로 제한한다. 이 디렉터리의 `backend/`, `frontend/`, `deploy/`는 배포 시 소스가 동기화되므로 다른 파일을 두지 않는다.
3. 서버에서 배정받은 호스트 포트를 `.env`의 `APP_PORT`에 입력한다. 값이 없으면 compose가 실행되지 않고, 배포 워크플로도 서버 확인 단계에서 멈춘다.
4. GitHub Actions 전용 SSH 키를 준비하고, 공개 키를 배포 계정의 `authorized_keys`에 등록한다. 서버 호스트 키 지문을 별도로 확인한 뒤 `known_hosts` 항목을 저장한다.
5. 저장소 **Settings → Environments**에 `production` 환경을 만들고, **Deployment branches**를 `dev` 브랜치 하나로 제한한다.
6. 다음 값을 설정한다. 두 CD가 같은 값을 쓴다. 배포 Secret은 저장소 전체가 아니라 `production` 환경의 Secret으로 둔다. 두 CD의 배포 작업만 이 환경을 쓰므로, 다른 브랜치나 다른 워크플로에서는 서버 접속 키를 꺼낼 수 없다. `REMAKE_DEPLOY_ENABLED`는 배포 작업의 실행 조건(`if`)에서 읽는데, 이 조건은 환경 값을 읽기 전에 계산되므로 저장소 변수(**Settings → Secrets and variables → Actions**)로 둔다.

| 종류 | 위치 | 이름 | 값 |
| --- | --- | --- | --- |
| Secret | `production` 환경 | `DEPLOY_HOST` | 서버의 도메인 또는 IPv4 주소 |
| Secret | `production` 환경 | `DEPLOY_USER` | SSH 배포 계정 |
| Secret | `production` 환경 | `DEPLOY_PATH` | 서버의 배포 디렉터리 절대 경로. 공백 없이 지정 |
| Secret | `production` 환경 | `SSH_PRIVATE_KEY` | Actions 전용 개인 키 전체 내용 |
| Secret | `production` 환경 | `SSH_KNOWN_HOSTS` | 확인된 서버 호스트 키의 `known_hosts` 항목 |
| Variable | 저장소 | `REMAKE_DEPLOY_ENABLED` | 준비가 끝난 뒤 `true` |

배포 워크플로는 서버의 `.env`와 `proxy/`를 전송하거나 덮어쓰지 않는다. DB 데이터는 Docker named volume(`remake_postgres-data`)에 보존된다. `compose.prod.yml`에 프로젝트 이름을 `remake`로 고정해 두었으므로, 배포 디렉터리의 이름이나 위치와 관계없이 같은 볼륨을 쓴다. 이 이름을 바꾸면 빈 볼륨이 새로 만들어지므로 바꾸지 않는다.

프론트의 `NEXT_PUBLIC_*` 값은 실행할 때가 아니라 이미지를 빌드할 때 번들에 들어간다. API 주소는 같은 도메인의 상대 경로(빈 값)로, MSW 목업은 꺼진 상태로 `frontend/Dockerfile`에 기본값이 정해져 있다. 서버마다 다른 값인 `NEXT_PUBLIC_VAPID_PUBLIC_KEY`(Web Push 공개 키)만 서버의 `.env`에 적는다. 값을 바꾸면 다음 프론트 배포 때 다시 빌드되어 적용된다.

## 호스트 Nginx 설정 (최초 1회)

서버에는 다른 사이트가 함께 운영되므로 기존 설정은 건드리지 않고 REMAKE용 사이트 파일만 추가한다. 개발 서버는 2026-11-17에 다음 절차로 설정했다.

1. `/etc/nginx/sites-available/remake`를 만들고 `sites-enabled`에 링크한다. `server_name`은 배정받은 도메인으로, `proxy_pass`는 `http://127.0.0.1:<APP_PORT>`로 지정한다. 포트는 `.env`의 `APP_PORT`와 같아야 한다. 경로 분기는 안쪽 프록시가 하므로 호스트 Nginx는 모든 경로를 그대로 넘긴다. 처음에는 HTTP(80) `server` 블록만 작성한다. 443 설정과 HTTP→HTTPS 리다이렉트는 5번에서 certbot이 추가한다.
2. `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto` 헤더를 넘긴다. 앱이 HTTPS 요청을 올바르게 인식하는 데 필요하다.
3. `proxy_buffering off`를 지정한다. 프론트(Next.js)는 화면을 나눠 스트리밍으로 보내는데, 호스트 Nginx가 응답을 모았다가 보내면 스트리밍이 동작하지 않는다. 안쪽 프록시도 프론트 경로에 같은 설정을 둔다.
4. 캐시(`proxy_cache`)를 켜지 않는다. 서비스 워커(`/serwist/sw.js`)에는 `s-maxage` 30일이 붙어 있어서, 공유 캐시에 남으면 배포한 뒤에도 예전 서비스 워커가 나간다.
5. `sudo nginx -t`가 통과한 뒤에만 reload한다. 다른 사이트와 같은 Nginx를 쓰므로, 검사에 실패한 설정을 남겨 두면 다른 사이트의 reload까지 막힌다. 실패하면 `sites-enabled/remake` 링크를 지운다. 그다음 `sudo certbot --nginx -d <도메인>`으로 인증서를 발급한다. 도메인의 DNS가 이 서버를 가리켜야 한다. PWA와 Web Push는 HTTPS에서만 동작한다. 인증서는 서버의 certbot 타이머가 자동으로 갱신한다.

이 설정은 최초 한 번만 하며, 이후 배포에서는 호스트 Nginx를 건드리지 않는다.

## 배포와 롤백

배포는 `Backend CD`, `Frontend CD`가 서버에서 `deploy/deploy.sh`를 실행하면서 이뤄진다. 서버에서 직접 실행할 수도 있다.

```bash
cd <DEPLOY_PATH>
bash deploy/deploy.sh backend             # 백엔드 배포 (deploy 생략 가능)
bash deploy/deploy.sh frontend            # 프론트 배포
bash deploy/deploy.sh frontend rollback   # 프론트를 이전 슬롯으로 되돌리기 (backend 도 같음)
bash deploy/deploy.sh status              # 앱별 활성 슬롯과 컨테이너 상태 (배포 중에도 실행 가능)
```

배포 흐름은 다음과 같다. 배포하는 앱만 바뀌고, 다른 앱의 슬롯은 건드리지 않는다.

1. (백엔드만) `postgres`가 떠 있지 않을 때만 띄운다. 이미 떠 있으면 건드리지 않는다.
2. 그 앱의 비활성 슬롯에 새 버전을 빌드해서 띄우고, 헬스체크(백엔드 `/actuator/health/readiness`, 프론트 `/`)가 정상이 될 때까지 기다린다.
3. 정상이면 `proxy/<앱>.conf`를 새 슬롯으로 바꾸고 `nginx -t`로 검사한 뒤 프록시를 reload한다.
4. 프록시가 실제로 새 슬롯으로 요청을 보내는지, 호스트 포트(`APP_PORT`)로 그 앱의 경로(백엔드 `/api/`, 프론트 `/`)가 응답하는지 확인한다. 5xx나 연결 실패만 실패로 보고, 404 같은 4xx는 앱까지 도착했다는 뜻이라 통과다.
5. 확인이 끝난 뒤에만 이전 슬롯을 종료한다. 롤백을 위해 컨테이너와 이미지는 남겨 둔다. 백엔드는 처리 중인 요청이 끝나길 최대 30초 기다린다.

프록시가 하나라서 배포와 롤백은 앱과 관계없이 한 번에 하나씩만 한다. 다른 배포가 진행 중이면 끝날 때까지 기다렸다가(최대 15분) 이어서 한다. 그래서 공용 파일이 바뀌어 두 CD가 동시에 시작해도 둘 다 차례로 배포된다.

어느 단계에서든 실패하면(새 슬롯 준비 실패, 설정 검사 실패, 프록시 시작이나 reload 실패, 전환 확인 실패) `proxy/<앱>.conf`와 프록시를 이전 상태로 되돌리고 중단한다. 이전 슬롯은 종료하지 않으므로 이전 버전이 계속 서비스된다. 다른 앱이 서비스 중일 수 있으므로 프록시는 멈추지 않는다. 실패 원인(`nginx -t` 출력, 새 슬롯의 최근 로그)은 배포 로그에 남는다. 그 앱을 처음 배포할 때는 `blue`가 먼저 올라간다. 롤백은 바로 직전 슬롯으로만 가능하다. 롤백한 뒤에도 그 앱의 CD가 다시 실행되면 `dev`의 최신 코드가 배포된다.

드물게 되돌리기까지 실패하면(예: 이전 설정으로 reload 실패) 프록시가 새 슬롯을 가리키고 있을 수 있다. 이때는 어느 슬롯을 꺼도 장애가 날 수 있으므로 새 슬롯과 이전 슬롯을 모두 그대로 두고 "수동 확인 필요" 오류로 중단한다. `bash deploy/deploy.sh status`와 `proxy/<앱>.conf`를 확인하고, `docker compose --env-file .env -f compose.prod.yml exec proxy nginx -s reload`로 프록시를 기록 파일에 맞춘 뒤, 쓰지 않는 슬롯을 직접 종료한다.

`deploy/deploy.sh`는 가장 먼저 compose가 서버 `.env`로 `compose.prod.yml`을 해석할 수 있는지 확인한다(`docker compose config`). `status`와 `rollback`도 같다. 해석하지 못하면 compose가 출력한 메시지를 보여 주고 `compose.prod.yml 을 해석하지 못했다`로 중단하며, 아무것도 바꾸지 않는다. 흔한 원인은 서버 `.env`에 아직 없는 새 필수 변수다. 서버 `.env`에 값을 넣고 실패한 CD 실행을 다시 실행한다(**Actions** → 실패한 실행 → **Re-run failed jobs**). Backend CD와 Frontend CD가 둘 다 실패했을 수 있다. 변수를 없앨 때는 반대 순서로, `compose.prod.yml`에서 먼저 빼고 머지한 뒤 서버 `.env`에서 지운다.

프록시는 슬롯 이름(`backend-blue`, `frontend-green` 등)을 도커 내부 DNS로 10초마다 다시 조회한다. 그래서 활성 슬롯 컨테이너가 재시작되거나 서버가 재부팅되어 IP가 바뀌어도, 다음 배포를 기다리지 않고 자동으로 새 IP를 따라간다.

## 확인

서버에서 `bash deploy/deploy.sh status`를 실행해 앱별 활성 슬롯과 컨테이너 상태를 확인한다. 새 슬롯의 헬스체크가 통과해야 배포 작업이 성공한다. `/actuator`는 프록시에서 외부로 노출하지 않으므로, 서버 안에서 확인한다. 배포 스크립트가 쓰는 확인 경로 `/__remake/upstream/backend`, `/__remake/upstream/frontend`도 프록시 컨테이너 안에서만 호출할 수 있고, 호스트 포트로는 막혀 있다(403).

`compose.prod.yml`의 `postgres` 설정을 바꿔도 배포만으로는 적용되지 않는다. 배포 중에 DB가 다시 만들어지는 일을 막기 위해서다. DB 설정을 바꿨다면 DB가 잠시 재시작되는 시간을 정해 서버에서 `docker compose --env-file .env -f compose.prod.yml up -d postgres`를 직접 실행한다.

DB 스키마는 Flyway로 관리한다. 테이블 변경은 `backend/src/main/resources/db/migration`의 버전별 SQL 파일(`V<번호>__<설명>.sql`)로만 하고, JPA는 운영과 로컬 프로필 모두 `validate`(엔티티와 테이블이 맞는지 검사)만 수행한다. 새 슬롯이 기동할 때 Flyway가 아직 적용하지 않은 파일을 순서대로 실행한다. 마이그레이션이 실패하거나 엔티티에 맞는 테이블이 없으면 새 슬롯이 정상 상태가 되지 않으므로 전환하지 않고, 이전 버전이 계속 서비스된다. PostgreSQL은 DDL도 트랜잭션으로 처리하므로 실패한 파일 안의 변경은 함께 취소된다(`CREATE INDEX CONCURRENTLY`처럼 트랜잭션 밖에서 실행되는 문장은 예외). 배포 중에는 이전 버전과 새 버전이 잠시 같은 DB를 함께 쓰므로, 스키마 변경은 이전 버전과 호환되게 단계적으로 한다. 백엔드와 프론트도 따로 배포되므로, 두 배포 사이에는 잠시 새 프론트와 이전 백엔드(또는 그 반대)가 함께 동작할 수 있다.

배포를 잠시 멈춰야 할 때(서버 작업, 시연 버전 고정, 롤백 후 수정 PR을 기다릴 때)는 `REMAKE_DEPLOY_ENABLED`를 `false`로 바꾼다. CD는 검사만 하고 서버 배포를 건너뛰며, 서버는 마지막으로 배포된 버전 그대로 동작한다.

## 배포 구성 때문에 앱에 필요한 것

팀 규칙이 아니라, 배포를 구현하면서 생긴 조건이다. 지금 배포 구성이 동작하려면 앱이 아래를 지켜야 한다. 숫자로 된 제한(1MB, 60초)은 Nginx 기본값을 그대로 둔 것이므로, 더 필요하면 배포 담당자에게 알려 조정한다.

| 조건 | 이유 | 지키지 않으면 | 하는 법 |
| --- | --- | --- | --- |
| 헬스체크 경로는 인증 없이 응답 | 도커 헬스체크와 `deploy/deploy.sh`의 전환 확인이 `/actuator/health/readiness`를 인증 정보 없이 호출한다. Spring Security를 추가하면 기본값으로 모든 경로에 인증이 필요해진다. | 새 슬롯이 정상 상태가 되지 않아 배포가 매번 실패한다(이전 버전은 계속 서비스된다). | Security 설정에서 `/actuator/health/**`를 인증 없이 허용한다. 외부 사용자는 프록시가 `/actuator`를 404로 막고, 운영 프로필은 `health`만 노출하므로 바깥에 드러나지 않는다. |
| 새 환경 변수는 `compose.prod.yml`에도 추가 | 서버의 `.env`는 compose가 설정 파일의 `${...}`를 채울 때만 쓰인다. 컨테이너에는 `compose.prod.yml`의 `environment`에 적힌 값만 들어간다. | 로컬에서는 동작하고 운영에서만 기동에 실패한다. compose는 파일 전체를 먼저 해석하기 때문에, 필수 변수(`${VAR:?...}`)가 서버 `.env`에 없으면 모든 compose 명령이 실패한다. 값을 넣을 때까지 백엔드·프론트 배포와 롤백이 모두 실패한다. 이미 떠 있는 컨테이너는 계속 서비스한다. | 새 설정 값(토큰 비밀 키, 외부 API 키 등)을 백엔드 `environment`와 `deploy/prod.env.example`에 함께 추가한다. **머지 전에** PR 본문에서 배포 담당자(`@Yunsu0407`)를 부르고 변수 이름을 알린다. 배포 담당자가 서버 `.env`에 실제 값을 넣고 PR에 답을 남기면 그다음에 머지한다. `compose.prod.yml`에 새 변수가 생기면 Deploy CI가 경고를 띄운다. |
| 파일은 컨테이너 안에 저장하지 않음 | 배포할 때마다 새 컨테이너로 바뀐다. 요청 본문 크기는 프록시와 호스트 Nginx 모두 1MB로 제한되어 있다. | 재배포 후 저장한 파일이 사라진다. 1MB가 넘는 요청은 413을 받는다. | 업로드 파일처럼 남겨야 하는 파일은 볼륨이나 외부 저장소에 둔다(도입할 때 배포 담당자와 정한다). |
| 배포 중 두 버전이 함께 실행됨을 감안 | 새 슬롯이 뜬 뒤 전환하고 이전 슬롯을 끄기까지 보통 수십 초 동안 두 버전이 함께 떠 있다. 서버 메모리에만 있는 상태는 전환할 때 사라진다. | `@Scheduled` 같은 예약 작업이 두 번 실행될 수 있다. 로그인 상태를 서버 메모리 세션에 두면 배포할 때마다 모든 사용자가 로그아웃된다. | 예약 작업은 두 번 실행되어도 문제없게 만들거나 중복 실행을 막는다. 로그인 상태는 서버 메모리 세션에 두지 않는다. |
| 요청 하나는 60초 안에 끝남 | 프록시와 호스트 Nginx는 응답을 60초까지 기다린다. 배포 때 이전 버전은 처리 중인 요청을 최대 30초까지 기다린 뒤 종료된다. | 오래 걸리는 요청이 504로 끊기거나 배포 때 중단된다. | OCR처럼 오래 걸리는 처리는 요청 안에서 끝까지 기다리지 말고, 작업을 접수한 뒤 상태를 조회하는 방식으로 만든다. |
| 외부에 등록하는 주소는 개발 서버 도메인 | 앱은 `https://remake.newlecture.com` 뒤에서 동작한다. 원래 요청이 HTTPS임은 호스트 Nginx가 넘긴 헤더로 인식한다(`server.forward-headers-strategy: native`). | 소셜 로그인 등 외부 연동이 운영에서만 실패한다. | 소셜 로그인 redirect URI 등은 `https://remake.newlecture.com/...`으로 등록한다. |
| 로그는 콘솔(표준 출력)로 | `docker logs`와 로그 크기 제한(10MB × 3개)은 콘솔 출력에만 적용된다. | 컨테이너 안에 로그 파일이 크기 제한 없이 쌓이고, 재배포 때 사라진다. | Spring 기본 설정(콘솔 출력)을 그대로 쓰고, 로그 파일 설정을 따로 추가하지 않는다. |
