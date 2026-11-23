// p1(로그인·온보딩·설정) 화면 상태 목록. /dev/p1이 이 순서로 목록을 보이고, /dev/p1/[state]가 하나를 그린다.
// 상태를 더하면 여기에 한 줄 추가한다(blueprint §3.1 7번). 실제 라우트는 컨테이너 기본값을 쓴다.

import type { ReactNode } from "react";

import { LoginContainer } from "../../../public/auth/login/_components/LoginContainer/LoginContainer";

export type PreviewState = {
  /** 주소 /dev/p1/<slug> */
  slug: string;
  /** 목록에 보일 이름. 피그마 프레임 이름과 맞춘다 */
  label: string;
  /** 실제 라우트 */
  route: string;
  render: () => ReactNode;
};

export const P1_STATES: PreviewState[] = [
  {
    slug: "login",
    label: "p1-로그인",
    route: "/public/auth/login",
    render: () => <LoginContainer recentProvider={null} />,
  },
  {
    slug: "login-recent",
    label: "p1-로그인-최근 로그인 (카카오)",
    route: "/public/auth/login",
    render: () => <LoginContainer recentProvider="kakao" />,
  },
  {
    slug: "login-terms",
    label: "common-약관드로워 (이용 약관)",
    route: "/public/auth/login",
    render: () => <LoginContainer recentProvider={null} initialOpenTerms="terms" />,
  },
  {
    slug: "login-privacy",
    label: "common-약관드로워 (개인정보 처리 방침)",
    route: "/public/auth/login",
    render: () => <LoginContainer recentProvider={null} initialOpenTerms="privacy" />,
  },
];
