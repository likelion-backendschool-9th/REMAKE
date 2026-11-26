import { PlayCircleIcon, StopCircleIcon } from "@solar-icons/react/linear";

import { Skeleton } from "@/components/Skeleton/Skeleton";

import { formatRecipeAmount, type RecipeContent, recipeMeta } from "../../_lib/recipe";

import styles from "./RecipeBody.module.css";

type RecipeBodyProps = {
  /** 레시피 본문. 없으면 불러오는 중(피그마 p4.1.1-레시피 상세-스켈레톤) */
  recipe?: RecipeContent;
  /** 지금 읽는 단계 번호(useStepReader). null이면 재생하지 않는 중 */
  playingStepNo?: number | null;
  /** "재생하기"·"중단하기". 없으면 재생 버튼을 그리지 않는다(공유 작성의 읽기 전용 본문) */
  onPlay?: () => void;
  onStop?: () => void;
  className?: string;
};

/**
 * 레시피 본문: 재료 준비(N인분 · N분 이내, 재료 칩), 만드는 방법(재생하기·번호 단계), Tips!.
 * 내 레시피 상세(p4.1.1)·생성 결과(p4.1.3)·공유 상세(p4.1.4.1)·공유 작성(p4.1.1.1)이 같이 쓴다.
 * 공유 상세에서는 내 냉장고에 없는 재료(have=false)를 빨간 테두리로 보인다(domain-rules 7).
 */
export function RecipeBody({
  recipe,
  playingStepNo = null,
  onPlay,
  onStop,
  className,
}: RecipeBodyProps) {
  const isPlaying = playingStepNo !== null;

  return (
    <div className={className}>
      <section className={styles.ingredients} aria-labelledby="recipe-ingredients-title">
        <div className={styles.titleRow}>
          <h2 id="recipe-ingredients-title" className={styles.title}>
            재료 준비
          </h2>
          {recipe ? (
            <p className={styles.meta}>
              {recipeMeta(recipe.servings, recipe.cookMinutes).map((text) => (
                <span key={text} className={styles.metaItem}>
                  {text}
                </span>
              ))}
            </p>
          ) : (
            <Skeleton width="6.875rem" height="1rem" />
          )}
        </div>
        {recipe ? (
          <ul className={styles.chips}>
            {recipe.ingredients.map((ingredient, index) => (
              <li
                key={`${ingredient.name}-${index}`}
                className={styles.chip}
                data-missing={ingredient.have === false || undefined}
              >
                <span className={styles.chipName}>{ingredient.name}</span>
                <span className={styles.chipAmount}>
                  {formatRecipeAmount(ingredient.amount, ingredient.unit)}
                </span>
                {ingredient.have === false && <span className={styles.srOnly}>(없는 재료)</span>}
              </li>
            ))}
          </ul>
        ) : (
          <div className={styles.chips} aria-busy="true">
            <Skeleton width="5.5625rem" height="1.75rem" />
            <Skeleton width="5.5625rem" height="1.75rem" />
            <Skeleton width="5.5625rem" height="1.75rem" />
          </div>
        )}
      </section>

      <section className={styles.steps} aria-labelledby="recipe-steps-title">
        <div className={styles.titleRow}>
          <h2 id="recipe-steps-title" className={styles.title}>
            만드는 방법
          </h2>
          {onPlay &&
            (isPlaying ? (
              <button type="button" className={styles.play} data-playing="" onClick={onStop}>
                중단하기
                <StopCircleIcon className={styles.playIcon} aria-hidden="true" />
              </button>
            ) : (
              <button type="button" className={styles.play} disabled={!recipe} onClick={onPlay}>
                재생하기
                <PlayCircleIcon className={styles.playIcon} aria-hidden="true" />
              </button>
            ))}
        </div>
        {recipe ? (
          <ol className={styles.stepList}>
            {recipe.steps.map((step) => (
              <li
                key={step.stepNo}
                className={styles.step}
                aria-current={step.stepNo === playingStepNo ? "step" : undefined}
              >
                <span className={styles.stepNo} aria-hidden="true">
                  {step.stepNo}
                </span>
                <p className={styles.stepText}>{step.content}</p>
              </li>
            ))}
          </ol>
        ) : (
          <div className={styles.stepList} aria-busy="true">
            <Skeleton width="100%" height="2.875rem" />
            <Skeleton width="100%" height="2.875rem" />
          </div>
        )}
      </section>

      {recipe?.tips && (
        <section className={styles.tips} aria-labelledby="recipe-tips-title">
          <h2 id="recipe-tips-title" className={styles.tipsTitle}>
            Tips!
          </h2>
          <ul className={styles.tipsList}>
            <li>{recipe.tips}</li>
          </ul>
        </section>
      )}
    </div>
  );
}
