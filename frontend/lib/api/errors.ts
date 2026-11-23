// 백엔드 오류 응답과 클라이언트에서 생기는 오류를 한 모양(ApiError)으로 맞춘다.
// 화면은 message가 아니라 code로 분기한다(docs/agent/spec/errors.md).

export type FieldError = { field: string; message: string };

/** 서버 응답이 없는 오류의 code(errors.md 7·8번) */
export const CLIENT_ERROR_CODES = {
  networkOffline: "NETWORK_OFFLINE",
  clientTimeout: "CLIENT_TIMEOUT",
} as const;

type ErrorBody = {
  code?: unknown;
  message?: unknown;
  status?: unknown;
  traceId?: unknown;
  fieldErrors?: unknown;
  details?: unknown;
};

/**
 * code 없는 응답의 code. 프록시의 "준비 중" 503은 점검 문구와 30초 뒤 재시도로,
 * 프록시 시간 제한 504는 타임아웃 문구로 보이게 한다. 나머지는 INTERNAL_ERROR.
 */
function codeFromStatus(status: number): string {
  if (status === 503) return "SERVICE_UNAVAILABLE";
  if (status === 504) return CLIENT_ERROR_CODES.clientTimeout;
  return "INTERNAL_ERROR";
}

export class ApiError extends Error {
  readonly code: string;
  /** HTTP 상태. 서버 응답이 없으면(네트워크·타임아웃) null */
  readonly status: number | null;
  readonly traceId: string | null;
  readonly fieldErrors: FieldError[];
  readonly details: Record<string, unknown>;

  constructor(init: {
    code: string;
    status: number | null;
    message?: string;
    traceId?: string | null;
    fieldErrors?: FieldError[];
    details?: Record<string, unknown>;
  }) {
    super(init.message ?? init.code);
    this.name = "ApiError";
    this.code = init.code;
    this.status = init.status;
    this.traceId = init.traceId ?? null;
    this.fieldErrors = init.fieldErrors ?? [];
    this.details = init.details ?? {};
  }

  /**
   * 서버 응답 본문으로 만든다. 본문에 code가 없으면(Nginx가 직접 돌려준 HTML 오류 등) 상태 코드로 정한다.
   */
  static fromBody(body: unknown, status: number): ApiError {
    const b = (typeof body === "object" && body !== null ? body : {}) as ErrorBody;
    return new ApiError({
      code: typeof b.code === "string" ? b.code : codeFromStatus(status),
      status,
      message: typeof b.message === "string" ? b.message : undefined,
      traceId: typeof b.traceId === "string" ? b.traceId : null,
      fieldErrors: Array.isArray(b.fieldErrors) ? (b.fieldErrors as FieldError[]) : [],
      details:
        typeof b.details === "object" && b.details !== null
          ? (b.details as Record<string, unknown>)
          : {},
    });
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

/**
 * openapi-fetch 결과에서 data를 꺼내고, 실패면 ApiError를 던진다. TanStack Query의 queryFn·mutationFn에서 쓴다.
 * 예: queryFn: async () => unwrap(await api.GET("/api/v1/user/profile"))
 */
export function unwrap<T>(result: { data?: T; error?: unknown; response: Response }): T {
  if (result.error !== undefined || !result.response.ok) {
    throw ApiError.fromBody(result.error, result.response.status);
  }
  return result.data as T;
}
