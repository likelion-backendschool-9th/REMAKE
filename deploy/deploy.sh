#!/usr/bin/env bash
# 무중단 배포 스크립트 (Blue/Green). 서버의 배포 디렉터리에서 실행한다.
#
# 사용법
#   deploy/deploy.sh [deploy]   새 버전을 비활성 슬롯에 띄우고, 준비되면 프록시를 전환한다. (기본)
#   deploy/deploy.sh rollback   이전 슬롯으로 되돌린다. (이전 슬롯 이미지가 남아 있어야 한다)
#   deploy/deploy.sh status     활성 슬롯과 컨테이너 상태를 보여 준다. (배포 중에도 실행 가능)
#
# 동작 요약
#   1. postgres 가 떠 있지 않을 때만 띄운다. 이미 떠 있으면 건드리지 않는다.
#   2. 비활성 슬롯에 새 버전을 빌드해서 띄우고 readiness 가 통과할 때까지 기다린다.
#      통과하지 못하면 전환하지 않고 중단한다. (이전 버전이 계속 서비스된다)
#   3. proxy/upstream.conf 를 새 슬롯으로 바꾸고, nginx 설정을 검사한 뒤 reload 한다.
#   4. 프록시가 실제로 새 슬롯으로 보내는지, 호스트 포트로 응답이 오는지 확인한다.
#      어느 단계든 실패하면 upstream.conf 와 프록시를 이전 상태로 되돌리고 중단한다.
#   5. 확인이 끝난 뒤에만 이전 슬롯을 graceful 하게 종료한다. (컨테이너와 이미지는 롤백용으로 남긴다)
#
# 주의: 함수 안의 명령은 set -e 에 기대지 않고 실패를 직접 처리한다.
#       bash 는 `if ! 함수` 처럼 조건 안에서 부른 함수 내부에서는 set -e 를 적용하지 않기 때문이다.
#
# 환경 변수(선택)
#   ENV_FILE       compose 에 넘길 env 파일 (기본 .env)
#   COMPOSE_FILE   compose 파일 (기본 compose.prod.yml)
#   READY_TIMEOUT  새 슬롯 readiness 대기 시간(초) (기본 180)
#   DRAIN_SECONDS  전환 후 이전 슬롯을 내리기 전 대기 시간(초) (기본 5)
#   VERIFY_TIMEOUT 전환 후 새 슬롯 응답 확인 대기 시간(초) (기본 15)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ENV_FILE="${ENV_FILE:-.env}"
COMPOSE_FILE="${COMPOSE_FILE:-compose.prod.yml}"
READY_TIMEOUT="${READY_TIMEOUT:-180}"
DRAIN_SECONDS="${DRAIN_SECONDS:-5}"
VERIFY_TIMEOUT="${VERIFY_TIMEOUT:-15}"
UPSTREAM_DIR="proxy"
UPSTREAM_FILE="$UPSTREAM_DIR/upstream.conf"
# proxy.conf 의 내부 전용 확인 경로. 프록시 컨테이너 안에서만 호출한다.
VERIFY_PATH="/__remake/upstream"

COMPOSE=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

log() { printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"; }
warn() { printf '[%s] WARN: %s\n' "$(date '+%H:%M:%S')" "$*" >&2; }
die() { printf '[%s] ERROR: %s\n' "$(date '+%H:%M:%S')" "$*" >&2; exit 1; }

# ---- 준비 확인 ---------------------------------------------------------------

check_requirements() {
  command -v docker >/dev/null || die "docker 가 필요하다"
  command -v curl >/dev/null || die "curl 이 필요하다"
  command -v flock >/dev/null || die "flock 이 필요하다"
  [[ -f "$ENV_FILE" ]] || die "$ENV_FILE 파일이 없다 (DB 값과 APP_PORT 를 채운 .env 가 필요하다)"
  [[ -f "$COMPOSE_FILE" ]] || die "$COMPOSE_FILE 파일이 없다"
  docker info >/dev/null 2>&1 || die "docker 를 사용할 수 없다 (docker 그룹 권한을 확인한다)"
}

# 배포와 롤백이 동시에 실행되지 않도록 잠근다. status 는 잠그지 않는다.
acquire_lock() {
  mkdir -p "$UPSTREAM_DIR"
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

# 슬롯 컨테이너의 IP 목록(공백 구분). 컨테이너가 없으면 실패.
slot_ips() {
  local id
  id="$("${COMPOSE[@]}" ps -q "backend-$1" 2>/dev/null)" || return 1
  [[ -n "$id" ]] || return 1
  docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}} {{end}}' "$id"
}

running() {
  # grep -q 는 먼저 끝나면서 앞 명령을 SIGPIPE 로 끝낼 수 있고, pipefail 때문에 실패로 판단될 수 있다.
  "${COMPOSE[@]}" ps --status running --services 2>/dev/null | grep -x "$1" >/dev/null
}

# postgres 는 떠 있지 않을 때만 띄운다. 슬롯은 --no-deps 로 띄우므로
# compose.prod.yml 의 postgres 설정이 바뀌어도 배포 중에 DB 가 다시 만들어지지 않는다.
ensure_postgres() {
  running postgres && return 0
  log "postgres 가 실행 중이 아니라 먼저 띄운다"
  "${COMPOSE[@]}" up -d --wait postgres || die "postgres 를 띄우지 못했다"
}

# ---- 프록시 --------------------------------------------------------------------

write_upstream() {
  local slot="$1" tmp="$UPSTREAM_FILE.tmp"
  cat >"$tmp" <<EOF || return 1
# deploy.sh 가 활성 슬롯을 바꿀 때마다 덮어쓴다. 직접 수정하지 않는다.
# resolve: 슬롯 컨테이너의 IP 가 바뀌어도 도커 DNS 로 다시 조회한다. (resolver 는 proxy.conf)
upstream remake_backend {
    zone remake_backend 64k;
    server backend-$slot:8080 resolve;
}
EOF
  mv "$tmp" "$UPSTREAM_FILE"   # 같은 폴더 안의 rename 이라 중간 상태가 보이지 않는다
}

# 바뀐 설정을 일회용 컨테이너로 검사한다. 실패하면 nginx 가 출력한 원인을 남긴다.
validate_proxy() {
  local out
  if ! out="$("${COMPOSE[@]}" run --rm --no-deps -T --entrypoint nginx proxy -t 2>&1)"; then
    printf '%s\n' "$out" >&2
    return 1
  fi
}

reload_proxy() {
  "${COMPOSE[@]}" exec -T proxy nginx -s reload
}

# 프록시가 실제로 $1 슬롯으로 요청을 보내는지 확인한다.
# 프록시 컨테이너 안에서 내부 전용 경로를 호출하고, 응답한 업스트림 주소를 새 슬롯 IP 와 비교한다.
verify_routes_to() {
  local slot="$1" ips out addr="" i
  ips="$(slot_ips "$slot")" || { warn "backend-$slot 컨테이너 주소를 알 수 없다"; return 1; }
  for ((i = 1; i <= VERIFY_TIMEOUT; i++)); do
    out="$("${COMPOSE[@]}" exec -T proxy wget -S -q -O /dev/null "http://127.0.0.1$VERIFY_PATH" 2>&1 || true)"
    addr="$(printf '%s\n' "$out" | awk -F': ' 'tolower($1) ~ /x-remake-upstream$/ {print $2}' | tr -d '\r' | tail -n 1)"
    if grep -q 'HTTP/[0-9.]* 200' <<<"$out" && [[ -n "$addr" && " $ips " == *" ${addr%:*} "* ]]; then
      log "프록시가 backend-$slot ($addr) 로 보내는 것을 확인"
      return 0
    fi
    sleep 1
  done
  warn "프록시가 backend-$slot 로 보내지 않는다 (응답한 주소: ${addr:-없음}, backend-$slot 주소: ${ips% })"
  return 1
}

# 호스트 포트(APP_PORT)로 들어온 요청이 응답하는지 확인한다. 502/503/504 나 연결 실패면 실패.
verify_public_port() {
  local hostport port code i
  hostport="$("${COMPOSE[@]}" port proxy 80 2>/dev/null | head -n 1)" || return 1
  port="${hostport##*:}"
  [[ -n "$port" ]] || return 1
  for i in $(seq 1 10); do
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "http://127.0.0.1:$port/" || true)"
    if [[ "$code" =~ ^[1-4][0-9][0-9]$ ]]; then
      log "호스트 포트 응답 확인 (HTTP $code)"
      return 0
    fi
    sleep 1
  done
  warn "호스트 포트 $port 에서 정상 응답이 없다 (마지막 HTTP ${code:-없음})"
  return 1
}

# 활성 슬롯을 $1 로 전환한다. 반환값:
#   0  전환 완료
#   1  전환하지 못했고, upstream.conf 와 프록시를 이전 상태로 되돌렸다. (새 슬롯을 종료해도 안전)
#   2  되돌리기도 실패했다. 프록시가 새 슬롯을 가리키고 있을 수 있으므로 새 슬롯을 종료하면 안 된다.
switch_to() {
  local target="$1" had_prev=0 was_running=0
  if [[ -f "$UPSTREAM_FILE" ]]; then
    cp -p "$UPSTREAM_FILE" "$UPSTREAM_FILE.prev" || return 1
    had_prev=1
  fi
  running proxy && was_running=1

  # 이전 상태로 되돌린다. 프록시가 이전 슬롯을 다시 가리키게 하는 것이 목적이다.
  # 하나라도 실패하면 1 을 반환한다.
  undo_switch() {
    if [[ "$had_prev" == 1 ]]; then
      if ! mv "$UPSTREAM_FILE.prev" "$UPSTREAM_FILE"; then
        warn "upstream.conf 를 이전 내용으로 복구하지 못했다."
        return 1
      fi
    else
      rm -f "$UPSTREAM_FILE"
    fi
    if [[ "$had_prev" == 0 ]]; then
      if ! "${COMPOSE[@]}" stop proxy >/dev/null 2>&1; then
        warn "프록시를 멈추지 못했다."
        return 1
      fi
    elif running proxy; then
      if ! reload_proxy; then
        warn "이전 설정으로 reload 하지 못했다."
        return 1
      fi
    else
      if ! "${COMPOSE[@]}" up -d --no-deps --wait proxy; then
        warn "프록시를 이전 설정으로 다시 띄우지 못했다."
        return 1
      fi
    fi
  }

  # 되돌리고, 되돌리기 결과에 맞는 반환값(1 또는 2)을 낸다.
  undo_or_report() {
    if undo_switch; then return 1; fi
    return 2
  }

  if ! write_upstream "$target"; then
    warn "upstream.conf 를 쓰지 못했다."
    undo_or_report
    return
  fi

  if ! validate_proxy; then
    warn "nginx 설정 검사에 실패했다. 전환하지 않는다."
    undo_or_report
    return
  fi

  # 설정이 바뀌지 않았다면 아무 일도 하지 않고, compose 정의가 바뀌었다면 프록시만 다시 만든다.
  if ! "${COMPOSE[@]}" up -d --no-deps --wait proxy; then
    warn "프록시를 띄우지 못했다. 이전 상태로 되돌린다."
    undo_or_report
    return
  fi
  if [[ "$was_running" == 1 ]] && ! reload_proxy; then
    warn "프록시 reload 에 실패했다. 이전 상태로 되돌린다."
    undo_or_report
    return
  fi

  if ! verify_routes_to "$target" || ! verify_public_port; then
    warn "전환 확인에 실패했다. 이전 상태로 되돌린다."
    undo_or_report
    return
  fi

  rm -f "$UPSTREAM_FILE.prev"
  log "활성 슬롯 전환 완료: $target"
}

stop_slot() {
  local slot="$1"
  log "이전 슬롯(backend-$slot) 종료 대기 ${DRAIN_SECONDS}초 후 graceful 종료"
  sleep "$DRAIN_SECONDS"
  "${COMPOSE[@]}" stop "backend-$slot" || warn "backend-$slot 를 종료하지 못했다. 서비스에는 영향이 없다."
}

# $1 슬롯을 띄우고 readiness 를 기다린다. 추가 인자는 compose up 에 그대로 넘긴다. (--build / --no-build)
start_slot() {
  local slot="$1"; shift
  if ! "${COMPOSE[@]}" up -d --no-deps "$@" --wait --wait-timeout "$READY_TIMEOUT" "backend-$slot"; then
    warn "backend-$slot 가 준비되지 않았다. 최근 로그:"
    "${COMPOSE[@]}" logs --tail 50 "backend-$slot" || true
    "${COMPOSE[@]}" stop "backend-$slot" >/dev/null 2>&1 || true
    return 1
  fi
}

# 전환도 되돌리기도 실패했을 때. 프록시가 어느 슬롯을 가리키는지 알 수 없으므로
# 새 슬롯과 이전 슬롯을 모두 그대로 두고 멈춘다. (어느 쪽을 꺼도 장애가 날 수 있다)
die_undo_failed() {
  die "전환과 되돌리기가 모두 실패했다. backend-$1 와 이전 슬롯을 모두 종료하지 않았다. \
수동 확인 필요: deploy/deploy.sh status 와 proxy/upstream.conf 를 확인하고, 프록시가 가리키는 슬롯이 응답하는지 확인한다."
}

# ---- 명령 ----------------------------------------------------------------------

cmd_deploy() {
  local active target
  active="$(current_slot)"
  if [[ -z "$active" ]]; then target=blue; else target="$(other_slot "$active")"; fi
  log "배포 시작: 활성=${active:-없음} -> 새 슬롯=$target"

  ensure_postgres

  log "backend-$target 빌드 및 기동 (readiness 최대 ${READY_TIMEOUT}초)"
  start_slot "$target" --build \
    || die "새 슬롯이 준비되지 않아 중단한다. 이전 버전(${active:-없음})은 그대로다."

  local rc=0
  switch_to "$target" || rc=$?
  if [[ "$rc" == 2 ]]; then die_undo_failed "$target"; fi
  if [[ "$rc" != 0 ]]; then
    "${COMPOSE[@]}" stop "backend-$target" >/dev/null 2>&1 || true
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

  ensure_postgres

  # 이미 만들어 둔 이미지로 다시 띄운다. 새로 빌드하지 않는다.
  start_slot "$target" --no-build \
    || die "롤백 대상이 준비되지 않았다. 현재 버전($active)은 그대로다."
  local rc=0
  switch_to "$target" || rc=$?
  if [[ "$rc" == 2 ]]; then die_undo_failed "$target"; fi
  if [[ "$rc" != 0 ]]; then
    "${COMPOSE[@]}" stop "backend-$target" >/dev/null 2>&1 || true
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
    deploy)   check_requirements; acquire_lock; cmd_deploy ;;
    rollback) check_requirements; acquire_lock; cmd_rollback ;;
    status)   check_requirements; cmd_status ;;
    *) die "알 수 없는 명령: $cmd (deploy | rollback | status)" ;;
  esac
}

main "$@"
