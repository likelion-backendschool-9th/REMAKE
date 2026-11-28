import { redirect } from "next/navigation";

import { ADMIN_PATHS } from "../../_lib/admin-menu";

/** /admin/monitoring/metrics로 들어오면 피그마 기본 화면인 AI 탭으로 보낸다 */
export default function AdminMetricsIndexPage() {
  redirect(ADMIN_PATHS.metrics("ai"));
}
