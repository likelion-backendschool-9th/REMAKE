import { Suspense } from "react";
import type { Metadata } from "next";

import { UserManagementContainer } from "./_components/UserManagementContainer/UserManagementContainer";

export const metadata: Metadata = {
  title: "사용자 관리",
  description: "회원 번호로 사용자를 찾고 제재 이력과 생성 횟수를 확인합니다.",
  robots: { index: false },
};

export default function AdminUsersPage() {
  return (
    <Suspense>
      <UserManagementContainer />
    </Suspense>
  );
}
