"use client";

import { useState } from "react";
import Link from "next/link";

import { StarIcon } from "@solar-icons/react/bold";

import { Chip } from "@/components/Chip/Chip";
import { Dropdown } from "@/components/Dropdown/Dropdown";
import { EmptyState } from "@/components/EmptyState/EmptyState";
import { IconButton } from "@/components/IconButton/IconButton";
import { Skeleton } from "@/components/Skeleton/Skeleton";

import { formatIngredientSummary, recipePath } from "../../../_lib/recipe";
import {
  SAVED_RECIPE_FILTERS,
  SAVED_RECIPE_SORT_LABEL,
  type SavedRecipeFilter,
  type SavedRecipeList,
  type SavedRecipeRow,
  type SavedRecipeSort,
} from "../../_lib/saved-recipes";

import styles from "./SavedRecipeSection.module.css";

/**
 * 저장한 레시피(API 33): "저장한 레시피 N", 칩(전체·즐겨찾기·바로 가능), 정렬, 줄(제목·재료 요약 +N·별).
 * 줄을 누르면 레시피 상세(p4.1.1), 별은 즐겨찾기 켜고 끄기(API 36·37). list가 없으면 불러오는 중.
 */
export function SavedRecipeSection({ list }: { list?: SavedRecipeList }) {
  const [filter, setFilter] = useState<SavedRecipeFilter>("ALL");
  const [sort] = useState<SavedRecipeSort>("SAVED_DESC");
  // TODO: API 36·37을 붙이면 성공한 뒤 목록을 다시 받는다. 지금은 누른 값만 화면에 덮어 둔다
  const [favoriteOverrides, setFavoriteOverrides] = useState<ReadonlyMap<number, boolean>>(
    () => new Map(),
  );
  const isFavorite = (row: SavedRecipeRow) => favoriteOverrides.get(row.recipeId) ?? row.favorite;

  // TODO: 칩·정렬을 바꾸면 API 33(?filter=&sort=)으로 다시 받는다. 바로 가능은 서버만 알아서 지금은 받은 목록을 그대로 둔다
  const rows = list?.content.filter((row) => filter !== "FAVORITE" || isFavorite(row)) ?? [];

  const toggleFavorite = (row: SavedRecipeRow) =>
    setFavoriteOverrides((prev) => new Map(prev).set(row.recipeId, !isFavorite(row)));

  return (
    <section className={styles.section} aria-labelledby="saved-recipes-title">
      <h2 id="saved-recipes-title" className={styles.title}>
        저장한 레시피
        {list && <span className={styles.count}>{list.totalCount}</span>}
      </h2>

      <div className={styles.controls}>
        <div className={styles.filters}>
          {SAVED_RECIPE_FILTERS.map((option) => (
            <Chip
              key={option.value}
              variant="soft"
              isSelected={filter === option.value}
              onClick={() => setFilter(option.value)}
              className={styles.chip}
            >
              {option.label}
            </Chip>
          ))}
        </div>
        {/* TODO: 정렬 시트(최신·오래된 저장순)는 디자인 v2에 없어 보류. 디자인이 나오면 SelectSheet를 연다 */}
        <Dropdown>{SAVED_RECIPE_SORT_LABEL[sort]}</Dropdown>
      </div>

      {!list ? (
        <div className={styles.skeletons} aria-busy="true">
          <Skeleton width="100%" height="2.75rem" />
          <Skeleton width="100%" height="2.75rem" />
        </div>
      ) : rows.length === 0 ? (
        // 피그마 p4.1-레시피-빈 리스트. 칩으로 거른 결과만 없을 때 문구는 피그마에 없어 따로 둔다
        <EmptyState className={styles.empty}>
          {list.content.length === 0 ? "레시피를 추가해 주세요" : "조건에 맞는 레시피가 없어요"}
        </EmptyState>
      ) : (
        <ul className={styles.rows}>
          {rows.map((row) => (
            <li key={row.recipeId} className={styles.row}>
              <Link href={recipePath(row.recipeId)} className={styles.rowLink}>
                <span className={styles.name}>{row.title}</span>
                <span className={styles.ingredients}>
                  {formatIngredientSummary(row.ingredientSummary, row.extraIngredientCount)}
                </span>
              </Link>
              <IconButton
                icon={<StarIcon />}
                label={`${row.title} 즐겨찾기`}
                aria-pressed={isFavorite(row)}
                onClick={() => toggleFavorite(row)}
                className={styles.star}
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
