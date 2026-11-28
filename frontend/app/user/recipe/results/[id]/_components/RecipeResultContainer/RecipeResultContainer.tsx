"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { IconButton } from "@/components/IconButton/IconButton";
import { SubHeader } from "@/components/SubHeader/SubHeader";

import { ReadingIndicator } from "../../../../_components/ReadingIndicator/ReadingIndicator";
import { RecipeBody } from "../../../../_components/RecipeBody/RecipeBody";
import { useStepReader } from "../../../../_hooks/useStepReader";
import { recipePath } from "../../../../_lib/recipe";
import type { RecipeResult } from "../../_lib/recipe-result";
import { BookmarkIcon } from "../BookmarkIcon/BookmarkIcon";

import styles from "./RecipeResultContainer.module.css";

type RecipeResultContainerProps = {
  /** 생성 결과(API 28·29 응답, 상세와 같은 모양). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  recipe?: RecipeResult;
  /** 오늘 남은 레시피 생성 횟수(API 27 recipe.remaining). "레시피 다시 받기 N회" */
  remainingCount?: number;
};

/**
 * p4.1.3 레시피 생성 결과. 서브 헤더(← 제목 + 저장 북마크), 재료 준비, 만드는 방법, Tips!,
 * 아래 "요리 완료"와 그 밑 "레시피 다시 받기 N회"(같은 조건으로 새로, 하루 횟수 차감, 늘 누를 수 있고 흐린 글자로만 보임).
 * 만든 레시피는 저장하기 전까지 목록에 안 보인다. 북마크로 저장하면 보충 재료가 장바구니로 간다(domain-rules 4).
 * 상태: 레시피 생성(저장 전) / 레시피 저장(저장 후). 북마크는 lucide 모양(피그마 5780-3817), 저장 후 선과 속 모두 brand/primary.
 * TODO: 생성 중·생성 실패(재료 부족·AI 오류)·횟수 소진은 이 주소의 상태(피그마 "추가 화면", 디자인 v2 대기)
 */
export function RecipeResultContainer({ recipe, remainingCount }: RecipeResultContainerProps) {
  const router = useRouter();
  const reader = useStepReader(recipe?.steps);
  const isPlaying = reader.playingStepNo !== null;
  // TODO: API 34(저장)·35(저장 취소)를 붙이면 응답으로 바꾼다. 저장하면 cartAddedCount 토스트(공유 상세와 같은 모양, 피그마 6115-7503)
  const [savedOverride, setSavedOverride] = useState<boolean | null>(null);
  const saved = savedOverride ?? recipe?.saved ?? false;

  return (
    <>
      <SubHeader
        title={recipe?.title}
        isTitleLoading={!recipe}
        right={
          <IconButton
            icon={<BookmarkIcon filled={saved} />}
            label="레시피 저장"
            aria-pressed={saved}
            disabled={!recipe}
            onClick={() => setSavedOverride(!saved)}
            className={styles.bookmark}
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
          <div className={styles.actions}>
            <Button
              disabled={!recipe}
              onClick={() => recipe && router.push(`${recipePath(recipe.id)}/cook`)}
            >
              요리 완료
            </Button>
            {/* 다시 받기: 늘 누를 수 있고 흐린 글자로만 보인다(domain-rules 4, 피그마 11/27). TODO: API 29를 보내고 새 결과 주소로 */}
            <button type="button" className={styles.regenerate} disabled={!recipe}>
              레시피 다시 받기{remainingCount !== undefined && ` ${remainingCount}회`}
            </button>
          </div>
        </BottomBar>
      )}
    </>
  );
}
