import { redirect } from "next/navigation";

import { ADMIN_PATHS } from "./_lib/admin-menu";

/** /admin으로 들어오면 대시보드로 보낸다(blueprint §4 "Admin screens") */
export default function AdminIndexPage() {
  redirect(ADMIN_PATHS.home);
}
