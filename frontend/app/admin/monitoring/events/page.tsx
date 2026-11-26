import { Suspense } from "react";
import type { Metadata } from "next";

import { EventLogContainer } from "./_components/EventLogContainer/EventLogContainer";

export const metadata: Metadata = {
  title: "이벤트 로그",
  robots: { index: false },
};

/** a6 이벤트 로그 */
export default function AdminEventsPage() {
  // 검색·기간 조건을 주소(nuqs)에서 읽으므로 Suspense 안에 둔다(cacheComponents, blueprint §5)
  return (
    <Suspense>
      <EventLogContainer />
    </Suspense>
  );
}
