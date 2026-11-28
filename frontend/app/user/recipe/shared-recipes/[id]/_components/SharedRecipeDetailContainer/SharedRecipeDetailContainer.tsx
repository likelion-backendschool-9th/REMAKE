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
import { Toast } from "@/components/Toast/Toast";
import { useTimedToast } from "@/hooks/useTimedToast";

import { RecipeBody } from "../../../../_components/RecipeBody/RecipeBody";
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
  /** 부족한 재료 토스트를 처음부터 띄운다(장바구니에 담은 개수). 미리보기용 */
  initialCartAddedCount?: number;
};

/**
 * p4.1.4.1 공유 레시피 상세(완전 익명). 서브 헤더(← 제목 ⋮ 신고), 사진, 재료 준비(내 냉장고에 없는 재료는 빨간 테두리),
 * 만드는 방법(읽어 주기 없음, 남이 공유한 글이라 내 레시피 상세·생성 결과에만 둔다), Tips!, 하트 + 저장 수,
 * 아래 "레시피 저장"(부족한 재료는 장바구니로). 저장한 뒤에는 빨간 "저장 해제"로 바뀐다.
 * 하트와 아래 버튼은 같은 동작이다. 다시 누르면 저장 취소(저장 수 -1, 장바구니는 그대로)(domain-rules 7).
 * 저장할 때 장바구니에 담긴 재료가 있으면 "부족한 재료 N개를 장바구니에 담았어요" 토스트(0개면 안 띄움).
 * 상태: 기본 / 신고하기 시트 / 저장(하트 빨강, 저장 수 +1) / 저장 시 부족한 재료 토스트 / 스켈레톤(실제 라우트 처음 상태).
 */
export function SharedRecipeDetailContainer({
  recipe,
  initialReportOpen = false,
  initialCartAddedCount,
}: SharedRecipeDetailContainerProps) {
  const [isReportOpen, setIsReportOpen] = useState(initialReportOpen);
  // TODO: API 53(저장)·54(저장 취소)를 붙이면 응답의 saveCount·saved로 바꾼다
  const [savedOverride, setSavedOverride] = useState<boolean | null>(null);
  const cartToast = useTimedToast<number>(initialCartAddedCount ?? null);

  const saved = savedOverride ?? recipe?.saved ?? false;
  const saveCount = recipe ? recipe.saveCount + (saved === recipe.saved ? 0 : saved ? 1 : -1) : 0;

  const toggleSave = () => {
    setSavedOverride(!saved);
    if (saved) {
      // 저장 취소: 장바구니는 그대로지만 "담았어요" 토스트는 더 맞지 않아 내린다
      cartToast.dismiss();
      return;
    }
    // TODO: API 53 응답의 cartAddedCount로 바꾼다. 지금은 내 냉장고에 없는 재료 수로 대신한다
    const cartAddedCount = recipe?.ingredients.filter((item) => item.have === false).length ?? 0;
    if (cartAddedCount > 0) cartToast.show(cartAddedCount);
  };

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

        <RecipeBody recipe={recipe} className={styles.body} />

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

      <BottomBar
        bottomNotice="레시피 저장 시 부족한 재료는 장바구니로 들어가요"
        className={styles.bottom}
      >
        {/* 저장 전 파란 "레시피 저장", 저장 후 빨간 "저장 해제"(피그마 6115-8022) */}
        <Button tone={saved ? "danger" : "primary"} disabled={!recipe} onClick={toggleSave}>
          {saved ? "저장 해제" : "레시피 저장"}
        </Button>
      </BottomBar>

      {/* 문구를 바꿔 끼우면 읽히도록 항상 그려 두는 영역(blueprint §6.3) */}
      <div className={styles.toastArea} role="status" aria-live="polite">
        {cartToast.value !== null && (
          <Toast variant="success" isLeaving={cartToast.isLeaving} onLeft={cartToast.remove}>
            부족한 재료 {cartToast.value}개를 장바구니에 담았어요
          </Toast>
        )}
      </div>

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
