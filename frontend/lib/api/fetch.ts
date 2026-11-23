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
 */
export function setAuthFailureHandler(handler: AuthFailureHandler) {
  authFailureHandler = handler;
}

// 여러 요청이 함께 만료되면 refresh는 한 번만 하고 나머지는 그 결과를 기다린다
let refreshing: Promise<boolean> | null = null;

function refreshAccessToken(): Promise<boolean> {
  refreshing ??= fetch(REFRESH_URL, { method: "POST", credentials: "include" })
    .then((res) => res.ok)
    .catch(() => false)
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

async function send(request: Request, timeoutMs: number): Promise<Response> {
  const signal = AbortSignal.any([request.signal, AbortSignal.timeout(timeoutMs)]);
  try {
    return await fetch(new Request(request, { signal }));
  } catch (error) {
    if (error instanceof DOMException && error.name === "TimeoutError") {
      throw new ApiError({ code: CLIENT_ERROR_CODES.clientTimeout, status: null });
    }
    if (error instanceof DOMException && error.name === "AbortError") throw error; // 호출한 쪽이 취소함
    throw new ApiError({ code: CLIENT_ERROR_CODES.networkOffline, status: null });
  }
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
      if (await refreshAccessToken()) return send(retry, timeoutMs);
      authFailureHandler("REFRESH_TOKEN_INVALID");
      return response;
    }
    if (code && AUTH_FAILURE_CODES.has(code)) authFailureHandler(code);
    return response;
  };
}
