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
import { Toast } from "@/components/Toast/Toast";
import { useTimedToast } from "@/hooks/useTimedToast";
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
import { UseIngredientSheet, type UseKind } from "../UseIngredientSheet/UseIngredientSheet";

import styles from "./FridgeContainer.module.css";

const LIST_PATH = "/user/fridge/ingredients";
const STORAGE_OPTIONS = STORAGES.map(({ code, label }) => ({ value: code, label }));
const CATEGORY_OPTIONS = CATEGORIES.map(({ code, name }) => ({ value: code, label: name }));

/** 위치·종류 거르기는 한 번에 하나만(domain-rules 2) */
type PlaceFilter =
  { kind: "storage"; value: StorageCode } | { kind: "category"; value: CategoryCode };

type FridgeSheet = "location" | "category";

/** 사용·폐기 결과 토스트(피그마 p2-냉장고-재료 사용 여부 완료) */
type UseToast = { variant: "success" | "error"; message: string };

const USE_FAILED_MESSAGE = "재료 처리에 실패했습니다. 다시 시도해주세요";

function getUseSuccessMessage(name: string, kind: UseKind): string {
  return kind === "USED" ? `${name}을(를) 사용하셨습니다` : `${name}을(를) 버리셨습니다`;
}

type FridgeContainerProps = {
  /** 재료 목록. 기본은 마크업 확인용 값(목록 API를 붙이기 전) */
  ingredients?: FridgeIngredient[];
  /** 불러오는 중(p2-냉장고-스켈레톤). 미리보기용 */
  isLoading?: boolean;
  /** 처음부터 열어 둘 시트. 미리보기용 */
  initialSheet?: FridgeSheet | null;
  /** 처음부터 사용 시트를 열 재료 id. 미리보기용(p2-냉장고-재료 사용 여부) */
  initialUseId?: number;
  /** 사용·폐기 확인을 실패로 처리한다. 미리보기용(API를 붙이기 전 실패 토스트 확인) */
  simulateUseFailure?: boolean;
};

/**
 * p2 냉장고(메인). 메인 헤더, 재료 개수·요약 카드, 상태·위치·종류로 거른 재료 목록.
 * 행을 누르면 사용·폐기 시트, 오른쪽 >는 재료 수정, +는 재료 추가(domain-rules 2).
 * 요약 카드·위치·종류 칩은 다시 누르면 거르기가 풀린다(토글).
 * 사용·폐기를 확인하면 시트를 닫고 결과 토스트("목살을(를) 사용하셨습니다" / 실패 문구)를 띄운다.
 * 상태: 기본 / 불러오는 중 / 재료 사용 시트 / 사용·폐기 성공·실패 토스트 / 재료 위치 시트 / 재료 종류 시트 / 빈 냉장고 / 거른 결과 없음.
 */
export function FridgeContainer({
  ingredients: initialIngredients = SAMPLE_INGREDIENTS,
  isLoading = false,
  initialSheet = null,
  initialUseId,
  simulateUseFailure = false,
}: FridgeContainerProps) {
  // TODO: 목록 API를 붙이면 서버 값(쿼리)을 쓰고, 사용·폐기 뒤에는 목록·요약 쿼리를 무효화한다
  const [ingredients, setIngredients] = useState(initialIngredients);
  const toast = useTimedToast<UseToast>();
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

  const handleUse = (kind: UseKind, amount: number) => {
    const target = usingIngredient;
    // 다른 화면 요소(토스트)를 띄우기 전에 시트를 먼저 닫는다
    setUsingId(null);
    if (!target) return;
    // TODO: API 18(POST /user/ingredients/{id}/consume { type, amount }). 실패(INSUFFICIENT_QUANTITY 등)면 실패 토스트
    if (simulateUseFailure) {
      toast.show({ variant: "error", message: USE_FAILED_MESSAGE });
      return;
    }
    // 지금은 화면 값에서 빼고, 0이 되면 지운다(서버도 0이면 재료를 지움)
    setIngredients((prev) =>
      prev.flatMap((item) => {
        if (item.id !== target.id) return [item];
        const remaining = Math.round((item.amount - amount) * 1000) / 1000;
        return remaining > 0 ? [{ ...item, amount: remaining }] : [];
      }),
    );
    toast.show({ variant: "success", message: getUseSuccessMessage(target.name, kind) });
  };

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

      <UseIngredientSheet
        ingredient={usingIngredient}
        onClose={() => setUsingId(null)}
        onConfirm={handleUse}
      />
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

      {/* 문구를 바꿔 끼우면 읽히도록 항상 그려 두는 영역(blueprint §6.3) */}
      <div className={styles.toastArea} role="status" aria-live="polite">
        {toast.value && (
          <Toast variant={toast.value.variant} isLeaving={toast.isLeaving} onLeft={toast.remove}>
            {toast.value.message}
          </Toast>
        )}
      </div>
    </>
  );
}
