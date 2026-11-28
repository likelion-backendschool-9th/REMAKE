"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import { BookmarkIcon as BookmarkFilledIcon } from "@solar-icons/react/bold";
import { BookmarkIcon } from "@solar-icons/react/linear";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { IconButton } from "@/components/IconButton/IconButton";
import { ResultState } from "@/components/ResultState/ResultState";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { TextButton } from "@/components/TextButton/TextButton";
import { Toast } from "@/components/Toast/Toast";
import { useTimedToast } from "@/hooks/useTimedToast";
import { getErrorMessage, getErrorMessageByCode } from "@/lib/error-messages";
import { SCREENS } from "@/lib/events/screens";
import { trackRecipeRegenerated } from "@/lib/events/tracker";
import {
  getRecipeMode,
  useRecipeDetailQuery,
  useRegenerateRecipeMutation,
} from "@/lib/queries/recipes";
import { useUsageQuotasQuery } from "@/lib/queries/usage-quotas";

import { ReadingIndicator } from "../../../../_components/ReadingIndicator/ReadingIndicator";
import { RecipeBody } from "../../../../_components/RecipeBody/RecipeBody";
import { RecipeGenerationStatus } from "../../../../_components/RecipeGenerationStatus/RecipeGenerationStatus";
import { useStepReader } from "../../../../_hooks/useStepReader";
import { RECIPE_HOME_PATH, recipePath, recipeResultPath } from "../../../../_lib/recipe";
import { type RecipeResult, toRecipeResult } from "../../_lib/recipe-result";

import styles from "./RecipeResultContainer.module.css";

type RecipeResultContainerProps = {
  /** 생성 결과(API 28·29 응답, 상세와 같은 모양). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  recipe?: RecipeResult;
  /** 오늘 남은 레시피 생성 횟수(API 27 recipe.remaining). "레시피 다시 받기 N회" */
  remainingCount?: number;
  /** 주소의 레시피 id. 양의 정수가 아니면 null(잘못된 주소). live일 때만 쓴다 */
  recipeId?: number | null;
  /** true면 상세(API 30)·남은 횟수(API 27)·다시 받기(API 29)를 실제로 부른다. 미리보기(app/dev)는 false라 요청을 보내지 않는다 */
  live?: boolean;
};

/**
 * p4.1.3 레시피 생성 결과. 서브 헤더(← 제목 + 저장 북마크), 재료 준비, 만드는 방법, Tips!,
 * 아래 "레시피 다시 받기 N회"(같은 조건으로 새로, 하루 횟수 차감, 늘 누를 수 있고 흐리게만 보임)와 "요리 완료".
 * 만든 레시피는 저장하기 전까지 목록에 안 보인다. 북마크로 저장하면 보충 재료가 장바구니로 간다(domain-rules 4).
 * 상태: 레시피 생성(저장 전) / 레시피 저장(저장 후) / 다시 받는 중·실패·횟수 소진(RecipeGenerationStatus) /
 * 잘못된 주소·불러오기 실패.
 * 횟수가 0이어도 다시 받기를 누를 수 있다. 서버가 429 GENERATION_LIMIT_EXCEEDED로 막으면 소진 화면을 보인다.
 * TODO: 북마크 저장(API 34·35)은 #106에서 붙인다. 지금은 화면 안에서만 바뀐다.
 */
export function RecipeResultContainer({
  recipe,
  remainingCount,
  recipeId = null,
  live = false,
}: RecipeResultContainerProps) {
  const router = useRouter();
  const toast = useTimedToast<string>();
  const mountedRef = useRef(false);
  /** 다시 받기 요청 번호. 이탈·재시도로 번호가 바뀐 옛 요청의 응답은 무시한다 */
  const attemptRef = useRef(0);
  const [regenerateError, setRegenerateError] = useState<unknown>(null);
  /** 다시 받는 중에 "기존 레시피로 돌아가기"로 빠져나왔는지. 이때는 응답이 와도 이동하지 않는다 */
  const [hasLeft, setHasLeft] = useState(false);
  // TODO: API 34(저장)·35(저장 취소)를 붙이면 응답으로 바꾼다(#106). 저장하면 cartAddedCount 토스트(디자인 대기)
  const [savedOverride, setSavedOverride] = useState<boolean | null>(null);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // 생성 직후에는 캐시(setQueryData)를 바로 쓰고, 새로고침·직접 들어오면 API 30으로 불러온다
  const detailQuery = useRecipeDetailQuery(recipeId, { enabled: live && !recipe });
  const quotaQuery = useUsageQuotasQuery({ enabled: live && remainingCount === undefined });
  const regenerateMutation = useRegenerateRecipeMutation();

  const detailData = detailQuery.data;
  const effectiveRecipe = useMemo<RecipeResult | undefined>(
    () => recipe ?? (live && detailData ? toRecipeResult(detailData) : undefined),
    [recipe, live, detailData],
  );
  const effectiveRemainingCount =
    remainingCount ?? (live ? quotaQuery.data?.recipe.remaining : undefined);

  const reader = useStepReader(effectiveRecipe?.steps);
  const isPlaying = reader.playingStepNo !== null;
  const saved = savedOverride ?? effectiveRecipe?.saved ?? false;
  const regenerateLabel =
    effectiveRemainingCount === undefined
      ? "레시피 다시 받기"
      : `레시피 다시 받기 ${effectiveRemainingCount}회`;

  const startRegenerate = () => {
    if (!live || !effectiveRecipe || regenerateMutation.isPending) return;
    const targetId = effectiveRecipe.id;
    attemptRef.current += 1;
    const attempt = attemptRef.current;
    setHasLeft(false);
    setRegenerateError(null);

    trackRecipeRegenerated(SCREENS.recipeResult, {
      recipe_mode: getRecipeMode(targetId) ?? "quick",
    });
    regenerateMutation.mutate(
      { recipeId: targetId, screen: SCREENS.recipeResult },
      {
        onSuccess: (data) => {
          if (!mountedRef.current || attemptRef.current !== attempt) return;
          router.replace(recipeResultPath(data.id));
        },
        onError: (error) => {
          // 실패한 생성은 횟수를 차감하지 않으므로 횟수 캐시는 건드리지 않는다
          if (!mountedRef.current || attemptRef.current !== attempt) return;
          setRegenerateError(error);
          toast.show(getErrorMessage(error));
        },
      },
    );
  };

  /** 다시 받기 화면에서 빠져나와 기존 레시피로 돌아온다. 아직 오는 중인 응답은 무시한다 */
  const leaveRegenerate = () => {
    attemptRef.current += 1;
    setHasLeft(true);
    setRegenerateError(null);
  };

  const isRegeneratingActive = live && regenerateMutation.isPending && !hasLeft;
  const hasRegenerateError = live && regenerateError !== null;

  if (isRegeneratingActive || hasRegenerateError) {
    return (
      <>
        <RecipeGenerationStatus
          status={hasRegenerateError ? "error" : "loading"}
          headerTitle="레시피 다시 받기"
          error={regenerateError}
          onRetry={startRegenerate}
          onBack={leaveRegenerate}
          backLabel="기존 레시피로 돌아가기"
        />
        <ToastArea message={toast.value} isLeaving={toast.isLeaving} onLeft={toast.remove} />
      </>
    );
  }

  const isInvalidId = recipeId === null;
  if (live && !recipe && (isInvalidId || detailQuery.isError)) {
    return (
      <>
        <SubHeader onBack={() => router.push(RECIPE_HOME_PATH)} />
        <main className={styles.state}>
          <ResultState
            variant="error"
            title="레시피를 불러오지 못했어요"
            description={
              isInvalidId
                ? getErrorMessageByCode("RECIPE_NOT_FOUND")
                : getErrorMessage(detailQuery.error)
            }
            action={
              <div className={styles.stateActions}>
                {!isInvalidId && (
                  <Button onClick={() => void detailQuery.refetch()}>다시 시도</Button>
                )}
                <TextButton tone="neutral" onClick={() => router.push(RECIPE_HOME_PATH)}>
                  레시피 홈으로
                </TextButton>
              </div>
            }
          />
        </main>
      </>
    );
  }

  return (
    <>
      <SubHeader
        title={effectiveRecipe?.title}
        isTitleLoading={!effectiveRecipe}
        right={
          <IconButton
            icon={saved ? <BookmarkFilledIcon /> : <BookmarkIcon />}
            label="레시피 저장"
            aria-pressed={saved}
            disabled={!effectiveRecipe}
            onClick={() => setSavedOverride(!saved)}
            className={styles.bookmark}
          />
        }
      />

      <main className={styles.main}>
        <RecipeBody
          recipe={effectiveRecipe}
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
            {/* 다시 받기: 횟수가 0이어도 누를 수 있고 흐리게만 보인다(domain-rules 4). 소진은 서버 429로 알린다 */}
            <Button
              className={styles.regenerate}
              disabled={!effectiveRecipe || regenerateMutation.isPending}
              onClick={startRegenerate}
            >
              {regenerateLabel}
            </Button>
            <Button
              disabled={!effectiveRecipe}
              onClick={() =>
                effectiveRecipe && router.push(`${recipePath(effectiveRecipe.id)}/cook`)
              }
            >
              요리 완료
            </Button>
          </div>
        </BottomBar>
      )}

      <ToastArea message={toast.value} isLeaving={toast.isLeaving} onLeft={toast.remove} />
    </>
  );
}

/** 문구를 바꿔 끼우면 읽히도록 항상 그려 두는 영역(blueprint §6.3) */
function ToastArea({
  message,
  isLeaving,
  onLeft,
}: {
  message: string | null;
  isLeaving: boolean;
  onLeft: () => void;
}) {
  return (
    <div className={styles.toastArea} aria-live="polite">
      {message && (
        <Toast variant="error" isLeaving={isLeaving} onLeft={onLeft}>
          {message}
        </Toast>
      )}
    </div>
  );
}
