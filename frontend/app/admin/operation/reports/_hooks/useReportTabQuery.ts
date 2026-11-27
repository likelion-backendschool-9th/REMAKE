import { parseAsStringLiteral, useQueryState } from "nuqs";

import { REPORT_TAB_VALUES, type ReportTab } from "../_lib/reports";

/**
 * 신고 관리의 처리 대기·처리 완료 탭을 주소(?tab=done)에 둔다. 기본값(대기)이면 주소에서 뺀다.
 * 이 훅을 쓰는 화면은 주소 값을 읽으므로 page에서 Suspense로 감싼다(cacheComponents, blueprint §5)
 */
export function useReportTabQuery(defaultTab: ReportTab = "pending") {
  const [tab, setTab] = useQueryState(
    "tab",
    parseAsStringLiteral(REPORT_TAB_VALUES).withDefault(defaultTab),
  );

  return { tab, setTab };
}
