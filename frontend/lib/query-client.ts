import { isServer, QueryClient } from "@tanstack/react-query";

import { isApiError } from "@/lib/api/errors";

// 재시도(blueprint §5): 조회는 1번, 변경 요청은 하지 않는다.
// 조회도 사용자가 고칠 수 있는 오류(4xx: 입력·권한·없음·한도)는 다시 보내지 않는다. 네트워크·타임아웃·5xx만 1번.
function shouldRetryQuery(failureCount: number, error: unknown): boolean {
  if (failureCount >= 1) return false;
  if (isApiError(error) && error.status !== null && error.status < 500) return false;
  return true;
}

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000,
        retry: shouldRetryQuery,
      },
      mutations: {
        retry: 0,
      },
    },
  });
}

let browserQueryClient: QueryClient | undefined;

export function getQueryClient() {
  if (isServer) return makeQueryClient();
  browserQueryClient ??= makeQueryClient();
  return browserQueryClient;
}
