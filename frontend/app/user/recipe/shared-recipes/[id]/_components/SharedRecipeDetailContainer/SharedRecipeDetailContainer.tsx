"use client";

import { useState } from "react";
import Image from "next/image";

import { HeartIcon, MenuDotsVerticalIcon } from "@solar-icons/react/bold";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { IconButton } from "@/components/IconButton/IconButton";
import { SelectSheet } from "@/components/SelectSheet/SelectSheet";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";

import { ReadingIndicator } from "../../../../_components/ReadingIndicator/ReadingIndicator";
import { RecipeBody } from "../../../../_components/RecipeBody/RecipeBody";
import { useStepReader } from "../../../../_hooks/useStepReader";
import {
  REPORT_REASONS,
  type ReportReason,
  type SharedRecipeDetail,
} from "../../_lib/shared-recipe-detail";

import styles from "./SharedRecipeDetailContainer.module.css";

type SharedRecipeDetailContainerProps = {
  /** 공유 레시피 상세(API 52). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  recipe?: SharedRecipeDetail;
  /** 신고 시트를 처음부터 연다. 미리보기용 */
  initialReportOpen?: boolean;
};

/**
 * p4.1.4.1 공유 레시피 상세(완전 익명). 서브 헤더(← 제목 ⋮ 신고), 사진, 재료 준비(내 냉장고에 없는 재료는 빨간 테두리),
 * 만드는 방법, Tips!, 하트 + 저장 수, 아래 "레시피 저장"(부족한 재료는 장바구니로).
 * 하트와 "레시피 저장"은 같은 동작이다. 다시 누르면 저장 취소(저장 수 -1, 장바구니는 그대로)(domain-rules 7).
 * 상태: 기본 / 신고하기 시트 / 저장(하트 빨강, 저장 수 +1) / 스켈레톤(실제 라우트 처음 상태).
 */
export function SharedRecipeDetailContainer({
  recipe,
  initialReportOpen = false,
}: SharedRecipeDetailContainerProps) {
  const [isReportOpen, setIsReportOpen] = useState(initialReportOpen);
  // TODO: API 53(저장)·54(저장 취소)를 붙이면 응답의 saveCount·saved로 바꾼다. 저장하면 cartAddedCount 토스트(디자인 대기)
  const [savedOverride, setSavedOverride] = useState<boolean | null>(null);
  const reader = useStepReader(recipe?.steps);
  const isPlaying = reader.playingStepNo !== null;

  const saved = savedOverride ?? recipe?.saved ?? false;
  const saveCount = recipe ? recipe.saveCount + (saved === recipe.saved ? 0 : saved ? 1 : -1) : 0;
  const toggleSave = () => setSavedOverride(!saved);

  const handleReport = ([reason]: ReportReason[]) => {
    // TODO: API 55({ reason })를 보내고 완료 토스트. 같은 글을 또 신고하면 REPORT_ALREADY_EXISTS 토스트
    void reason;
    setIsReportOpen(false);
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
            label="신고하기"
            aria-haspopup="dialog"
            disabled={!recipe}
            onClick={() => setIsReportOpen(true)}
          />
        }
      />

      <main className={styles.main}>
        <div className={styles.photo}>
          {recipe ? (
            // 공유 사진 API는 로그인 쿠키가 필요해 Next 이미지 최적화를 거치지 않는다
            <Image
              src={recipe.imageUrl}
              alt={`${recipe.title} 사진`}
              fill
              sizes="(max-width: 480px) 100vw, 480px"
              priority
              unoptimized
              className={styles.image}
            />
          ) : (
            <Skeleton width="100%" height="100%" radius="0" />
          )}
        </div>

        <RecipeBody
          recipe={recipe}
          playingStepNo={reader.playingStepNo}
          onPlay={reader.play}
          onStop={reader.stop}
          className={styles.body}
        />

        {recipe && (
          <button type="button" className={styles.heart} aria-pressed={saved} onClick={toggleSave}>
            <span className={styles.saveCount}>
              {saveCount}
              <span className={styles.srOnly}>명 저장</span>
            </span>
            <HeartIcon className={styles.heartIcon} aria-hidden="true" />
            <span className={styles.srOnly}>레시피 저장</span>
          </button>
        )}
      </main>

      {isPlaying ? (
        <ReadingIndicator onStop={reader.stop} />
      ) : (
        <BottomBar bottomNotice="레시피 저장 시 부족한 재료는 장바구니로 들어가요">
          <Button disabled={!recipe} onClick={toggleSave}>
            레시피 저장
          </Button>
        </BottomBar>
      )}

      <SelectSheet<ReportReason>
        isOpen={isReportOpen}
        onClose={() => setIsReportOpen(false)}
        title="레시피 신고하기"
        options={REPORT_REASONS}
        defaultValues={[]}
        tone="danger"
        confirmLabel="신고하기"
        onConfirm={handleReport}
      />
    </>
  );
}
