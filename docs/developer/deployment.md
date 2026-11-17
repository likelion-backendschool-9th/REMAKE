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
- 앱별 활성 슬롯은 서버의 `proxy/backend.conf`, `proxy/frontend.conf`가 가리킨다. 배포 스크립트가 배포하는 앱의 파일을 바꾸고 프록시를 reload해서 전환한다. 배포 파일 동기화로 덮어쓰이지 않는 서버 전용 파일이다. 아직 한 번도 배포하지 않은 앱은 "준비 중" 자리를 가리키며, 그 앱의 경로는 503을 돌려준다.

## 지금 상태

- `dev`에 `backend/` 변경이 들어오면 `Backend CD`, `frontend/` 변경이 들어오면 `Frontend CD`가 실행된다. 공용 배포 파일(`compose.prod.yml`, `deploy/`)이 바뀌면 둘 다 실행된다.
- 각 CD는 먼저 검사(백엔드 `./gradlew build`, 프론트 `pnpm lint`, `pnpm typecheck`, `pnpm build`)를 하고, 통과해야 서버에 배포한다.
- 실제 서버 배포는 저장소 변수 `REMAKE_DEPLOY_ENABLED=true`를 설정한 뒤부터 실행된다. 서버가 정해지기 전에는 배포 작업이 건너뛰어진다.
- `workflow_dispatch`로 수동 실행할 수도 있다. 실제 서버 배포 여부에는 같은 변수가 적용된다.

## 서버 준비

1. Ubuntu 서버에 Docker Engine, Docker Compose 플러그인, rsync, curl을 설치한다. 배포 계정을 `docker` 그룹에 넣어 sudo 없이 Docker를 실행할 수 있게 한다. (`sudo usermod -aG docker <계정>` 후 재접속)
2. REMAKE만 사용하는 디렉터리를 정하고 그 안에 `.env`를 만든다. [`deploy/prod.env.example`](../../deploy/prod.env.example)을 참고해 DB 비밀번호를 채우고 파일 권한을 `600`으로 제한한다. 이 디렉터리의 `backend/`, `frontend/`, `deploy/`는 배포 시 소스가 동기화되므로 다른 파일을 두지 않는다.
3. 서버에서 배정받은 호스트 포트를 `.env`의 `APP_PORT`에 입력한다. 값이 없으면 compose가 실행되지 않고, 배포 워크플로도 서버 확인 단계에서 멈춘다.
4. GitHub Actions 전용 SSH 키를 준비하고, 공개 키를 배포 계정의 `authorized_keys`에 등록한다. 서버 호스트 키 지문을 별도로 확인한 뒤 `known_hosts` 항목을 저장한다.
5. 저장소 **Settings → Secrets and variables → Actions**에 다음을 설정한다. 두 CD가 같은 값을 쓴다.

| 종류 | 이름 | 값 |
| --- | --- | --- |
| Secret | `DEPLOY_HOST` | 서버의 도메인 또는 IPv4 주소 |
| Secret | `DEPLOY_USER` | SSH 배포 계정 |
| Secret | `DEPLOY_PATH` | 서버의 배포 디렉터리 절대 경로. 공백 없이 지정 |
| Secret | `SSH_PRIVATE_KEY` | Actions 전용 개인 키 전체 내용 |
| Secret | `SSH_KNOWN_HOSTS` | 확인된 서버 호스트 키의 `known_hosts` 항목 |
| Variable | `REMAKE_DEPLOY_ENABLED` | 준비가 끝난 뒤 `true` |

배포 워크플로는 서버의 `.env`와 `proxy/`를 전송하거나 덮어쓰지 않는다. DB 데이터는 Docker named volume(`remake_postgres-data`)에 보존된다. `compose.prod.yml`에 프로젝트 이름을 `remake`로 고정해 두었으므로, 배포 디렉터리의 이름이나 위치와 관계없이 같은 볼륨을 쓴다. 이 이름을 바꾸면 빈 볼륨이 새로 만들어지므로 바꾸지 않는다.

프론트의 `NEXT_PUBLIC_*` 값은 실행할 때가 아니라 이미지를 빌드할 때 번들에 들어간다. API 주소는 같은 도메인의 상대 경로(빈 값)로, MSW 목업은 꺼진 상태로 `frontend/Dockerfile`에 기본값이 정해져 있다. 서버마다 다른 값인 `NEXT_PUBLIC_VAPID_PUBLIC_KEY`(Web Push 공개 키)만 서버의 `.env`에 적는다. 값을 바꾸면 다음 프론트 배포 때 다시 빌드되어 적용된다.

## 호스트 Nginx 설정 (도메인 확정 후)

서버에는 다른 사이트가 함께 운영되므로 기존 설정은 건드리지 않고 REMAKE용 사이트 파일만 추가한다. 도메인이 정해지면 다음을 진행한다.

1. `/etc/nginx/sites-available/remake`를 만들고 `sites-enabled`에 링크한다. `server_name`은 배정받은 도메인으로, `proxy_pass`는 `http://127.0.0.1:<APP_PORT>`로 지정한다. 포트는 `.env`의 `APP_PORT`와 같아야 한다. 경로 분기는 안쪽 프록시가 하므로 호스트 Nginx는 모든 경로를 그대로 넘긴다.
2. `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto` 헤더를 넘긴다. 앱이 HTTPS 요청을 올바르게 인식하는 데 필요하다.
3. 캐시(`proxy_cache`)를 켜지 않는다. 서비스 워커(`/serwist/sw.js`)에는 `s-maxage` 30일이 붙어 있어서, 공유 캐시에 남으면 배포한 뒤에도 예전 서비스 워커가 나간다.
4. `sudo nginx -t` 후 reload하고, `sudo certbot --nginx -d <도메인>`으로 인증서를 발급한다. 도메인의 DNS가 이 서버를 가리켜야 한다. PWA와 Web Push는 HTTPS에서만 동작한다.

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

어느 단계에서든 실패하면(새 슬롯 준비 실패, 설정 검사 실패, 프록시 시작이나 reload 실패, 전환 확인 실패) `proxy/<앱>.conf`와 프록시를 이전 상태로 되돌리고 중단한다. 이전 슬롯은 종료하지 않으므로 이전 버전이 계속 서비스된다. 다른 앱이 서비스 중일 수 있으므로 프록시는 멈추지 않는다. 실패 원인(`nginx -t` 출력, 새 슬롯의 최근 로그)은 배포 로그에 남는다. 그 앱을 처음 배포할 때는 `blue`가 먼저 올라간다. 롤백은 바로 직전 슬롯으로만 가능하다.

드물게 되돌리기까지 실패하면(예: 이전 설정으로 reload 실패) 프록시가 새 슬롯을 가리키고 있을 수 있다. 이때는 어느 슬롯을 꺼도 장애가 날 수 있으므로 새 슬롯과 이전 슬롯을 모두 그대로 두고 "수동 확인 필요" 오류로 중단한다. `bash deploy/deploy.sh status`와 `proxy/<앱>.conf`를 확인하고, `docker compose --env-file .env -f compose.prod.yml exec proxy nginx -s reload`로 프록시를 기록 파일에 맞춘 뒤, 쓰지 않는 슬롯을 직접 종료한다.

프록시는 슬롯 이름(`backend-blue`, `frontend-green` 등)을 도커 내부 DNS로 10초마다 다시 조회한다. 그래서 활성 슬롯 컨테이너가 재시작되거나 서버가 재부팅되어 IP가 바뀌어도, 다음 배포를 기다리지 않고 자동으로 새 IP를 따라간다.

## 확인

서버에서 `bash deploy/deploy.sh status`를 실행해 앱별 활성 슬롯과 컨테이너 상태를 확인한다. 새 슬롯의 헬스체크가 통과해야 배포 작업이 성공한다. `/actuator`는 프록시에서 외부로 노출하지 않으므로, 서버 안에서 확인한다. 배포 스크립트가 쓰는 확인 경로 `/__remake/upstream/backend`, `/__remake/upstream/frontend`도 프록시 컨테이너 안에서만 호출할 수 있고, 호스트 포트로는 막혀 있다(403).

`compose.prod.yml`의 `postgres` 설정을 바꿔도 배포만으로는 적용되지 않는다. 배포 중에 DB가 다시 만들어지는 일을 막기 위해서다. DB 설정을 바꿨다면 DB가 잠시 재시작되는 시간을 정해 서버에서 `docker compose --env-file .env -f compose.prod.yml up -d postgres`를 직접 실행한다.

운영 프로필은 JPA 스키마를 자동 변경하지 않고 `validate`만 수행하므로, 엔티티 변경 시 DB 마이그레이션을 먼저 적용해야 한다. 배포 중에는 이전 버전과 새 버전이 잠시 같은 DB를 함께 쓰므로, 스키마 변경은 이전 버전과 호환되게 단계적으로 한다. 백엔드와 프론트도 따로 배포되므로, 두 배포 사이에는 잠시 새 프론트와 이전 백엔드(또는 그 반대)가 함께 동작할 수 있다.

서버가 아직 정해지지 않았다면 `REMAKE_DEPLOY_ENABLED`를 설정하지 않는다. 이 단계에서는 실제 서비스 주소가 생성되지 않는다.
