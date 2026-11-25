"use client";

import { useState } from "react";

import { AltArrowRightIcon } from "@solar-icons/react/linear";

import { ExpiryDateSheet } from "@/components/ExpiryDateSheet/ExpiryDateSheet";
import { InfoBox } from "@/components/InfoBox/InfoBox";
import {
  IngredientTextSheet,
  type TextSheetCopyByKind,
} from "@/components/IngredientTextSheet/IngredientTextSheet";
import { SelectSheet } from "@/components/SelectSheet/SelectSheet";
import { SettingRow } from "@/components/SettingRow/SettingRow";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { Stepper } from "@/components/Stepper/Stepper";
import {
  AMOUNT_MAX,
  firstAmount,
  formatExpiry,
  formatPrice,
  type IngredientDraft,
  pickSampleUnit,
} from "@/lib/ingredient-form";
import {
  CATEGORIES,
  formatAmount,
  FRIDGE_UNITS,
  getCategory,
  getStorage,
  STORAGES,
} from "@/lib/ingredients";

import styles from "./IngredientForm.module.css";

export type IngredientSheet = "name" | "category" | "storage" | "expiry" | "price";

const CATEGORY_OPTIONS = CATEGORIES.map(({ code, name }) => ({ value: code, label: name }));
const STORAGE_OPTIONS = STORAGES.map(({ code, label }) => ({ value: code, label }));

type IngredientFormProps = {
  draft: IngredientDraft;
  onChange: (draft: IngredientDraft) => void;
  /** 재료명·가격 입력 시트 문구를 화면별로 바꾼다. 없으면 냉장고(p2.1·p2.2) 문구 */
  textSheetCopy?: TextSheetCopyByKind;
  /** 처음부터 열어 둘 시트. 미리보기용 */
  initialSheet?: IngredientSheet | null;
};

/**
 * 재료 입력 카드와 안내 박스. 냉장고 재료 추가(p2.2)·수정(p2.1), 영수증 재료 추가(p3.3.2)·수정(p3.3.1)이 같이 쓴다.
 * 행을 누르면 값 시트가 열린다: 재료명·가격=글 입력, 카테고리·보관 위치=선택, 소비 기한=휠.
 * 수량은 재료명을 쓰기 전에는 꺼져 있다(단위를 서버가 재료명으로 고름, domain-rules 2).
 */
export function IngredientForm({
  draft,
  onChange,
  textSheetCopy,
  initialSheet = null,
}: IngredientFormProps) {
  const [sheet, setSheet] = useState<IngredientSheet | null>(initialSheet);
  const closeSheet = () => setSheet(null);

  const update = (patch: Partial<IngredientDraft>) => {
    onChange({ ...draft, ...patch });
    closeSheet();
  };

  const handleName = (name: string) => {
    // TODO: API 14로 단위를 다시 받는다(카테고리가 있으면 함께). 이름이 바뀌어도 숫자는 그대로 둔다
    const unit = pickSampleUnit(name);
    update({ name, unit, amount: draft.unit ? draft.amount : firstAmount(unit) });
  };

  const unit = draft.unit;

  return (
    <>
      <div className={styles.card}>
        <SettingRow
          label="재료명"
          value={draft.name}
          placeholder="작성하지 않음"
          onClick={() => setSheet("name")}
          className={styles.row}
        />
        <SettingRow
          label="카테고리"
          value={draft.categoryCode ? getCategory(draft.categoryCode).name : undefined}
          onClick={() => setSheet("category")}
          className={styles.row}
        />
        <SettingRow
          label="보관 위치"
          value={draft.storage ? getStorage(draft.storage).label : undefined}
          onClick={() => setSheet("storage")}
          className={styles.row}
        />
        <SettingRow
          label="소비 기한"
          value={draft.expirationDate ? formatExpiry(draft.expirationDate) : undefined}
          onClick={() => setSheet("expiry")}
          className={styles.row}
        />
        <SettingRow
          label="재료 가격"
          value={draft.price === null ? undefined : formatPrice(draft.price)}
          placeholder="작성하지 않음"
          onClick={() => setSheet("price")}
          className={styles.row}
        />
        <SettingRow
          label="재료 수량"
          className={styles.amountRow}
          control={() => (
            <Stepper
              label="재료 수량"
              value={draft.amount}
              step={unit ? FRIDGE_UNITS[unit].step : 1}
              min={unit ? FRIDGE_UNITS[unit].step : 0}
              max={AMOUNT_MAX}
              disabled={!unit}
              formatValue={(value) => (unit ? formatAmount(value, unit) : "-")}
              onChange={(amount) => onChange({ ...draft, amount })}
            />
          )}
        />
      </div>

      <IngredientInfo />

      {(sheet === "name" || sheet === "price") && (
        <IngredientTextSheet
          kind={sheet}
          copy={textSheetCopy?.[sheet]}
          defaultValue={
            sheet === "name" ? draft.name : draft.price === null ? "" : String(draft.price)
          }
          onClose={closeSheet}
          onConfirm={(value) =>
            sheet === "name"
              ? handleName(value)
              : update({ price: value === "" ? null : Number(value) })
          }
        />
      )}
      {sheet === "expiry" && (
        <ExpiryDateSheet
          defaultValue={draft.expirationDate}
          onClose={closeSheet}
          onConfirm={(expirationDate) => update({ expirationDate })}
        />
      )}
      <SelectSheet
        isOpen={sheet === "category"}
        onClose={closeSheet}
        title="카테고리"
        options={CATEGORY_OPTIONS}
        defaultValues={draft.categoryCode ? [draft.categoryCode] : []}
        // TODO: 카테고리가 바뀌면 API 14로 단위를 다시 받는다
        onConfirm={([categoryCode]) => update({ categoryCode })}
      />
      <SelectSheet
        isOpen={sheet === "storage"}
        onClose={closeSheet}
        title="보관 위치"
        options={STORAGE_OPTIONS}
        defaultValues={draft.storage ? [draft.storage] : []}
        onConfirm={([storage]) => update({ storage })}
      />
    </>
  );
}

function IngredientInfo() {
  return (
    <InfoBox className={styles.info}>
      정보가 구체적일수록 AI가 남은 용량에 딱 맞는 계량법과 재료 상태에 가장 어울리는 맞춤형
      조리법을 만들어 드립니다
    </InfoBox>
  );
}

const SKELETON_LABELS = ["재료명", "카테고리", "보관 위치", "소비 기한", "재료 가격"] as const;

/**
 * 불러오는 중 입력 카드(피그마 p3.3.1·p3.3.2 스켈레톤): 이름은 그대로, 값 자리만 회색, 수량 스테퍼는 꺼짐.
 * 누를 수 있는 것이 없도록 행을 버튼으로 만들지 않는다.
 */
export function IngredientFormSkeleton() {
  return (
    <>
      <div className={styles.card} aria-busy="true">
        {SKELETON_LABELS.map((label) => (
          <SettingRow
            key={label}
            label={label}
            className={styles.row}
            control={() => (
              <span className={styles.skeletonValue} aria-hidden="true">
                <Skeleton width="4.5rem" height="1rem" />
                <AltArrowRightIcon className={styles.skeletonArrow} />
              </span>
            )}
          />
        ))}
        <SettingRow
          label="재료 수량"
          className={styles.amountRow}
          control={() => (
            <Stepper
              label="재료 수량"
              value={0}
              disabled
              formatValue={() => <Skeleton width="1.25rem" height="1rem" />}
              onChange={() => {}}
            />
          )}
        />
      </div>
      <IngredientInfo />
    </>
  );
}
