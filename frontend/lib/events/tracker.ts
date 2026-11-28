// 공용 CLIENT 이벤트 트래커(blueprint §11, docs/agent/spec/events.md, API 56 POST /api/v1/user/events).
// 화면은 POST /api/v1/user/events를 직접 부르지 않고 track()만 쓴다.
// - screen·sessionId(30분 쉬면 새 세션)·occurredAt(KST, +09:00)을 붙여 큐에 쌓는다
// - 50개가 차면 바로, 아니면 FLUSH_DELAY_MS 뒤에 보낸다. 화면이 숨겨지거나(pagehide 포함) 닫힐 때도 보낸다
// - 실패는 조용히 버린다(throw·토스트 없음). 추적 때문에 사용자 흐름이 멈추면 안 된다
// 개인정보·자유 입력 텍스트는 절대 넣지 않는다(이메일, 이름, 직접 입력한 재료명, 레시피 내용 등). 코드·개수·boolean만.

import { api } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";
import { env } from "@/lib/env";

import type { ScreenCode } from "./screens";

type EventItem = components["schemas"]["EventItem"];

/** 속성 값: 평평한 원시값만(중첩 객체·배열 금지, events.md Rules) */
export type EventPropertyValue = string | number | boolean | null;
export type EventProperties = Record<string, EventPropertyValue>;

/** 서버 배치 한도(API 56) */
const MAX_BATCH_SIZE = 50;
/** properties 키 한도 */
const MAX_PROPERTY_KEYS = 25;
/** 큐가 덜 찼을 때 모아서 보내는 대기 시간 */
const FLUSH_DELAY_MS = 5_000;
/** 마지막 이벤트 뒤 이만큼 쉬면 새 세션 */
const SESSION_IDLE_MS = 30 * 60 * 1000;
/** 오프라인 등으로 보내지 못할 때 메모리가 끝없이 늘지 않게 하는 상한(넘치면 오래된 것부터 버림) */
const MAX_QUEUE_SIZE = MAX_BATCH_SIZE * 4;

const EVENTS_PATH = "/api/v1/user/events";
const SESSION_STORAGE_KEY = "remake:event-session";
const KST_OFFSET_MS = 9 * 60 * 60 * 1000;

type SessionState = { id: string; lastAt: number };

let queue: EventItem[] = [];
let flushTimer: ReturnType<typeof setTimeout> | null = null;
let listenersAttached = false;
/** sessionStorage를 못 쓰는 환경(사파리 비공개 모드 등)용 */
let memorySession: SessionState | null = null;

function isBrowser(): boolean {
  return typeof window !== "undefined";
}

// ===== 세션 =====

function createId(): string {
  const cryptoApi = typeof crypto !== "undefined" ? crypto : undefined;
  if (cryptoApi && typeof cryptoApi.randomUUID === "function") {
    try {
      return cryptoApi.randomUUID();
    } catch {
      // 보안 컨텍스트가 아니면(http LAN 주소 등) 실패할 수 있다
    }
  }
  const bytes = new Uint8Array(16);
  if (cryptoApi && typeof cryptoApi.getRandomValues === "function") {
    cryptoApi.getRandomValues(bytes);
  } else {
    for (let i = 0; i < bytes.length; i += 1) bytes[i] = Math.floor(Math.random() * 256);
  }
  // RFC 4122 v4 형식으로 맞춘다
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
  return hex.replace(/^(.{8})(.{4})(.{4})(.{4})(.{12})$/, "$1-$2-$3-$4-$5");
}

function readStoredSession(): SessionState | null {
  if (!isBrowser()) return memorySession;
  try {
    const raw = window.sessionStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) return memorySession;
    const parsed: unknown = JSON.parse(raw);
    if (
      typeof parsed === "object" &&
      parsed !== null &&
      "id" in parsed &&
      "lastAt" in parsed &&
      typeof parsed.id === "string" &&
      typeof parsed.lastAt === "number"
    ) {
      return { id: parsed.id, lastAt: parsed.lastAt };
    }
  } catch {
    // 저장소 접근 불가·깨진 값은 메모리 값으로 대신한다
  }
  return memorySession;
}

function writeStoredSession(session: SessionState): void {
  memorySession = session;
  if (!isBrowser()) return;
  try {
    window.sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
  } catch {
    // 저장소를 못 쓰면 메모리 값만 쓴다
  }
}

/** 지금 이벤트의 sessionId. 마지막 이벤트 뒤 30분이 지났으면 새로 만든다 */
function touchSession(now: number): string {
  const current = readStoredSession();
  const id = current && now - current.lastAt <= SESSION_IDLE_MS ? current.id : createId();
  writeStoredSession({ id, lastAt: now });
  return id;
}

// ===== 값 정리 =====

/**
 * 한국 시간 ISO 8601 문자열. 예: "2026-11-28T14:03:05.123+09:00"
 * 기기 시간대와 관계없이 +09:00으로 보낸다(서버 KST 정책, events.md occurred_at).
 * lib/dayjs에 utc·timezone 플러그인이 없어 직접 계산한다.
 */
function kstIsoString(now: number): string {
  return new Date(now + KST_OFFSET_MS).toISOString().replace("Z", "+09:00");
}

/** 타입을 우회해 들어온 값까지 런타임에서 한 번 더 거른다: 원시값만, 최대 25개 키 */
function sanitizeProperties(properties: EventProperties | undefined): EventProperties | undefined {
  if (!properties || typeof properties !== "object") return undefined;
  const result: EventProperties = {};
  let count = 0;
  for (const [key, value] of Object.entries(properties as Record<string, unknown>)) {
    if (count >= MAX_PROPERTY_KEYS) break;
    if (
      value === null ||
      typeof value === "string" ||
      typeof value === "boolean" ||
      (typeof value === "number" && Number.isFinite(value))
    ) {
      result[key] = value;
      count += 1;
    }
  }
  return count > 0 ? result : undefined;
}

// ===== 전송 =====

function clearFlushTimer(): void {
  if (flushTimer !== null) {
    clearTimeout(flushTimer);
    flushTimer = null;
  }
}

function scheduleFlush(): void {
  if (flushTimer !== null) return;
  flushTimer = setTimeout(() => {
    flushTimer = null;
    void flushEvents();
  }, FLUSH_DELAY_MS);
}

function takeAll(): EventItem[] {
  clearFlushTimer();
  const taken = queue;
  queue = [];
  return taken;
}

function chunk(events: EventItem[]): EventItem[][] {
  const batches: EventItem[][] = [];
  for (let i = 0; i < events.length; i += MAX_BATCH_SIZE) {
    batches.push(events.slice(i, i + MAX_BATCH_SIZE));
  }
  return batches;
}

/** 큐에 쌓인 이벤트를 보낸다. 실패해도 throw하지 않는다(실패한 이벤트는 버림) */
export async function flushEvents(): Promise<void> {
  if (!isBrowser()) return;
  const events = takeAll();
  for (const batch of chunk(events)) {
    try {
      await api.POST(EVENTS_PATH, { body: { events: batch } });
    } catch {
      // 네트워크·타임아웃 등. 추적 실패는 조용히 넘긴다
    }
  }
}

/**
 * 페이지가 숨겨지거나 닫힐 때 보낸다. 페이지가 사라져도 끝까지 가도록 fetch keepalive를 먼저 쓰고
 * (쿠키 인증 + 개발 환경 교차 출처에서도 CORS가 정상 동작), 안 되면 sendBeacon으로 보낸다.
 */
function flushOnExit(): void {
  if (!isBrowser() || queue.length === 0) return;
  const url = `${env.apiBaseUrl}${EVENTS_PATH}`;
  for (const batch of chunk(takeAll())) {
    const body = JSON.stringify({ events: batch });
    try {
      void fetch(url, {
        method: "POST",
        credentials: "include",
        keepalive: true,
        headers: { "Content-Type": "application/json" },
        body,
      }).catch(() => {});
    } catch {
      try {
        if (typeof navigator !== "undefined" && typeof navigator.sendBeacon === "function") {
          navigator.sendBeacon(url, new Blob([body], { type: "application/json" }));
        }
      } catch {
        // 버린다
      }
    }
  }
}

function attachListeners(): void {
  if (listenersAttached || !isBrowser()) return;
  listenersAttached = true;
  try {
    document.addEventListener("visibilitychange", () => {
      if (document.visibilityState === "hidden") flushOnExit();
    });
    window.addEventListener("pagehide", flushOnExit);
  } catch {
    // 리스너를 못 달아도 타이머 전송은 동작한다
  }
}

// ===== 공개 API =====

/**
 * CLIENT 이벤트를 기록한다. 이벤트 이름은 events.md의 Source가 CLIENT인 것만 쓴다(SERVER 이벤트를 중복 기록하지 않음).
 * 절대 throw하지 않는다.
 */
export function track(name: string, screen: ScreenCode, properties?: EventProperties): void {
  if (!isBrowser()) return;
  try {
    attachListeners();
    const now = Date.now();
    const item: EventItem = {
      name,
      screen,
      occurredAt: kstIsoString(now),
      sessionId: touchSession(now),
    };
    const props = sanitizeProperties(properties);
    if (props) item.properties = props;

    queue.push(item);
    if (queue.length > MAX_QUEUE_SIZE) queue = queue.slice(queue.length - MAX_QUEUE_SIZE);

    if (queue.length >= MAX_BATCH_SIZE) void flushEvents();
    else scheduleFlush();
  } catch {
    // 추적 실패는 조용히 넘긴다
  }
}

/**
 * 세션을 끝낸다(로그아웃 시). 아직 안 보낸 이벤트도 버린다: 다음 로그인 사용자 쿠키로 앞 사용자 이벤트가
 * 가지 않게 하기 위해서다. 로그아웃 전에 보내야 할 이벤트(E16 logout_confirmed 등)는
 * `track(...)` → `await flushEvents()` → 로그아웃 API → `resetEventSession()` 순서로 부른다.
 */
export function resetEventSession(): void {
  clearFlushTimer();
  queue = [];
  memorySession = null;
  if (!isBrowser()) return;
  try {
    window.sessionStorage.removeItem(SESSION_STORAGE_KEY);
  } catch {
    // 무시
  }
}

// ===== 타입 보조 함수(자주 쓰는 레시피 이벤트) =====

/** E39·E40·E41·E42 `recipe_mode` 값(소문자) */
export type RecipeModeProperty = "quick" | "custom";

/** E39 recipe_generate_started. 생성 요청을 보내기 직전에 부른다(빠른 생성 모달·커스텀 화면) */
export function trackRecipeGenerateStarted(
  screen: ScreenCode,
  properties: {
    recipe_mode: RecipeModeProperty;
    servings: number;
    /** 고른 옵션 수 0~5 */
    option_count: number;
    supplement_enabled: boolean;
  },
): void {
  track("recipe_generate_started", screen, properties);
}

/** E42 recipe_regenerated. 결과 화면에서 "다시 받기"를 눌렀을 때 부른다 */
export function trackRecipeRegenerated(
  screen: ScreenCode,
  properties: { recipe_mode: RecipeModeProperty },
): void {
  track("recipe_regenerated", screen, properties);
}
