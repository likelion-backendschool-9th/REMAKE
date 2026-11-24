import type { Metadata } from "next";

import { AccountDeletionContainer } from "./_components/AccountDeletionContainer/AccountDeletionContainer";

export const metadata: Metadata = {
  title: "계정 삭제",
  robots: { index: false },
};

/** p1.3.3 계정 삭제 이메일 인증번호 */
export default function AccountDeletionPage() {
  return <AccountDeletionContainer />;
}
