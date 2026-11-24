"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { ConfirmModal } from "@/components/ConfirmModal/ConfirmModal";
import { InfoBox } from "@/components/InfoBox/InfoBox";
import { InputSheet } from "@/components/InputSheet/InputSheet";
import { SelectSheet } from "@/components/SelectSheet/SelectSheet";
import { SettingRow } from "@/components/SettingRow/SettingRow";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { Stepper } from "@/components/Stepper/Stepper";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { type WheelOption, WheelPicker } from "@/components/WheelPicker/WheelPicker";
import { dayjs } from "@/lib/dayjs";
import { getErrorMessageByCode } from "@/lib/error-messages";

import { MealPlanQuotaNotice } from "../../../../_components/MealPlanQuotaNotice/MealPlanQuotaNotice";
import {
  MEAL_PLAN_FAIL_NOTICE,
  MEAL_PURPOSES,
  MEAL_TYPES,
  type MealPurpose,
  type MealType,
} from "../../../../_lib/meal-plan";

import styles from "./MealPlanSetupContainer.module.css";

/** 식사 인원 범위: 가구원 수와 같은 1~50 */
const SERVINGS_MIN = 1;
const SERVINGS_MAX = 50;

const PURPOSE_OPTIONS = MEAL_PURPOSES.map(({ code, label }) => ({ value: code, label }));
const MEAL_OPTIONS = MEAL_TYPES.map(({ code, label }) => ({ value: code, label }));

type SetupSheet = "startDate" | "purpose" | "meals";

/** 생성 화면 처음 값(API 5의 가구원 수·마지막 식단 목적, API 27의 남은 횟수) */
export type MealPlanSetupDefaults = {
  remainingPlans: number;
  householdSize: number;
  lastPurpose: MealPurpose | null;
};

type MealPlanSetupContainerProps = {
  /** 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  defaults?: MealPlanSetupDefaults;
  /** 처음 시작일("2026-11-18"). 미리보기용, 기본은 오늘 */
  initialStartDate?: string;
  /** 처음부터 열어 둘 시트. 미리보기용 */
  initialSheet?: SetupSheet | null;
  /** 겹침 경고 모달로 시작. 미리보기용(API 38의 409 MEAL_PLAN_OVERLAP) */
  initialOverlapOpen?: boolean;
};

/**
 * p5.1 식단표 생성. 시작일(오늘부터)·식단 목적(하나)·넣을 끼니(여러 개)·식사 인원을 고르고 "식단표 만들기"(domain-rules 5).
 * 만들기를 누르면 API 38(202)을 보내고 바로 냉장고로 가서 "식단을 완성하면 알림을 보내드려요" 토스트. 생성 중 화면은 없다.
 * 상태: 불러오는 중 / 기본 / 시작일·목적·끼니 시트 / 겹침 경고 모달 / 남은 횟수 0(버튼 꺼짐).
 */
export function MealPlanSetupContainer({ defaults, ...rest }: MealPlanSetupContainerProps) {
  return (
    <>
      <SubHeader title="식단표 생성" />
      {defaults ? (
        <MealPlanSetupBody defaults={defaults} {...rest} />
      ) : (
        <main className={styles.main} aria-busy="true">
          <Skeleton width="100%" height="17rem" radius="var(--radius-lg)" />
        </main>
      )}
    </>
  );
}

function MealPlanSetupBody({
  defaults,
  initialStartDate,
  initialSheet = null,
  initialOverlapOpen = false,
}: Omit<MealPlanSetupContainerProps, "defaults"> & { defaults: MealPlanSetupDefaults }) {
  const router = useRouter();
  // 오늘은 값이 온 뒤에 읽는다(미리 그리는 빈 화면에서는 시각을 읽지 않음, cacheComponents)
  const [today] = useState(() => dayjs().format("YYYY-MM-DD"));
  const [startDate, setStartDate] = useState(initialStartDate ?? today);
  const [purpose, setPurpose] = useState<MealPurpose>(defaults.lastPurpose ?? "BALANCED");
  const [meals, setMeals] = useState<MealType[]>(["BREAKFAST", "LUNCH", "DINNER"]);
  const [servings, setServings] = useState(defaults.householdSize);
  const [sheet, setSheet] = useState<SetupSheet | null>(initialSheet);
  const [isOverlapOpen, setIsOverlapOpen] = useState(initialOverlapOpen);

  const closeSheet = () => setSheet(null);
  // TODO: 횟수 소진 화면(정책: 생성 주소 안의 상태, 디자인 대기)이 생기면 여기서 그 화면을 보이고,
  // 식단표 빈 화면·추가 시트의 "식단표 만들기"는 켜 두어 이 화면까지 들어오게 한다(#217 리뷰)
  const canSubmit = defaults.remainingPlans > 0 && meals.length > 0;
  const purposeLabel = MEAL_PURPOSES.find((item) => item.code === purpose)?.label;
  const mealsLabel = MEAL_TYPES.filter((meal) => meals.includes(meal.code))
    .map((meal) => meal.label)
    .join(" · ");

  const submit = (overwrite: boolean) => {
    // TODO: API 38(POST /user/meal-plans/generate { startDate, purpose, mealTypes, servings, overwrite }).
    // 202면 냉장고로 가서 "식단을 완성하면 알림을 보내드려요" 토스트. 409 MEAL_PLAN_OVERLAP이면 겹침 모달,
    // MEAL_PLAN_IN_PROGRESS·횟수 초과는 오류 토스트
    void overwrite;
    setIsOverlapOpen(false);
    router.push("/user/fridge/ingredients");
  };

  return (
    <>
      <main className={styles.main}>
        <div className={styles.card}>
          <SettingRow
            label="시작일"
            value={dayjs(startDate).format("YYYY.MM.DD")}
            onClick={() => setSheet("startDate")}
            className={styles.row}
          />
          <SettingRow
            label="식단 목적"
            value={purposeLabel}
            onClick={() => setSheet("purpose")}
            className={styles.row}
          />
          <SettingRow
            label="넣을 끼니"
            value={mealsLabel}
            onClick={() => setSheet("meals")}
            className={styles.row}
          />
          <SettingRow
            label="식사 인원"
            className={styles.stepperRow}
            control={() => (
              <Stepper
                label="식사 인원"
                value={servings}
                min={SERVINGS_MIN}
                max={SERVINGS_MAX}
                formatValue={(value) => `${value}명`}
                onChange={setServings}
              />
            )}
          />
        </div>
        <InfoBox className={styles.info}>
          냉장고 재료 상태에 따라 식단표 일수가 줄어들 수 있어요
        </InfoBox>
      </main>

      <BottomBar
        topNotice={<MealPlanQuotaNotice remaining={defaults.remainingPlans} />}
        bottomNotice={MEAL_PLAN_FAIL_NOTICE}
      >
        <Button disabled={!canSubmit} onClick={() => submit(false)}>
          식단표 만들기
        </Button>
      </BottomBar>

      {sheet === "startDate" && (
        <StartDateSheet
          today={today}
          defaultValue={startDate}
          onClose={closeSheet}
          onConfirm={(date) => {
            setStartDate(date);
            closeSheet();
          }}
        />
      )}
      <SelectSheet
        isOpen={sheet === "purpose"}
        onClose={closeSheet}
        title="식단 목적"
        options={PURPOSE_OPTIONS}
        defaultValues={[purpose]}
        onConfirm={([value]) => {
          if (value) setPurpose(value);
          closeSheet();
        }}
      />
      <SelectSheet
        isOpen={sheet === "meals"}
        onClose={closeSheet}
        title="넣을 끼니"
        options={MEAL_OPTIONS}
        defaultValues={meals}
        multiple
        onConfirm={(values) => {
          setMeals(values);
          closeSheet();
        }}
      />

      {/* 겹침 경고 모달은 피그마에 아직 없다(domain-rules 5 "modal pending"). 문구는 오류 문구표 */}
      <ConfirmModal
        isOpen={isOverlapOpen}
        onClose={() => setIsOverlapOpen(false)}
        onConfirm={() => submit(true)}
        title="식단표 덮어쓰기"
        message={getErrorMessageByCode("MEAL_PLAN_OVERLAP")}
        confirmLabel="덮어쓰기"
      />
    </>
  );
}

function range(from: number, to: number, label: (n: number) => string): WheelOption[] {
  return Array.from({ length: to - from + 1 }, (_, i) => ({
    value: String(from + i),
    label: label(from + i),
  }));
}

/**
 * 피그마 식단표-식단 레시피 설정-시작일: "시작일" 입력 시트, 연(2자리)·월·일 휠 3열, "수정".
 * 오늘 이전 날짜는 휠에 아예 보이지 않는다(시작일은 오늘부터, domain-rules 5):
 * 올해면 이번 달부터, 이번 달이면 오늘부터. 범위를 벗어난 값은 WheelPicker가 가장 가까운 값으로 맞춘다.
 */
function StartDateSheet({
  today,
  defaultValue,
  onClose,
  onConfirm,
}: {
  today: string;
  defaultValue: string;
  onClose: () => void;
  onConfirm: (date: string) => void;
}) {
  const start = dayjs(defaultValue);
  const [year, setYear] = useState(String(start.year()));
  const [month, setMonth] = useState(String(start.month() + 1));
  const [day, setDay] = useState(String(start.date()));
  const todayValue = dayjs(today);
  const thisYear = todayValue.year();
  const isThisYear = Number(year) === thisYear;
  const isThisMonth = isThisYear && Number(month) === todayValue.month() + 1;
  const firstMonth = isThisYear ? todayValue.month() + 1 : 1;
  const firstDay = isThisMonth ? todayValue.date() : 1;
  const daysInMonth = dayjs(`${year}-${month.padStart(2, "0")}-01`).daysInMonth();

  const handleConfirm = () => {
    const picked = dayjs(`${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`).format(
      "YYYY-MM-DD",
    );
    // 휠이 오늘 이전을 보여 주지 않지만, 값이 맞춰지기 전에 누른 경우를 위해 한 번 더 막는다
    onConfirm(picked < today ? today : picked);
  };

  return (
    <InputSheet
      isOpen
      onClose={onClose}
      title="시작일"
      footer={
        <Button size="md" onClick={handleConfirm}>
          수정
        </Button>
      }
    >
      <WheelPicker
        columns={[
          {
            label: "연도",
            options: range(thisYear, thisYear + 1, (n) => String(n).slice(-2)),
            value: year,
            onChange: setYear,
          },
          { label: "월", options: range(firstMonth, 12, String), value: month, onChange: setMonth },
          {
            label: "일",
            options: range(firstDay, daysInMonth, String),
            value: day,
            onChange: setDay,
          },
        ]}
      />
    </InputSheet>
  );
}
