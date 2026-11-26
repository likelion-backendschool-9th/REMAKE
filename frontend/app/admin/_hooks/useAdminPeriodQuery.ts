import { parseAsBoolean, parseAsStringLiteral, useQueryStates } from "nuqs";

import { ADMIN_PERIOD_VALUES, type AdminPeriod, DEFAULT_ADMIN_PERIOD } from "../_lib/period";

/**
 * 운영자 지표 화면(대시보드·퍼널)의 기간과 "실제 데이터만"을 주소에 둔다(blueprint §5, nuqs).
 * 키는 모니터링 API(61~63) 조회 조건과 같다. 예: ?period=LAST_30_DAYS&excludeSeed=false. 기본값이면 주소에서 뺀다.
 * 이 훅을 쓰는 화면은 주소 값을 읽으므로 page에서 Suspense로 감싼다(cacheComponents)
 */
export function useAdminPeriodQuery(defaultPeriod: AdminPeriod = DEFAULT_ADMIN_PERIOD) {
  const [query, setQuery] = useQueryStates({
    period: parseAsStringLiteral(ADMIN_PERIOD_VALUES).withDefault(defaultPeriod),
    excludeSeed: parseAsBoolean.withDefault(true),
  });

  return {
    period: query.period,
    excludeSeed: query.excludeSeed,
    setPeriod: (period: AdminPeriod) => void setQuery({ period }),
    setExcludeSeed: (excludeSeed: boolean) => void setQuery({ excludeSeed }),
  };
}
