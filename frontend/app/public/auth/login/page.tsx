import type { Metadata } from "next";

import { LoginContainer } from "./_components/LoginContainer/LoginContainer";

export const metadata: Metadata = {
  title: "로그인",
  description: "카카오·네이버·구글 계정으로 REMAKE를 시작하세요.",
};

/** p1 로그인 */
export default function LoginPage() {
  return <LoginContainer />;
}
