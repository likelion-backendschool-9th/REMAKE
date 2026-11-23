import { Suspense } from "react";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { env } from "@/lib/env";

import { P1_STATES } from "../_lib/states";

// 개발 환경 전용: p1 화면 상태 하나를 그린다(blueprint §3.1 7번). 운영에서는 404.
export const metadata: Metadata = {
  title: "p1 상태 미리보기",
  robots: { index: false },
};

export function generateStaticParams() {
  return P1_STATES.map((state) => ({ state: state.slug }));
}

type StateParams = PageProps<"/dev/p1/[state]">["params"];

// 주소 값(params)은 Suspense 안에서 읽는다(cacheComponents)
async function PreviewState({ params }: { params: StateParams }) {
  const { state: slug } = await params;
  const state = P1_STATES.find((item) => item.slug === slug);
  if (!state) notFound();
  return state.render();
}

export default function P1PreviewStatePage({ params }: PageProps<"/dev/p1/[state]">) {
  if (!env.isDev) notFound();

  return (
    <Suspense>
      <PreviewState params={params} />
    </Suspense>
  );
}
