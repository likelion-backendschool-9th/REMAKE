"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { MenuDotsVerticalIcon } from "@solar-icons/react/bold";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { IconButton } from "@/components/IconButton/IconButton";
import { SelectSheet } from "@/components/SelectSheet/SelectSheet";
import { SubHeader } from "@/components/SubHeader/SubHeader";

import { ReadingIndicator } from "../../../../_components/ReadingIndicator/ReadingIndicator";
import { RecipeBody } from "../../../../_components/RecipeBody/RecipeBody";
import { useStepReader } from "../../../../_hooks/useStepReader";
import { RECIPE_HOME_PATH, recipePath, SHARED_RECIPES_PATH } from "../../../../_lib/recipe";
import type { RecipeDetail } from "../../_lib/recipe-detail";

import styles from "./RecipeDetailContainer.module.css";

type MenuAction = "DELETE" | "SHARE";

type RecipeDetailContainerProps = {
  /** 레시피 상세(API 30). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  recipe?: RecipeDetail;
  /** ⋮ 시트를 처음부터 연다. 미리보기용 */
  initialMenuOpen?: boolean;
  /** ⋮ 시트에서 미리 골라 둘 항목. 미리보기용(피그마 케밥 클릭-액티브) */
  initialMenuAction?: MenuAction;
  /** 읽는 중인 단계로 그린다(소리는 내지 않음). 미리보기용 */
  initialPlayingStepNo?: number;
};

/**
 * p4.1.1 레시피 상세. 서브 헤더(← 제목 ⋮), 재료 준비, 만드는 방법(재생하기), Tips!, 아래 "요리 완료"(→ 재료 차감 p4.1.1.2).
 * ⋮: "삭제하기"(내 저장 목록에서만 빠짐, API 35) / "공유하기"(→ 공유 작성 p4.1.1.1, 이번 주에 공유했으면 막음).
 * 상태: 기본 / 케밥 클릭-디세이블·액티브 / 레시피 재생 / 스켈레톤(실제 라우트 처음 상태).
 */
export function RecipeDetailContainer({
  recipe,
  initialMenuOpen = false,
  initialMenuAction,
  initialPlayingStepNo,
}: RecipeDetailContainerProps) {
  const router = useRouter();
  const [isMenuOpen, setIsMenuOpen] = useState(initialMenuOpen);
  const reader = useStepReader(recipe?.steps, initialPlayingStepNo);
  const isPlaying = reader.playingStepNo !== null;

  const handleMenuConfirm = ([action]: MenuAction[]) => {
    setIsMenuOpen(false);
    if (!recipe) return;
    if (action === "SHARE") {
      router.push(`${SHARED_RECIPES_PATH}/new?recipeId=${recipe.id}`);
      return;
    }
    // TODO: API 35(DELETE /user/saved-recipes/{recipeId})를 보내고 레시피 목록으로. 공유 글·남의 저장은 그대로
    router.push(RECIPE_HOME_PATH);
  };

  return (
    <>
      <SubHeader
        title={recipe?.title}
        isTitleLoading={!recipe}
        right={
          <IconButton
            icon={<MenuDotsVerticalIcon />}
            iconSize={16}
            label="더보기"
            aria-haspopup="dialog"
            disabled={!recipe}
            onClick={() => setIsMenuOpen(true)}
          />
        }
      />

      <main className={styles.main}>
        <RecipeBody
          recipe={recipe}
          playingStepNo={reader.playingStepNo}
          onPlay={reader.play}
          onStop={reader.stop}
        />
      </main>

      {isPlaying ? (
        <ReadingIndicator onStop={reader.stop} />
      ) : (
        <BottomBar>
          {/* 요리 완료 → 재료 차감(p4.1.1.2) */}
          <Button
            disabled={!recipe}
            onClick={() => recipe && router.push(`${recipePath(recipe.id)}/cook`)}
          >
            요리 완료
          </Button>
        </BottomBar>
      )}

      {recipe && (
        <SelectSheet<MenuAction>
          isOpen={isMenuOpen}
          onClose={() => setIsMenuOpen(false)}
          title={recipe.title}
          options={[
            { value: "DELETE", label: "삭제하기" },
            { value: "SHARE", label: "공유하기", disabled: !recipe.shareable },
          ]}
          defaultValues={initialMenuAction ? [initialMenuAction] : []}
          onConfirm={handleMenuConfirm}
        />
      )}
    </>
  );
}
