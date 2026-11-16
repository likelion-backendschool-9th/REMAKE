#!/usr/bin/env bash
# 무중단 배포 스크립트 (Blue/Green). 서버의 배포 디렉터리에서 실행한다.
#
# 사용법
#   deploy/deploy.sh [deploy]   새 버전을 비활성 슬롯에 띄우고, 준비되면 프록시를 전환한다. (기본)
#   deploy/deploy.sh rollback   이전 슬롯으로 되돌린다. (이전 슬롯 이미지가 남아 있어야 한다)
#   deploy/deploy.sh status     활성 슬롯과 컨테이너 상태를 보여 준다.
#
# 동작 요약
#   1. 비활성 슬롯에 새 버전을 빌드해서 띄우고 readiness 가 통과할 때까지 기다린다.
#      통과하지 못하면 전환하지 않고 중단한다. (이전 버전이 계속 서비스된다)
#   2. proxy/upstream.conf 를 새 슬롯으로 바꾸고, nginx 설정을 검사한 뒤 reload 한다.
#   3. 프록시를 통해 응답이 오는지 확인한다. 실패하면 이전 슬롯으로 되돌린다.
#   4. 이전 슬롯을 graceful 하게 종료한다. (컨테이너와 이미지는 롤백용으로 남긴다)
#
# 환경 변수(선택)
#   ENV_FILE       compose 에 넘길 env 파일 (기본 .env)
#   COMPOSE_FILE   compose 파일 (기본 compose.prod.yml)
#   READY_TIMEOUT  새 슬롯 readiness 대기 시간(초) (기본 180)
#   DRAIN_SECONDS  전환 후 이전 슬롯을 내리기 전 대기 시간(초) (기본 5)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ENV_FILE="${ENV_FILE:-.env}"
COMPOSE_FILE="${COMPOSE_FILE:-compose.prod.yml}"
READY_TIMEOUT="${READY_TIMEOUT:-180}"
DRAIN_SECONDS="${DRAIN_SECONDS:-5}"
UPSTREAM_DIR="proxy"
UPSTREAM_FILE="$UPSTREAM_DIR/upstream.conf"

COMPOSE=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

log() { printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"; }
die() { printf '[%s] ERROR: %s\n' "$(date '+%H:%M:%S')" "$*" >&2; exit 1; }

# ---- 준비 확인 ---------------------------------------------------------------

preflight() {
  command -v docker >/dev/null || die "docker 가 필요하다"
  command -v curl >/dev/null || die "curl 이 필요하다"
  command -v flock >/dev/null || die "flock 이 필요하다"
  [[ -f "$ENV_FILE" ]] || die "$ENV_FILE 파일이 없다 (DB 값과 APP_PORT 를 채운 .env 가 필요하다)"
  [[ -f "$COMPOSE_FILE" ]] || die "$COMPOSE_FILE 파일이 없다"
  docker info >/dev/null 2>&1 || die "docker 를 사용할 수 없다 (docker 그룹 권한을 확인한다)"

  mkdir -p "$UPSTREAM_DIR"
  # 동시에 두 번 배포되지 않도록 잠근다.
  exec 9>"$UPSTREAM_DIR/.deploy.lock"
  flock -n 9 || die "다른 배포가 진행 중이다"
}

# ---- 슬롯 ----------------------------------------------------------------------

# 현재 활성 슬롯(blue/green). 아직 배포한 적이 없으면 빈 값.
current_slot() {
  [[ -f "$UPSTREAM_FILE" ]] || return 0
  grep -oE 'backend-(blue|green)' "$UPSTREAM_FILE" | head -n 1 | sed 's/^backend-//' || true
}

other_slot() {
  if [[ "$1" == "blue" ]]; then echo green; else echo blue; fi
}

# ---- 프록시 --------------------------------------------------------------------

write_upstream() {
  local slot="$1" tmp="$UPSTREAM_FILE.tmp"
  cat >"$tmp" <<EOF
# deploy.sh 가 활성 슬롯을 바꿀 때마다 덮어쓴다. 직접 수정하지 않는다.
upstream remake_backend { server backend-$slot:8080; }
EOF
  mv "$tmp" "$UPSTREAM_FILE"   # 같은 폴더 안의 rename 이라 중간 상태가 보이지 않는다
}

proxy_running() {
  "${COMPOSE[@]}" ps --status running --services 2>/dev/null | grep -qx proxy
}

# 변경된 설정으로 nginx 문법과 업스트림 이름 해석이 되는지 일회용 컨테이너로 검사한다.
validate_proxy() {
  "${COMPOSE[@]}" run --rm --no-deps -T --entrypoint nginx proxy -t >/dev/null 2>&1
}

# 프록시를 통해 응답이 오는지 확인한다. 502/503/504 나 연결 실패면 실패.
smoke_test() {
  local hostport port code i
  hostport="$("${COMPOSE[@]}" port proxy 80 2>/dev/null | head -n 1)" || return 1
  port="${hostport##*:}"
  [[ -n "$port" ]] || return 1
  for i in $(seq 1 10); do
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "http://127.0.0.1:$port/" || true)"
    if [[ "$code" =~ ^[1-4][0-9][0-9]$ ]]; then
      log "프록시 응답 확인 (HTTP $code)"
      return 0
    fi
    sleep 1
  done
  return 1
}

# 활성 슬롯을 $1 로 전환한다. 실패하면 이전 상태로 돌리고 1 을 반환한다.
switch_to() {
  local target="$1" had_prev=0
  if [[ -f "$UPSTREAM_FILE" ]]; then
    cp -p "$UPSTREAM_FILE" "$UPSTREAM_FILE.prev"
    had_prev=1
  fi

  restore() {
    if [[ "$had_prev" == 1 ]]; then
      mv "$UPSTREAM_FILE.prev" "$UPSTREAM_FILE"
    else
      rm -f "$UPSTREAM_FILE"
    fi
  }

  write_upstream "$target"

  if ! validate_proxy; then
    restore
    log "nginx 설정 검사에 실패했다. 전환하지 않는다."
    return 1
  fi

  if proxy_running; then
    # 설정이 바뀌지 않았다면 아무 일도 하지 않고, compose 정의가 바뀌었다면 프록시만 다시 만든다.
    "${COMPOSE[@]}" up -d --no-deps --wait proxy
    "${COMPOSE[@]}" exec -T proxy nginx -s reload
  else
    "${COMPOSE[@]}" up -d --no-deps --wait proxy
  fi

  if ! smoke_test; then
    log "전환 후 응답 확인에 실패했다. 이전 상태로 되돌린다."
    restore
    if [[ "$had_prev" == 1 ]]; then
      "${COMPOSE[@]}" exec -T proxy nginx -s reload || true
    else
      "${COMPOSE[@]}" stop proxy || true
    fi
    return 1
  fi

  rm -f "$UPSTREAM_FILE.prev"
  log "활성 슬롯 전환 완료: $target"
}

stop_slot() {
  local slot="$1"
  log "이전 슬롯(backend-$slot) 종료 대기 ${DRAIN_SECONDS}초 후 graceful 종료"
  sleep "$DRAIN_SECONDS"
  "${COMPOSE[@]}" stop "backend-$slot"
}

# ---- 명령 ----------------------------------------------------------------------

cmd_deploy() {
  local active target
  active="$(current_slot)"
  if [[ -z "$active" ]]; then target=blue; else target="$(other_slot "$active")"; fi
  log "배포 시작: 활성=${active:-없음} -> 새 슬롯=$target"

  log "backend-$target 빌드 및 기동 (readiness 최대 ${READY_TIMEOUT}초)"
  if ! "${COMPOSE[@]}" up -d --build --wait --wait-timeout "$READY_TIMEOUT" "backend-$target"; then
    log "backend-$target 가 준비되지 않았다. 최근 로그:"
    "${COMPOSE[@]}" logs --tail 50 "backend-$target" || true
    "${COMPOSE[@]}" stop "backend-$target" || true
    die "새 슬롯이 준비되지 않아 중단한다. 이전 버전(${active:-없음})은 그대로다."
  fi

  if ! switch_to "$target"; then
    "${COMPOSE[@]}" stop "backend-$target" || true
    die "전환에 실패해 중단한다. 이전 버전(${active:-없음})은 그대로다."
  fi

  if [[ -n "$active" ]]; then stop_slot "$active"; fi
  log "배포 완료: 활성 슬롯 = $target"
}

cmd_rollback() {
  local active target
  active="$(current_slot)"
  [[ -n "$active" ]] || die "활성 슬롯을 알 수 없다. 먼저 배포가 한 번 이상 필요하다."
  target="$(other_slot "$active")"
  docker image inspect "remake-backend-$target" >/dev/null 2>&1 \
    || die "롤백할 이미지(remake-backend-$target)가 없다."
  log "롤백 시작: 활성=$active -> $target"

  # 이미 만들어 둔 이미지로 다시 띄운다. 새로 빌드하지 않는다.
  if ! "${COMPOSE[@]}" up -d --no-build --wait --wait-timeout "$READY_TIMEOUT" "backend-$target"; then
    "${COMPOSE[@]}" logs --tail 50 "backend-$target" || true
    "${COMPOSE[@]}" stop "backend-$target" || true
    die "롤백 대상이 준비되지 않았다. 현재 버전($active)은 그대로다."
  fi
  if ! switch_to "$target"; then
    "${COMPOSE[@]}" stop "backend-$target" || true
    die "롤백 전환에 실패했다. 현재 버전($active)은 그대로다."
  fi
  stop_slot "$active"
  log "롤백 완료: 활성 슬롯 = $target"
}

cmd_status() {
  echo "활성 슬롯: $(current_slot || true)"
  "${COMPOSE[@]}" ps
}

main() {
  local cmd="${1:-deploy}"
  case "$cmd" in
    deploy)   preflight; cmd_deploy ;;
    rollback) preflight; cmd_rollback ;;
    status)   preflight; cmd_status ;;
    *) die "알 수 없는 명령: $cmd (deploy | rollback | status)" ;;
  esac
}

main "$@"
