// 로그인 화면 약관 드로워 본문. 별도 동의 화면 없이 로그인 화면 링크로 갈음한다(정책 "약관 동의", 제안).
// TODO: 확정된 이용 약관·개인정보 처리 방침 본문으로 바꾼다. 피그마 본문은 다른 서비스의 예시 글이라 옮기지 않았다.

export type TermsKind = "terms" | "privacy";

export const TERMS: Record<TermsKind, { title: string; body: string }> = {
  terms: {
    title: "이용 약관",
    body: "이용 약관 본문을 준비하고 있어요.",
  },
  privacy: {
    title: "개인정보 처리 방침",
    body: "개인정보 처리 방침 본문을 준비하고 있어요.",
  },
};
