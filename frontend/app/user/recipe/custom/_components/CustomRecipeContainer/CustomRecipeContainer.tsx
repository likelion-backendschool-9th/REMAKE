"use client";

import { useState } from "react";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { InfoBox } from "@/components/InfoBox/InfoBox";
import { SelectSheet } from "@/components/SelectSheet/SelectSheet";
import { SettingRow } from "@/components/SettingRow/SettingRow";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { Stepper } from "@/components/Stepper/Stepper";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { type AllergenCode, ALLERGENS } from "@/lib/allergens";

import {
  COOK_TIMES,
  CUISINES,
  type CustomOptions,
  type CustomRecipeDefaults,
  METHODS,
  SERVINGS_MAX,
  SERVINGS_MIN,
  TASTES,
  TOOLS,
} from "../../_lib/custom-recipe";
import { AllergenDrawer } from "../AllergenDrawer/AllergenDrawer";
import { IngredientPickerDrawer } from "../IngredientPickerDrawer/IngredientPickerDrawer";

import styles from "./CustomRecipeContainer.module.css";

/** 한 가지만 고르는 옵션 시트(맛·시간·도구·방식·유형). 순서·제목은 피그마 p4.1.2 */
const OPTION_SHEETS = [
  { key: "taste", title: "원하는 맛", options: TASTES },
  { key: "cookTime", title: "조리 시간", options: COOK_TIMES },
  { key: "tool", title: "조리 도구", options: TOOLS },
  { key: "method", title: "조리 방식", options: METHODS },
  { key: "cuisine", title: "요리 유형", options: CUISINES },
] as const;

type OptionKey = (typeof OPTION_SHEETS)[number]["key"];
type Panel = OptionKey | "ingredients" | "allergens";

const EMPTY_OPTIONS: CustomOptions = {
  taste: null,
  cookTime: null,
  tool: null,
  method: null,
  cuisine: null,
};

type CustomRecipeContainerProps = {
  /** 가구원 수·냉장고 재료. 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  defaults?: CustomRecipeDefaults;
  /** 처음부터 열어 둘 시트·드로워. 미리보기용 */
  initialPanel?: Panel | null;
  /** 미리 골라 둘 재료. 미리보기용(피그마 드로워 액티브) */
  initialIngredientIds?: number[];
  /** 미리 골라 둘 알레르기 재료. 미리보기용 */
  initialAllergens?: AllergenCode[];
};

/**
 * p4.1.2 커스텀 레시피. 식사 인원(필수, 기본 가구원 수) + 재료(내 냉장고)·맛·시간·도구·방식·유형·알레르기 재료를
 * 고르고 "레시피 생성"(API 28, 하루 횟수 차감). 고르기만 하고 직접 쓰는 칸은 없다(domain-rules 4).
 * 상태: 기본 / 재료 선택 드로워(디세이블·액티브) / 옵션 시트 5개 / 알레르기 재료 드로워.
 */
export function CustomRecipeContainer({ defaults, ...rest }: CustomRecipeContainerProps) {
  return (
    <>
      <SubHeader title="커스텀 레시피" />
      {defaults ? (
        <CustomRecipeForm defaults={defaults} {...rest} />
      ) : (
        <main className={styles.main} aria-busy="true">
          <Skeleton width="100%" height="34rem" radius="0.75rem" />
        </main>
      )}
    </>
  );
}

function CustomRecipeForm({
  defaults,
  initialPanel = null,
  initialIngredientIds = [],
  initialAllergens = [],
}: Omit<CustomRecipeContainerProps, "defaults"> & { defaults: CustomRecipeDefaults }) {
  const [servings, setServings] = useState(defaults.householdSize);
  const [ingredientIds, setIngredientIds] = useState<number[]>(initialIngredientIds);
  const [options, setOptions] = useState<CustomOptions>(EMPTY_OPTIONS);
  const [allergens, setAllergens] = useState<AllergenCode[]>(initialAllergens);
  const [panel, setPanel] = useState<Panel | null>(initialPanel);

  const closePanel = () => setPanel(null);
  const ingredientsLabel = defaults.ingredients
    .filter((item) => ingredientIds.includes(item.id))
    .map((item) => item.name)
    .join(" · ");
  const allergensLabel = ALLERGENS.filter(({ code }) => allergens.includes(code))
    .map(({ name }) => name)
    .join(" · ");

  const handleSubmit = () => {
    // TODO: API 28({ mode: "CUSTOM", servings, ingredientIds, ...options, allergenCodes })을 보내고
    // 생성 결과(p4.1.3)로 이동. 기다리는 동안·실패·횟수 소진은 그 화면 상태. 안 고른 옵션(null)은 보내지 않는다
  };

  return (
    <>
      <main className={styles.main}>
        <div className={styles.card}>
          <SettingRow
            label="식사 인원"
            isRequired
            className={styles.stepperRow}
            control={() => (
              <Stepper
                label="식사 인원"
                value={servings}
                min={SERVINGS_MIN}
                max={SERVINGS_MAX}
                formatValue={(value) => `${value}인`}
                onChange={setServings}
              />
            )}
          />
          <SettingRow
            label="재료 선택"
            value={ingredientsLabel}
            onClick={() => setPanel("ingredients")}
            className={styles.row}
          />
          {OPTION_SHEETS.map(({ key, title, options: choices }) => (
            <SettingRow
              key={key}
              label={title}
              value={choices.find((choice) => choice.value === options[key])?.label}
              onClick={() => setPanel(key)}
              className={styles.row}
            />
          ))}
          <SettingRow
            label="알레르기 재료"
            value={allergensLabel}
            onClick={() => setPanel("allergens")}
            className={styles.row}
          />
        </div>
        <InfoBox className={styles.info}>
          정보가 구체적일수록 AI가 남은 용량에 딱 맞는 계량법과 재료 상태에 가장 어울리는 맞춤형
          조리법을 만들어 드립니다
        </InfoBox>
      </main>

      <BottomBar>
        <Button onClick={handleSubmit}>레시피 생성</Button>
      </BottomBar>

      {/* 피그마 원하는 맛·조리 시간·조리 도구·조리 방식·요리 유형: 하나 고르기, 고르기 전에는 확인이 꺼짐 */}
      {OPTION_SHEETS.map(({ key, title, options: choices }) => (
        <SelectSheet<string>
          key={key}
          isOpen={panel === key}
          onClose={closePanel}
          title={title}
          options={choices}
          defaultValues={options[key] ? [options[key]] : []}
          onConfirm={([value]) => {
            setOptions((prev) => ({ ...prev, [key]: value ?? null }));
            closePanel();
          }}
        />
      ))}

      <IngredientPickerDrawer
        isOpen={panel === "ingredients"}
        onClose={closePanel}
        ingredients={defaults.ingredients}
        defaultIds={ingredientIds}
        onConfirm={(ids) => {
          setIngredientIds(ids);
          closePanel();
        }}
      />
      <AllergenDrawer
        isOpen={panel === "allergens"}
        onClose={closePanel}
        defaultCodes={allergens}
        onConfirm={(codes) => {
          setAllergens(codes);
          closePanel();
        }}
      />
    </>
  );
}
