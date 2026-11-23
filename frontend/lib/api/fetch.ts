// API 요청용 fetch: 타임아웃, 네트워크 오류 변환, 401 TOKEN_EXPIRED 처리(refresh 1회 후 원래 요청 1회 재전송).
// blueprint §5, docs/agent/spec/errors.md "Auth flow".

import { env } from "@/lib/env";

import { ApiError, CLIENT_ERROR_CODES } from "./errors";

/**
 * 요청 타임아웃(ms). 프론트 값은 백엔드 제한(일반 10s, OCR 30s, AI 60s)보다 길어서
 * 서버의 OCR_TIMEOUT·AI_TIMEOUT 응답을 먼저 받는다. 최종 값은 #122에서 정한다.
 */
export const TIMEOUT_MS = {
  default: 30_000,
  ocr: 60_000,
  ai: 120_000,
} as const;

const REFRESH_URL = `${env.apiBaseUrl}/api/v1/public/auth/refresh`;

/** 다시 로그인해야 하는 code(refresh 실패 포함) */
const AUTH_FAILURE_CODES = new Set(["UNAUTHORIZED", "REFRESH_TOKEN_INVALID"]);

type AuthFailureHandler = (code: string) => void;
let authFailureHandler: AuthFailureHandler = () => {};

/**
 * 로그인이 풀렸을 때(UNAUTHORIZED, REFRESH_TOKEN_INVALID) 부를 함수를 등록한다.
 * 로그인 기능에서 "쿼리 캐시 비우기 → /public/auth/login 이동(돌아올 주소 보관)"을 등록한다.
 * refresh 실패는 함께 기다린 요청 수와 관계없이 한 번만 부른다. 다만 UNAUTHORIZED는 요청마다 부를 수 있으니
 * 여러 번 불려도 괜찮게(이미 이동 중이면 무시) 만든다.
 */
export function setAuthFailureHandler(handler: AuthFailureHandler) {
  authFailureHandler = handler;
}

/** 여러 신호 중 하나라도 끊기면 끊긴다. AbortSignal.any가 없는 브라우저(iOS 17.4 미만)를 위해 직접 잇는다. */
function anySignal(signals: AbortSignal[]): AbortSignal {
  if ("any" in AbortSignal) return AbortSignal.any(signals);
  const controller = new AbortController();
  for (const signal of signals) {
    if (signal.aborted) {
      controller.abort(signal.reason);
      break;
    }
    signal.addEventListener("abort", () => controller.abort(signal.reason), { once: true });
  }
  return controller.signal;
}

async function send(request: Request, timeoutMs: number): Promise<Response> {
  try {
    const signal = anySignal([request.signal, AbortSignal.timeout(timeoutMs)]);
    return await fetch(new Request(request, { signal }));
  } catch (error) {
    if (error instanceof DOMException && error.name === "TimeoutError") {
      throw new ApiError({ code: CLIENT_ERROR_CODES.clientTimeout, status: null });
    }
    if (error instanceof DOMException && error.name === "AbortError") throw error; // 호출한 쪽이 취소함
    throw new ApiError({ code: CLIENT_ERROR_CODES.networkOffline, status: null });
  }
}

/** refresh 결과. 성공이 아니면 refresh 응답을 그대로 돌려준다(네트워크·타임아웃은 ApiError를 던짐). */
type RefreshResult = { ok: true } | { ok: false; response: Response };

// 여러 요청이 함께 만료되면 refresh는 한 번만 하고 나머지는 그 결과를 기다린다
let refreshing: Promise<RefreshResult> | null = null;

/**
 * refresh(API 2). 로그인 만료로 보는 건 refresh가 401일 때뿐이다.
 * 잠깐 오프라인(NETWORK_OFFLINE)·응답 없음(CLIENT_TIMEOUT)·5xx는 로그인 화면으로 보내지 않고 그 오류를 화면에 돌려준다.
 */
function refreshAccessToken(): Promise<RefreshResult> {
  refreshing ??= send(
    new Request(REFRESH_URL, { method: "POST", credentials: "include" }),
    TIMEOUT_MS.default,
  )
    .then((response): RefreshResult => {
      if (response.ok) return { ok: true };
      if (response.status === 401) authFailureHandler("REFRESH_TOKEN_INVALID");
      return { ok: false, response };
    })
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}

async function readErrorCode(response: Response): Promise<string | null> {
  try {
    const body: unknown = await response.clone().json();
    if (typeof body === "object" && body !== null && "code" in body) {
      return typeof body.code === "string" ? body.code : null;
    }
  } catch {
    // 본문이 JSON이 아님
  }
  return null;
}

/**
 * openapi-fetch에 넘기는 fetch를 만든다. 기본 클라이언트는 TIMEOUT_MS.default를 쓴다.
 * 오래 걸리는 요청은 호출할 때 바꿔 끼운다: api.POST(path, { body, fetch: createApiFetch(TIMEOUT_MS.ocr) })
 */
export function createApiFetch(timeoutMs: number = TIMEOUT_MS.default) {
  return async (request: Request): Promise<Response> => {
    // 재전송용 사본은 보내기 전에 만들어 둔다(본문은 한 번만 읽을 수 있음)
    const retry = request.clone();
    const response = await send(request, timeoutMs);
    if (response.status !== 401) return response;

    const code = await readErrorCode(response);
    if (code === "TOKEN_EXPIRED") {
      const refreshed = await refreshAccessToken();
      if (refreshed.ok) return send(retry, timeoutMs);
      // 같은 응답을 여러 요청이 나눠 받으므로 사본을 돌려준다
      return refreshed.response.clone();
    }
    if (code && AUTH_FAILURE_CODES.has(code)) authFailureHandler(code);
    return response;
  };
}
