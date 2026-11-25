// 로그인 화면 약관 드로워 본문. 별도 동의 화면 없이 로그인 화면 링크로 갈음한다(이용 약관 제3조 1항).
// 본문은 lib/legal(원문 docs/developer/terms-of-service.md, privacy-policy.md)에서 가져온다.

import { PRIVACY_POLICY } from "@/lib/legal/privacy-policy";
import { TERMS_OF_SERVICE } from "@/lib/legal/terms-of-service";
import type { LegalDocumentData } from "@/lib/legal/types";

export type TermsKind = "terms" | "privacy";

export const TERMS: Record<TermsKind, LegalDocumentData> = {
  terms: TERMS_OF_SERVICE,
  privacy: PRIVACY_POLICY,
};
