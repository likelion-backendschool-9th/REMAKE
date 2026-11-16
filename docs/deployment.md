# 백엔드 배포 준비

Resuit 프로젝트와 같이 `dev`의 변경을 GitHub Actions에서 검증한 후, SSH와 rsync로 서버에 전송하고 Docker Compose로 백엔드와 PostgreSQL을 실행한다. 현재 프론트엔드는 비어 있으므로 백엔드만 배포한다.

## 지금 상태

- `dev`에 백엔드나 배포 설정 변경이 들어오면 `Backend CD`의 빌드와 테스트가 실행된다.
- 실제 서버 배포는 저장소 변수 `REMAKE_DEPLOY_ENABLED=true`를 설정한 뒤부터 실행된다. 서버가 정해지기 전에는 배포 작업이 건너뛰어진다.
- `workflow_dispatch`로 수동 실행할 수도 있다. 실제 서버 배포 여부에는 같은 변수가 적용된다.

## 서버 준비

1. Ubuntu 서버에 Docker Engine, Docker Compose 플러그인과 rsync를 설치한다. 배포 계정에 Docker 실행 권한을 준다.
2. REMAKE만 사용하는 디렉터리를 정하고 그 안에 `.env`를 만든다. [`deploy/prod.env.example`](../deploy/prod.env.example)을 참고해 DB 비밀번호를 채우고 파일 권한을 `600`으로 제한한다. 이 디렉터리의 `backend/`는 배포 시 소스가 동기화되므로 다른 파일을 두지 않는다.
3. GitHub Actions 전용 SSH 키를 준비하고, 공개 키를 배포 계정의 `authorized_keys`에 등록한다. 서버 호스트 키 지문을 별도로 확인한 뒤 `known_hosts` 항목을 저장한다.
4. 저장소 **Settings → Secrets and variables → Actions**에 다음을 설정한다.

| 종류 | 이름 | 값 |
| --- | --- | --- |
| Secret | `DEPLOY_HOST` | 서버의 도메인 또는 IPv4 주소 |
| Secret | `DEPLOY_USER` | SSH 배포 계정 |
| Secret | `DEPLOY_PATH` | 서버의 배포 디렉터리 절대 경로. 공백 없이 지정 |
| Secret | `SSH_PRIVATE_KEY` | Actions 전용 개인 키 전체 내용 |
| Secret | `SSH_KNOWN_HOSTS` | 확인된 서버 호스트 키의 `known_hosts` 항목 |
| Variable | `REMAKE_DEPLOY_ENABLED` | 준비가 끝난 뒤 `true` |

배포 워크플로는 서버의 `.env`를 전송하거나 덮어쓰지 않는다. DB 데이터는 Docker named volume에 보존된다. 백엔드는 서버의 `127.0.0.1:8080`에만 열리며, 외부 접속이 필요하면 도메인과 TLS가 정해진 뒤 Nginx 등으로 프록시한다.

## 확인

서버에서 `docker compose --env-file .env -f compose.prod.yml ps`를 실행한다. 백엔드의 `/actuator/health`가 정상 응답해야 배포 작업이 성공한다. 운영 프로필은 JPA 스키마를 자동 변경하지 않고 `validate`만 수행하므로, 엔티티 변경 시 DB 마이그레이션을 먼저 적용해야 한다.

서버가 아직 정해지지 않았다면 `REMAKE_DEPLOY_ENABLED`를 설정하지 않는다. 이 단계에서는 실제 서비스 주소가 생성되지 않는다.
