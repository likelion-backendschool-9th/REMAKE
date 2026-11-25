"use client";

import { useState } from "react";
import Link from "next/link";

import { ArrowRightIcon } from "@solar-icons/react/linear";

import { ConfirmModal } from "@/components/ConfirmModal/ConfirmModal";
import { MainHeader } from "@/components/MainHeader/MainHeader";
import { MAIN_TABS, NOTIFICATIONS_HREF, SETTINGS_HREF } from "@/lib/navigation";

import { CUSTOM_RECIPE_PATH, RECIPE_HOME_PATH } from "../../../_lib/recipe";
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
}: RecipesContainerProps) {
  const [isQuickModalOpen, setIsQuickModalOpen] = useState(initialQuickModalOpen);

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
          // TODO: API 28({ mode: "QUICK" })을 보내고 생성 결과(p4.1.3)로 이동. 기다리는 동안·실패·횟수 소진은 그 화면 상태
          setIsQuickModalOpen(false);
        }}
        title="빠른 레시피 생성"
        message="가구원 수와 냉장고의 재료에 맞게 레시피를 생성합니다. 레시피를 생성하면 하루 생성 횟수를 차감합니다. 생성하시겠습니까?"
        confirmLabel="생성하기"
      />
    </>
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
