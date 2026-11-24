"use client";

import { useState } from "react";

import { AddIcon } from "@solar-icons/react/linear";

import { Chip } from "@/components/Chip/Chip";
import { EmptyState } from "@/components/EmptyState/EmptyState";
import { ExpiryLabel, type ExpiryStatus } from "@/components/ExpiryLabel/ExpiryLabel";
import { IconButton } from "@/components/IconButton/IconButton";
import { IngredientRow } from "@/components/IngredientRow/IngredientRow";
import { MainHeader } from "@/components/MainHeader/MainHeader";
import { SelectSheet } from "@/components/SelectSheet/SelectSheet";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import {
  CATEGORIES,
  type CategoryCode,
  formatAmount,
  getCategory,
  getCategoryImage,
  getStorage,
  type StorageCode,
  STORAGES,
} from "@/lib/ingredients";
import { MAIN_TABS, NOTIFICATIONS_HREF, SETTINGS_HREF } from "@/lib/navigation";

import {
  type FridgeIngredient,
  SAMPLE_INGREDIENTS,
  SAMPLE_NOTICE,
  STATUS_LABEL,
  STATUS_ORDER,
} from "../../_lib/fridge";
import { FridgeHero } from "../FridgeHero/FridgeHero";
import { UseIngredientSheet } from "../UseIngredientSheet/UseIngredientSheet";

import styles from "./FridgeContainer.module.css";

const LIST_PATH = "/user/fridge/ingredients";
const STORAGE_OPTIONS = STORAGES.map(({ code, label }) => ({ value: code, label }));
const CATEGORY_OPTIONS = CATEGORIES.map(({ code, name }) => ({ value: code, label: name }));

/** 위치·종류 거르기는 한 번에 하나만(domain-rules 2) */
type PlaceFilter =
  { kind: "storage"; value: StorageCode } | { kind: "category"; value: CategoryCode };

type FridgeSheet = "location" | "category";

type FridgeContainerProps = {
  /** 재료 목록. 기본은 마크업 확인용 값(목록 API를 붙이기 전) */
  ingredients?: FridgeIngredient[];
  /** 불러오는 중(p2-냉장고-스켈레톤). 미리보기용 */
  isLoading?: boolean;
  /** 처음부터 열어 둘 시트. 미리보기용 */
  initialSheet?: FridgeSheet | null;
  /** 처음부터 사용 시트를 열 재료 id. 미리보기용(p2-냉장고-재료 사용 여부) */
  initialUseId?: number;
};

/**
 * p2 냉장고(메인). 메인 헤더, 재료 개수·요약 카드, 상태·위치·종류로 거른 재료 목록.
 * 행을 누르면 사용·폐기 시트, 오른쪽 >는 재료 수정, +는 재료 추가(domain-rules 2).
 * 요약 카드·위치·종류 칩은 다시 누르면 거르기가 풀린다(토글).
 * 상태: 기본 / 불러오는 중 / 재료 사용 시트 / 재료 위치 시트 / 재료 종류 시트 / 빈 냉장고 / 거른 결과 없음.
 */
export function FridgeContainer({
  ingredients = SAMPLE_INGREDIENTS,
  isLoading = false,
  initialSheet = null,
  initialUseId,
}: FridgeContainerProps) {
  const [statusFilter, setStatusFilter] = useState<ExpiryStatus | null>(null);
  const [placeFilter, setPlaceFilter] = useState<PlaceFilter | null>(null);
  const [sheet, setSheet] = useState<FridgeSheet | null>(initialSheet);
  const [usingId, setUsingId] = useState<number | null>(initialUseId ?? null);

  const counts = Object.fromEntries(
    STATUS_ORDER.map((status) => [
      status,
      ingredients.filter((item) => item.expiryStatus === status).length,
    ]),
  ) as Record<ExpiryStatus, number>;

  // TODO: 거르기는 목록 API 쿼리로 보낸다(서버가 가까운 소비기한 순으로 줌). 지금은 예시 값을 화면에서 거른다
  const visible = ingredients.filter(
    (item) =>
      (!statusFilter || item.expiryStatus === statusFilter) &&
      (!placeFilter ||
        (placeFilter.kind === "storage"
          ? item.storage === placeFilter.value
          : item.categoryCode === placeFilter.value)),
  );
  // 목록 제목: 단계를 골랐으면 그 단계, 아니면 위험 재료 개수(피그마 p2-냉장고)
  const titleStatus = statusFilter ?? "DANGER";
  const titleCount = statusFilter ? visible.length : counts.DANGER;
  const usingIngredient = ingredients.find((item) => item.id === usingId) ?? null;

  const storageLabel =
    placeFilter?.kind === "storage" ? getStorage(placeFilter.value).label : "재료 위치";
  const categoryLabel =
    placeFilter?.kind === "category" ? getCategory(placeFilter.value).name : "재료 종류";

  return (
    <>
      <MainHeader
        tabs={MAIN_TABS}
        activeHref={LIST_PATH}
        settingsHref={SETTINGS_HREF}
        notice={isLoading ? undefined : { ...SAMPLE_NOTICE, href: NOTIFICATIONS_HREF }}
        isNoticeLoading={isLoading}
      />
      <main className={styles.main} aria-busy={isLoading || undefined}>
        <FridgeHero
          total={ingredients.length}
          counts={counts}
          selectedStatus={statusFilter}
          onSelectStatus={setStatusFilter}
          isLoading={isLoading}
        />

        <section className={styles.panel} aria-labelledby="fridge-list-title">
          <div className={styles.titleRow}>
            {isLoading ? (
              <>
                <h2 id="fridge-list-title" className={styles.srOnly}>
                  재료 목록
                </h2>
                <Skeleton width="7.5625rem" height="1.5rem" />
              </>
            ) : (
              <h2 id="fridge-list-title" className={styles.title}>
                <span data-status={titleStatus} className={styles.titleCount}>
                  {titleCount}개
                </span>
                의 {STATUS_LABEL[titleStatus]} 재료
              </h2>
            )}
            <IconButton
              href="/user/fridge/ingredients/new"
              icon={<AddIcon strokeWidth={1.8} />}
              iconSize={20}
              label="재료 추가"
              className={styles.add}
            />
          </div>

          <div className={styles.filters}>
            {/* 고른 칩을 다시 누르면 거르기를 풀고 전체로(토글) */}
            <Chip
              isSelected={placeFilter?.kind === "storage"}
              onClick={() =>
                placeFilter?.kind === "storage" ? setPlaceFilter(null) : setSheet("location")
              }
            >
              {storageLabel}
            </Chip>
            <Chip
              isSelected={placeFilter?.kind === "category"}
              onClick={() =>
                placeFilter?.kind === "category" ? setPlaceFilter(null) : setSheet("category")
              }
            >
              {categoryLabel}
            </Chip>
          </div>

          {isLoading ? (
            <div className={styles.skeletons}>
              <Skeleton width="100%" height="2.25rem" />
              <Skeleton width="100%" height="2.25rem" />
            </div>
          ) : visible.length === 0 ? (
            <EmptyState>
              {/* 피그마 p2-냉장고-빈 리스트. 거른 결과만 없을 때 문구는 피그마에 없어 따로 둔다 */}
              {ingredients.length === 0 ? "냉장고에 재료가 없어요" : "조건에 맞는 재료가 없어요"}
            </EmptyState>
          ) : (
            <ul className={styles.list}>
              {visible.map((item) => (
                <li key={item.id}>
                  <IngredientRow
                    name={item.name}
                    imageSrc={getCategoryImage(item.categoryCode)}
                    description={`${getStorage(item.storage).place} · ${formatAmount(item.amount, item.unit)}`}
                    trailing={<ExpiryLabel status={item.expiryStatus} text={item.dDay} />}
                    onClick={() => setUsingId(item.id)}
                    editHref={`/user/fridge/ingredients/${item.id}/edit`}
                  />
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>

      <UseIngredientSheet ingredient={usingIngredient} onClose={() => setUsingId(null)} />
      <SelectSheet
        isOpen={sheet === "location"}
        onClose={() => setSheet(null)}
        title="재료 위치"
        options={STORAGE_OPTIONS}
        defaultValues={placeFilter?.kind === "storage" ? [placeFilter.value] : []}
        onConfirm={([value]) => {
          setPlaceFilter({ kind: "storage", value });
          setSheet(null);
        }}
      />
      <SelectSheet
        isOpen={sheet === "category"}
        onClose={() => setSheet(null)}
        title="재료 종류"
        options={CATEGORY_OPTIONS}
        defaultValues={placeFilter?.kind === "category" ? [placeFilter.value] : []}
        confirmLabel="완료"
        onConfirm={([value]) => {
          setPlaceFilter({ kind: "category", value });
          setSheet(null);
        }}
      />
    </>
  );
}
