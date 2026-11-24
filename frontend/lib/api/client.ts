import createClient from "openapi-fetch";

import { env } from "@/lib/env";

import { createApiFetch } from "./fetch";
import type { paths } from "./schema";

// 모든 API 호출은 이 클라이언트로 한다(blueprint §5). 쿠키 인증이라 credentials: "include".
// 타임아웃·401 refresh 처리는 ./fetch.ts, 오류 모양은 ./errors.ts(unwrap, ApiError).
export const api = createClient<paths>({
  baseUrl: env.apiBaseUrl,
  credentials: "include",
  fetch: createApiFetch(),
});

export { ApiError, isApiError, unwrap } from "./errors";
export { createApiFetch, setAuthFailureHandler, TIMEOUT_MS } from "./fetch";
