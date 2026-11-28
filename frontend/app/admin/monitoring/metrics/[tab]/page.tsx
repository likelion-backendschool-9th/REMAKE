import { Suspense } from "react";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { MetricsContainer } from "../_components/MetricsContainer/MetricsContainer";
import { isMetricsTab, METRICS_TABS } from "../_lib/metrics";

export const metadata: Metadata = {
  title: "기능별 지표",
  robots: { index: false },
};

// 탭 8개를 미리 만든다. 그 밖의 주소는 404(cacheComponents라 dynamicParams 대신 notFound로 거른다)
export function generateStaticParams() {
  return METRICS_TABS.map((item) => ({ tab: item.tab }));
}

type TabParams = PageProps<"/admin/monitoring/metrics/[tab]">["params"];

// 주소 값(params)은 Suspense 안에서 읽는다(cacheComponents)
async function MetricsTabPage({ params }: { params: TabParams }) {
  const { tab } = await params;
  if (!isMetricsTab(tab)) notFound();
  // 기간·"실제 데이터만"을 주소(nuqs)에서 읽으므로 이 컨테이너도 Suspense 안에 둔다
  return <MetricsContainer tab={tab} />;
}

/** a3 기능별 지표. API 63이 붙기 전까지 실제 주소는 불러오는 중으로 보이고, 피그마 값은 /dev/admin에서 확인한다 */
export default function AdminMetricsTabPage({
  params,
}: PageProps<"/admin/monitoring/metrics/[tab]">) {
  return (
    <Suspense>
      <MetricsTabPage params={params} />
    </Suspense>
  );
}
