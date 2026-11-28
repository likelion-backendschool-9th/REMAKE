"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { InfoBox } from "@/components/InfoBox/InfoBox";
import { ResultState } from "@/components/ResultState/ResultState";
import { SelectSheet } from "@/components/SelectSheet/SelectSheet";
import { SettingRow } from "@/components/SettingRow/SettingRow";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { Stepper } from "@/components/Stepper/Stepper";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { Toast } from "@/components/Toast/Toast";
import { useTimedToast } from "@/hooks/useTimedToast";
import { type AllergenCode, ALLERGENS } from "@/lib/allergens";
import { getErrorMessage } from "@/lib/error-messages";
import { SCREENS } from "@/lib/events/screens";
import { trackRecipeGenerateStarted } from "@/lib/events/tracker";
import { formatDDay } from "@/lib/format";
import { useIngredientsQuery } from "@/lib/queries/ingredients";
import { useProfileQuery } from "@/lib/queries/profile";
import { type GenerateRecipeVariables, useGenerateRecipeMutation } from "@/lib/queries/recipes";

import { RecipeGenerationStatus } from "../../../_components/RecipeGenerationStatus/RecipeGenerationStatus";
import { recipeResultPath } from "../../../_lib/recipe";
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
  /** true면 프로필(API 5)·냉장고 재료(API 12)를 불러오고 생성(API 28)을 실제로 보낸다. 미리보기(app/dev)는 false */
  live?: boolean;
};

/** 마지막으로 보낸 생성 조건. 다시 시도할 때 그대로 다시 보낸다 */
type GenerationRequest = {
  body: GenerateRecipeVariables["body"];
  servings: number;
  optionCount: number;
};

/**
 * p4.1.2 커스텀 레시피. 식사 인원(필수, 기본 가구원 수) + 재료(내 냉장고)·맛·시간·도구·방식·유형·알레르기 재료를
 * 고르고 "레시피 생성"(API 28, 하루 횟수 차감). 고르기만 하고 직접 쓰는 칸은 없다(domain-rules 4).
 * 상태: 기본 / 재료 선택 드로워(디세이블·액티브) / 옵션 시트 5개 / 알레르기 재료 드로워.
 */
export function CustomRecipeContainer({
  defaults,
  live = false,
  ...rest
}: CustomRecipeContainerProps) {
  // defaults를 직접 넘기면(미리보기) 요청하지 않는다
  const shouldFetch = live && !defaults;
  const profileQuery = useProfileQuery({ enabled: shouldFetch });
  const ingredientsQuery = useIngredientsQuery({ enabled: shouldFetch });

  const profile = profileQuery.data;
  const ingredients = ingredientsQuery.data;
  const resolvedDefaults: CustomRecipeDefaults | undefined =
    defaults ??
    (shouldFetch && profile && ingredients
      ? {
          householdSize: profile.householdSize ?? 1,
          ingredients: ingredients.items.map((item) => ({
            id: item.id,
            name: item.name,
            categoryCode: item.categoryCode,
            storage: item.storage,
            amount: item.amount,
            unit: item.unit,
            expiryStatus: item.status,
            dDay: formatDDay(item.dDay),
          })),
        }
      : undefined);

  if (resolvedDefaults) {
    return (
      <CustomRecipeForm
        defaults={resolvedDefaults}
        live={live}
        supplementEnabled={profile?.recipeSupplementEnabled ?? false}
        {...rest}
      />
    );
  }

  if (shouldFetch && (profileQuery.isError || ingredientsQuery.isError)) {
    return (
      <>
        <SubHeader title="커스텀 레시피" />
        <main className={styles.main}>
          <ResultState
            variant="error"
            title="정보를 불러오지 못했어요"
            description={getErrorMessage(profileQuery.error ?? ingredientsQuery.error)}
            action={
              <Button
                onClick={() => {
                  if (profileQuery.isError) void profileQuery.refetch();
                  if (ingredientsQuery.isError) void ingredientsQuery.refetch();
                }}
              >
                다시 시도
              </Button>
            }
          />
        </main>
      </>
    );
  }

  return (
    <>
      <SubHeader title="커스텀 레시피" />
      <main className={styles.main} aria-busy="true">
        <Skeleton width="100%" height="34rem" radius="0.75rem" />
      </main>
    </>
  );
}

function CustomRecipeForm({
  defaults,
  live = false,
  supplementEnabled = false,
  initialPanel = null,
  initialIngredientIds = [],
  initialAllergens = [],
}: Omit<CustomRecipeContainerProps, "defaults"> & {
  defaults: CustomRecipeDefaults;
  /** E39 supplement_enabled(프로필의 재료 보충 설정) */
  supplementEnabled?: boolean;
}) {
  const router = useRouter();
  const generateMutation = useGenerateRecipeMutation();
  const toast = useTimedToast<string>();
  const [generationError, setGenerationError] = useState<unknown>(null);
  /** 생성 중에 뒤로 가기로 폼으로 돌아왔는지. 이때는 응답이 와도 결과 화면으로 보내지 않는다 */
  const [hasLeft, setHasLeft] = useState(false);
  const mountedRef = useRef(false);
  /** 생성 요청 번호. 뒤로 가기·재시도로 번호가 바뀐 옛 요청의 응답은 무시한다 */
  const attemptRef = useRef(0);
  const [lastRequest, setLastRequest] = useState<GenerationRequest | null>(null);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

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

  /** 생성 요청을 보낸다(E39 기록 후 API 28). 처음 제출과 다시 시도가 함께 쓴다 */
  const startGeneration = (request: GenerationRequest) => {
    if (generateMutation.isPending) return;
    attemptRef.current += 1;
    const attempt = attemptRef.current;
    setHasLeft(false);
    setGenerationError(null);

    trackRecipeGenerateStarted(SCREENS.recipeCustom, {
      recipe_mode: "custom",
      servings: request.servings,
      option_count: request.optionCount,
      supplement_enabled: supplementEnabled,
    });
    generateMutation.mutate(
      { body: request.body, screen: SCREENS.recipeCustom },
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

  const handleSubmit = () => {
    if (!live) return;
    if (generateMutation.isPending) return;

    // 안 고른 옵션(null)과 빈 배열은 보내지 않는다. id·코드·인원수만 보낸다(자유 입력 없음)
    const body: GenerateRecipeVariables["body"] = { mode: "CUSTOM", servings };
    if (ingredientIds.length > 0) body.ingredientIds = ingredientIds;
    if (options.taste) body.taste = options.taste;
    if (options.cookTime) body.cookTime = options.cookTime;
    if (options.tool) body.tool = options.tool;
    if (options.method) body.method = options.method;
    if (options.cuisine) body.cuisine = options.cuisine;
    if (allergens.length > 0) body.allergenCodes = allergens;

    const request: GenerationRequest = {
      body,
      servings,
      optionCount: OPTION_SHEETS.filter(({ key }) => options[key] !== null).length,
    };
    setLastRequest(request);
    startGeneration(request);
  };

  /** 생성 화면에서 폼으로 돌아온다. 입력값은 그대로 두고, 아직 오는 중인 응답은 무시한다 */
  const leaveGeneration = () => {
    attemptRef.current += 1;
    setHasLeft(true);
    setGenerationError(null);
  };

  const isGenerating = live && generateMutation.isPending && !hasLeft;
  const hasGenerationError = live && generationError !== null;
  const showStatus = isGenerating || hasGenerationError;

  return (
    <>
      {showStatus && (
        <RecipeGenerationStatus
          status={hasGenerationError ? "error" : "loading"}
          error={generationError}
          onRetry={lastRequest ? () => startGeneration(lastRequest) : undefined}
          onBack={leaveGeneration}
        />
      )}

      {/* 생성 중·실패 화면에서도 입력값이 남도록 폼은 지우지 않고 숨긴다 */}
      <div hidden={showStatus} className={styles.form}>
        <SubHeader title="커스텀 레시피" />
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
          <Button onClick={handleSubmit} disabled={generateMutation.isPending}>
            레시피 생성
          </Button>
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
      </div>

      {/* 문구를 바꿔 끼우면 읽히도록 항상 그려 두는 영역(blueprint §6.3) */}
      <div className={styles.toastArea} aria-live="polite">
        {toast.value && (
          <Toast variant="error" isLeaving={toast.isLeaving} onLeft={toast.remove}>
            {toast.value}
          </Toast>
        )}
      </div>
    </>
  );
}
