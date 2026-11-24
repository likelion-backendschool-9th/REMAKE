import type { Metadata } from "next";

import { SettingContainer } from "./_components/SettingContainer/SettingContainer";

export const metadata: Metadata = {
  title: "설정",
  robots: { index: false },
};

/** p1.3 설정 */
export default function SettingProfilePage() {
  return <SettingContainer />;
}
