# 백엔드 배포

> 에이전트용 영문판: [`docs/agent/deployment.md`](../agent/deployment.md)

`dev`의 변경을 GitHub Actions에서 검증한 후, SSH와 rsync로 서버에 전송하고 `deploy/deploy.sh`로 Blue/Green 무중단 배포를 한다. 프론트엔드 배포는 아직 구성하지 않았으므로 백엔드만 배포한다.

## 배포 구조

```
사용자 → 호스트 Nginx(80/443, 도메인·TLS) → 127.0.0.1:APP_PORT → proxy 컨테이너 ─┬→ backend-blue:8080
                                                                                  └→ backend-green:8080
                                                                      postgres (두 슬롯이 공유, 호스트 포트 없음)
```

- `backend-blue`와 `backend-green`은 같은 앱을 실행하는 두 슬롯이다. 한쪽이 서비스하는 동안 다른 쪽에 새 버전을 띄운다.
- 호스트 포트는 배정받은 포트 하나(`APP_PORT`)만 쓰고, 이를 `proxy` 컨테이너가 받는다. 슬롯과 DB는 도커 내부 네트워크로만 연결된다.
- 어느 슬롯이 활성인지는 서버의 `proxy/upstream.conf`가 가리킨다. 배포 스크립트가 이 파일을 바꾸고 프록시를 reload해서 전환한다. 배포 파일 동기화로 덮어쓰이지 않는 서버 전용 파일이다.

## 지금 상태

- `dev`에 백엔드나 배포 설정(`backend/`, `compose.prod.yml`, `deploy/`) 변경이 들어오면 `Backend CD`의 빌드와 테스트가 실행된다.
- 실제 서버 배포는 저장소 변수 `REMAKE_DEPLOY_ENABLED=true`를 설정한 뒤부터 실행된다. 서버가 정해지기 전에는 배포 작업이 건너뛰어진다.
- `workflow_dispatch`로 수동 실행할 수도 있다. 실제 서버 배포 여부에는 같은 변수가 적용된다.

## 서버 준비

1. Ubuntu 서버에 Docker Engine, Docker Compose 플러그인, rsync, curl을 설치한다. 배포 계정을 `docker` 그룹에 넣어 sudo 없이 Docker를 실행할 수 있게 한다. (`sudo usermod -aG docker <계정>` 후 재접속)
2. REMAKE만 사용하는 디렉터리를 정하고 그 안에 `.env`를 만든다. [`deploy/prod.env.example`](../../deploy/prod.env.example)을 참고해 DB 비밀번호를 채우고 파일 권한을 `600`으로 제한한다. 이 디렉터리의 `backend/`와 `deploy/`는 배포 시 소스가 동기화되므로 다른 파일을 두지 않는다.
3. 서버에서 배정받은 호스트 포트를 `.env`의 `APP_PORT`에 입력한다. 값이 없으면 compose가 실행되지 않고, 배포 워크플로도 서버 확인 단계에서 멈춘다.
4. GitHub Actions 전용 SSH 키를 준비하고, 공개 키를 배포 계정의 `authorized_keys`에 등록한다. 서버 호스트 키 지문을 별도로 확인한 뒤 `known_hosts` 항목을 저장한다.
5. 저장소 **Settings → Secrets and variables → Actions**에 다음을 설정한다.

| 종류 | 이름 | 값 |
| --- | --- | --- |
| Secret | `DEPLOY_HOST` | 서버의 도메인 또는 IPv4 주소 |
| Secret | `DEPLOY_USER` | SSH 배포 계정 |
| Secret | `DEPLOY_PATH` | 서버의 배포 디렉터리 절대 경로. 공백 없이 지정 |
| Secret | `SSH_PRIVATE_KEY` | Actions 전용 개인 키 전체 내용 |
| Secret | `SSH_KNOWN_HOSTS` | 확인된 서버 호스트 키의 `known_hosts` 항목 |
| Variable | `REMAKE_DEPLOY_ENABLED` | 준비가 끝난 뒤 `true` |

배포 워크플로는 서버의 `.env`와 `proxy/`를 전송하거나 덮어쓰지 않는다. DB 데이터는 Docker named volume에 보존된다.

## 호스트 Nginx 설정 (도메인 확정 후)

서버에는 다른 사이트가 함께 운영되므로 기존 설정은 건드리지 않고 REMAKE용 사이트 파일만 추가한다. 도메인과 경로 구성이 정해지면 다음을 진행한다.

1. `/etc/nginx/sites-available/remake`를 만들고 `sites-enabled`에 링크한다. `server_name`은 배정받은 도메인으로, `proxy_pass`는 `http://127.0.0.1:<APP_PORT>`로 지정한다. 포트는 `.env`의 `APP_PORT`와 같아야 한다.
2. `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto` 헤더를 넘긴다. 앱이 HTTPS 요청을 올바르게 인식하는 데 필요하다.
3. `sudo nginx -t` 후 reload하고, `sudo certbot --nginx -d <도메인>`으로 인증서를 발급한다. 도메인의 DNS가 이 서버를 가리켜야 한다.

이 설정은 최초 한 번만 하며, 이후 배포에서는 호스트 Nginx를 건드리지 않는다.

## 배포와 롤백

배포는 `Backend CD`가 서버에서 `deploy/deploy.sh`를 실행하면서 이뤄진다. 서버에서 직접 실행할 수도 있다.

```bash
cd <DEPLOY_PATH>
bash deploy/deploy.sh            # 배포 (기본)
bash deploy/deploy.sh rollback   # 이전 슬롯으로 되돌리기
bash deploy/deploy.sh status     # 활성 슬롯과 컨테이너 상태
```

배포 흐름은 다음과 같다.

1. 비활성 슬롯에 새 버전을 빌드해서 띄우고, `/actuator/health/readiness`가 정상이 될 때까지 기다린다.
2. 정상이면 `proxy/upstream.conf`를 새 슬롯으로 바꾸고 `nginx -t`로 검사한 뒤 프록시를 reload한다.
3. 프록시를 통해 응답이 오는지 확인한다.
4. 이전 슬롯은 처리 중인 요청이 끝나길 기다린 뒤(최대 30초) 종료한다. 롤백을 위해 컨테이너와 이미지는 남겨 둔다.

새 슬롯이 준비되지 않거나 전환 후 확인에 실패하면 전환하지 않고 중단하며, 이전 버전이 계속 서비스된다. 처음 배포할 때는 `blue`가 먼저 올라가고 프록시가 시작된다. 롤백은 바로 직전 슬롯으로만 가능하다.

## 확인

서버에서 `bash deploy/deploy.sh status`를 실행해 활성 슬롯과 컨테이너 상태를 확인한다. 새 슬롯의 readiness가 통과해야 배포 작업이 성공한다. `/actuator`는 프록시에서 외부로 노출하지 않으므로, 서버 안에서 확인한다.

운영 프로필은 JPA 스키마를 자동 변경하지 않고 `validate`만 수행하므로, 엔티티 변경 시 DB 마이그레이션을 먼저 적용해야 한다. 배포 중에는 이전 버전과 새 버전이 잠시 같은 DB를 함께 쓰므로, 스키마 변경은 이전 버전과 호환되게 단계적으로 한다.

서버가 아직 정해지지 않았다면 `REMAKE_DEPLOY_ENABLED`를 설정하지 않는다. 이 단계에서는 실제 서비스 주소가 생성되지 않는다.
