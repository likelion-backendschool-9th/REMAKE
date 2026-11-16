import { http, HttpResponse } from "msw";

import { env } from "@/lib/env";

const api = (path: string) => `${env.apiBaseUrl}${path}`;

// 백엔드 API가 완성되기 전까지 사용할 더미 응답
export const handlers = [http.get(api("/api/health"), () => HttpResponse.json({ status: "ok" }))];
