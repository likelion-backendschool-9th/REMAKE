import type { ReactNode } from "react";

/** 개발용 미리보기 상태 하나(blueprint §3.1 7번). /dev/p<N>/_lib/states.tsx가 목록으로 쓴다 */
export type PreviewState = {
  /** 주소 /dev/p<N>/<slug> */
  slug: string;
  /** 목록에 보일 이름. 피그마 프레임 이름과 맞춘다 */
  label: string;
  /** 실제 라우트 */
  route: string;
  render: () => ReactNode;
};
