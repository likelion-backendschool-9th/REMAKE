import type { Metadata } from "next";

import { SettingHouseholdContainer } from "./_components/SettingHouseholdContainer/SettingHouseholdContainer";

export const metadata: Metadata = {
  title: "가구원 설정",
  robots: { index: false },
};

/** p1.3.1 가구원 설정 */
export default function SettingHouseholdPage() {
  return <SettingHouseholdContainer />;
}
