import { Suspense } from "react";
import type { Metadata } from "next";

import { RecipeResultContainer } from "./_components/RecipeResultContainer/RecipeResultContainer";

export const metadata: Metadata = {
  title: "레시피 생성",
  robots: { index: false },
};

/** p4.1.3 레시피 생성 결과. cacheComponents에서는 params를 Suspense 안에서 읽는다(불러오는 중 모양이 fallback) */
export default function RecipeResultPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <Suspense fallback={<RecipeResultContainer />}>
      <RecipeResultContent params={params} />
    </Suspense>
  );
}

async function RecipeResultContent({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  // 양의 정수가 아니면 잘못된 주소(null)로 보내 "레시피를 찾을 수 없어요"를 보인다
  const recipeId = /^[1-9]\d*$/.test(id) ? Number(id) : null;
  return <RecipeResultContainer key={recipeId ?? "invalid"} recipeId={recipeId} live />;
}
