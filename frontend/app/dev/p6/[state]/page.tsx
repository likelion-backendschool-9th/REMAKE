import { Suspense } from "react";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { env } from "@/lib/env";

import { P6_STATES } from "../_lib/states";

// 개발 환경 전용: p6 화면 상태 하나를 그린다(blueprint §3.1 7번). 운영에서는 404.
export const metadata: Metadata = {
  title: "p6 상태 미리보기",
  robots: { index: false },
};

// 운영 빌드에서는 미리보기 경로를 만들지 않는다. cacheComponents라 빈 배열은 빌드 오류여서 자리표시 하나만 둔다(페이지는 404)
export function generateStaticParams() {
  if (!env.isDev) return [{ state: "__none__" }];
  return P6_STATES.map((state) => ({ state: state.slug }));
}

type StateParams = PageProps<"/dev/p6/[state]">["params"];

// 주소 값(params)은 Suspense 안에서 읽는다(cacheComponents)
async function PreviewState({ params }: { params: StateParams }) {
  const { state: slug } = await params;
  const state = P6_STATES.find((item) => item.slug === slug);
  if (!state) notFound();
  return state.render();
}

export default function P6PreviewStatePage({ params }: PageProps<"/dev/p6/[state]">) {
  if (!env.isDev) notFound();

  return (
    <Suspense>
      <PreviewState params={params} />
    </Suspense>
  );
}
