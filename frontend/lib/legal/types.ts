// 이용 약관·개인정보 처리방침 본문 구조. 원문은 docs/developer/terms-of-service.md, privacy-policy.md.
// 원문을 바꾸면 lib/legal의 데이터도 같이 바꾼다.

/** 본문 한 덩어리 */
export type LegalBlock =
  | { type: "paragraph"; text: string }
  | { type: "ordered"; items: string[] }
  | { type: "bullets"; items: string[] }
  | { type: "table"; headers: string[]; rows: string[][] };

/** 조 하나. 예: "제1조 목적" */
export type LegalSection = { title: string; blocks: LegalBlock[] };

export type LegalDocumentData = {
  title: string;
  /** 첫 조 앞의 머리말(개인정보 처리방침) */
  intro?: LegalBlock[];
  sections: LegalSection[];
  /** "2026년 12월 1일" */
  effectiveDate: string;
};
