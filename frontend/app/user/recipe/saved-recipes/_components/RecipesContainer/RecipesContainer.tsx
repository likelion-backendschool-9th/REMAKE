"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { ArrowRightIcon } from "@solar-icons/react/linear";

import { ConfirmModal } from "@/components/ConfirmModal/ConfirmModal";
import { MainHeader } from "@/components/MainHeader/MainHeader";
import { Toast } from "@/components/Toast/Toast";
import { useTimedToast } from "@/hooks/useTimedToast";
import { getErrorMessage } from "@/lib/error-messages";
import { SCREENS } from "@/lib/events/screens";
import { trackRecipeGenerateStarted } from "@/lib/events/tracker";
import { MAIN_TABS, NOTIFICATIONS_HREF, SETTINGS_HREF } from "@/lib/navigation";
import { useProfileQuery } from "@/lib/queries/profile";
import { useGenerateRecipeMutation } from "@/lib/queries/recipes";

import { RecipeGenerationStatus } from "../../../_components/RecipeGenerationStatus/RecipeGenerationStatus";
import { CUSTOM_RECIPE_PATH, RECIPE_HOME_PATH, recipeResultPath } from "../../../_lib/recipe";
import type { SavedRecipeList, WeeklyTopItem } from "../../_lib/saved-recipes";
import { SavedRecipeSection } from "../SavedRecipeSection/SavedRecipeSection";
import { WeeklyTopSection } from "../WeeklyTopSection/WeeklyTopSection";

import styles from "./RecipesContainer.module.css";

type RecipesContainerProps = {
  /** 금주의 레시피(API 49). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  weeklyTop?: WeeklyTopItem[];
  /** 저장한 레시피(API 33). 없으면 불러오는 중 */
  savedRecipes?: SavedRecipeList;
  /** 가장 급한 알림 한 줄(API 43). 없으면 알림 띠를 그리지 않는다 */
  notice?: { message: string; unreadCount: number };
  /** 빠른 레시피 생성 모달을 처음부터 연다. 미리보기용 */
  initialQuickModalOpen?: boolean;
  /** true면 빠른 생성(API 28)과 프로필(API 5)을 실제로 부른다. 미리보기(app/dev)는 false라 요청을 보내지 않는다 */
  live?: boolean;
};

/**
 * p4.1 레시피. 위: 빠른·커스텀 레시피 생성 카드. 가운데: 금주의 레시피 TOP 3(전체 보기 → p4.1.4). 아래: 저장한 레시피
 * (전체·즐겨찾기·바로 가능, 최신 저장순, 줄을 누르면 레시피 상세).
 * 상태: 기본(p4.1-레시피) / 빠른 레시피 생성 모달 / 불러오는 중(p4.1-레시피-스켈레톤) / 빈 리스트(p4.1-레시피-빈 리스트).
 */
export function RecipesContainer({
  weeklyTop,
  savedRecipes,
  notice,
  initialQuickModalOpen = false,
  live = false,
}: RecipesContainerProps) {
  const router = useRouter();
  const [isQuickModalOpen, setIsQuickModalOpen] = useState(initialQuickModalOpen);
  const [generationError, setGenerationError] = useState<unknown>(null);
  /** 생성 중에 뒤로 가기로 빠져나왔는지. 이때는 응답이 와도 결과 화면으로 보내지 않는다 */
  const [hasLeft, setHasLeft] = useState(false);
  const toast = useTimedToast<string>();
  const mountedRef = useRef(false);
  /** 생성 요청 번호. 뒤로 가기·재시도로 번호가 바뀐 옛 요청의 응답은 무시한다 */
  const attemptRef = useRef(0);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // E39에 필요한 가구원 수·재료 보충 설정(API 5). 아직 못 불렀으면 1·false
  const { data: profile } = useProfileQuery({ enabled: live });
  const generateMutation = useGenerateRecipeMutation();

  const startQuickGeneration = () => {
    if (generateMutation.isPending) return;
    attemptRef.current += 1;
    const attempt = attemptRef.current;
    setHasLeft(false);
    setGenerationError(null);

    trackRecipeGenerateStarted(SCREENS.recipeCreate, {
      recipe_mode: "quick",
      servings: profile?.householdSize ?? 1,
      option_count: 0,
      supplement_enabled: profile?.recipeSupplementEnabled ?? false,
    });
    generateMutation.mutate(
      { body: { mode: "QUICK" }, screen: SCREENS.recipeCreate },
      {
        onSuccess: (data) => {
          if (!mountedRef.current || attemptRef.current !== attempt) return;
          router.push(recipeResultPath(data.id));
        },
        onError: (error) => {
          if (!mountedRef.current || attemptRef.current !== attempt) return;
          setGenerationError(error);
          toast.show(getErrorMessage(error));
        },
      },
    );
  };

  /** 생성 화면에서 빠져나와 레시피 홈으로 돌아온다. 아직 오는 중인 응답은 무시한다 */
  const leaveGeneration = () => {
    attemptRef.current += 1;
    setHasLeft(true);
    setGenerationError(null);
  };

  const isGenerating = live && generateMutation.isPending && !hasLeft;
  const hasGenerationError = live && generationError !== null;

  if (isGenerating || hasGenerationError) {
    return (
      <>
        <RecipeGenerationStatus
          status={hasGenerationError ? "error" : "loading"}
          error={generationError}
          onRetry={startQuickGeneration}
          onBack={leaveGeneration}
        />
        <ToastArea message={toast.value} isLeaving={toast.isLeaving} onLeft={toast.remove} />
      </>
    );
  }

  return (
    <>
      <MainHeader
        tabs={MAIN_TABS}
        activeHref={RECIPE_HOME_PATH}
        settingsHref={SETTINGS_HREF}
        notice={notice && { ...notice, href: NOTIFICATIONS_HREF }}
      />
      <main className={styles.main}>
        <section className={styles.hero} aria-labelledby="recipe-hero-title">
          <div className={styles.heroBackground} aria-hidden="true">
            <span className={styles.heroGlowLeft} />
            <span className={styles.heroGlowRight} />
          </div>
          <p className={styles.heroEyebrow}>소중한 시간을 아낄 수 있도록</p>
          <h1 id="recipe-hero-title" className={styles.heroTitle}>
            레시피를 만들어 드릴게요
          </h1>

          <div className={styles.generators}>
            {/* 빠른 생성: 확인 모달 → 생성(API 28, 하루 횟수 차감) */}
            <button
              type="button"
              className={styles.generator}
              data-kind="quick"
              aria-haspopup="dialog"
              onClick={() => setIsQuickModalOpen(true)}
            >
              <GeneratorText
                label="빠른 레시피 생성"
                lines={["냉장고 재료 기반으로", "레시피 생성하기"]}
              />
            </button>
            <Link href={CUSTOM_RECIPE_PATH} className={styles.generator} data-kind="custom">
              <GeneratorText
                label="커스텀 레시피 생성"
                lines={["사용자 취향 기반으로", "레시피 생성하기"]}
              />
            </Link>
          </div>
        </section>

        <WeeklyTopSection items={weeklyTop} />
        <SavedRecipeSection list={savedRecipes} />
      </main>

      {/* 피그마 p4.1-레시피-빠른 레시피 생성 */}
      <ConfirmModal
        isOpen={isQuickModalOpen}
        onClose={() => setIsQuickModalOpen(false)}
        onConfirm={() => {
          if (!live) {
            setIsQuickModalOpen(false);
            return;
          }
          // 중복 클릭 방지
          if (generateMutation.isPending) return;
          // 모달을 먼저 닫고 요청을 보낸다(blueprint §7.1)
          setIsQuickModalOpen(false);
          startQuickGeneration();
        }}
        title="빠른 레시피 생성"
        message="가구원 수와 냉장고의 재료에 맞게 레시피를 생성합니다. 레시피를 생성하면 하루 생성 횟수를 차감합니다. 생성하시겠습니까?"
        confirmLabel="생성하기"
      />

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

/** 생성 카드 안 글: 작은 이름, 두 줄 제목, 오른쪽 아래 화살표 */
function GeneratorText({ label, lines }: { label: string; lines: readonly [string, string] }) {
  return (
    <>
      <span className={styles.generatorLabel}>{label}</span>
      <span className={styles.generatorTitle}>
        {lines[0]}
        <br />
        {lines[1]}
      </span>
      <span className={styles.generatorArrow} aria-hidden="true">
        <ArrowRightIcon strokeWidth={2} />
      </span>
    </>
  );
}
