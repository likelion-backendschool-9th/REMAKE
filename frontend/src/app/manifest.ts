import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "REMAKE",
    short_name: "REMAKE",
    description: "AI 기반 냉장고 식재료 관리 서비스",
    start_url: "/",
    display: "standalone",
    background_color: "#FFFFFF",
    theme_color: "#FFFFFF",
    lang: "ko",
    // TODO: 디자인 확정 후 192/512 PNG 아이콘(public/icons) 추가 — TWA 패키징에 필요
    icons: [{ src: "/favicon.ico", sizes: "any", type: "image/x-icon" }],
  };
}
