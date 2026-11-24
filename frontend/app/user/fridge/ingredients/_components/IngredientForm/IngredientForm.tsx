"use client";

import { useState } from "react";

import { InfoBox } from "@/components/InfoBox/InfoBox";
import { SelectSheet } from "@/components/SelectSheet/SelectSheet";
import { SettingRow } from "@/components/SettingRow/SettingRow";
import { Stepper } from "@/components/Stepper/Stepper";
import {
  CATEGORIES,
  formatAmount,
  FRIDGE_UNITS,
  getCategory,
  getStorage,
  STORAGES,
} from "@/lib/ingredients";

import {
  AMOUNT_MAX,
  firstAmount,
  formatExpiry,
  formatPrice,
  type IngredientDraft,
  pickSampleUnit,
} from "../../_lib/ingredient-form";
import { ExpiryDateSheet } from "../ExpiryDateSheet/ExpiryDateSheet";
import { IngredientTextSheet } from "../IngredientTextSheet/IngredientTextSheet";

import styles from "./IngredientForm.module.css";

export type IngredientSheet = "name" | "category" | "storage" | "expiry" | "price";

const CATEGORY_OPTIONS = CATEGORIES.map(({ code, name }) => ({ value: code, label: name }));
const STORAGE_OPTIONS = STORAGES.map(({ code, label }) => ({ value: code, label }));

type IngredientFormProps = {
  draft: IngredientDraft;
  onChange: (draft: IngredientDraft) => void;
  /** 처음부터 열어 둘 시트. 미리보기용 */
  initialSheet?: IngredientSheet | null;
};

/**
 * 재료 추가(p2.2)·수정(p2.1)이 같이 쓰는 입력 카드와 안내 박스.
 * 행을 누르면 값 시트가 열린다: 재료명·가격=글 입력, 카테고리·보관 위치=선택, 소비 기한=휠.
 * 수량은 재료명을 쓰기 전에는 꺼져 있다(단위를 서버가 재료명으로 고름, domain-rules 2).
 */
export function IngredientForm({ draft, onChange, initialSheet = null }: IngredientFormProps) {
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

      <InfoBox className={styles.info}>
        정보가 구체적일수록 AI가 남은 용량에 딱 맞는 계량법과 재료 상태에 가장 어울리는 맞춤형
        조리법을 만들어 드립니다
      </InfoBox>

      {(sheet === "name" || sheet === "price") && (
        <IngredientTextSheet
          kind={sheet}
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
