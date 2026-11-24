import type { Metadata } from "next";

import { NotificationsContainer } from "./_components/NotificationsContainer/NotificationsContainer";

export const metadata: Metadata = {
  title: "알림",
  robots: { index: false },
};

/** p6 알림 */
export default function NotificationsPage() {
  return <NotificationsContainer />;
}
