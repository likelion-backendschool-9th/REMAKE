"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { AddIcon, AltArrowDownIcon } from "@solar-icons/react/linear";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { BottomSheet } from "@/components/BottomSheet/BottomSheet";
import { Button } from "@/components/Button/Button";
import { DraggableSheet } from "@/components/DraggableSheet/DraggableSheet";
import { EmptyState } from "@/components/EmptyState/EmptyState";
import { IconButton } from "@/components/IconButton/IconButton";
import { InputSheet } from "@/components/InputSheet/InputSheet";
import { MainHeader } from "@/components/MainHeader/MainHeader";
import { SelectSheet } from "@/components/SelectSheet/SelectSheet";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { Toast } from "@/components/Toast/Toast";
import { type WheelOption, WheelPicker } from "@/components/WheelPicker/WheelPicker";
import { useTimedToast } from "@/hooks/useTimedToast";
import { dayjs } from "@/lib/dayjs";
import { getErrorMessageByCode } from "@/lib/error-messages";
import { MAIN_TABS, NOTIFICATIONS_HREF, SETTINGS_HREF } from "@/lib/navigation";

import { MealPlanQuotaNotice } from "../../../_components/MealPlanQuotaNotice/MealPlanQuotaNotice";
import { MEAL_PLAN_FAIL_NOTICE, MEAL_PLAN_SETUP_PATH } from "../../../_lib/meal-plan";
import {
  formatDayTitle,
  formatMonthTitle,
  type MealPlanCalendar,
  type MealSlot,
} from "../../_lib/calendar";
import { MealSlotRow } from "../MealSlotRow/MealSlotRow";
import { MonthCalendar } from "../MonthCalendar/MonthCalendar";

import styles from "./MealPlanContainer.module.css";

const CALENDAR_PATH = "/user/meal-plan/calendar";

type SlotAction = "DELETE" | "REPLACE";
const SLOT_ACTIONS = [
  { value: "DELETE", label: "식단 삭제" },
  { value: "REPLACE", label: "레시피 교체" },
] as const satisfies readonly { value: SlotAction; label: string }[];

type MealPlanSheet = "month" | "add";

type MealPlanContainerProps = {
  /** 달력 값(API 39). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  calendar?: MealPlanCalendar;
  /** 날짜별 식단(API 40). 키는 "2026-11-17" */
  slotsByDate?: Record<string, MealSlot[]>;
  /** 이번 달에 남은 식단표 생성 횟수(API 27) */
  remainingPlans?: number;
  /** 가장 급한 알림 한 줄(API 43). 없으면 알림 띠를 그리지 않는다 */
  notice?: { message: string; unreadCount: number };
  /** 오늘("2026-11-17"). 미리보기용, 기본은 화면을 연 날 */
  today?: string;
  /** 처음 고른 날짜. 미리보기용, 기본은 오늘 */
  initialDate?: string;
  /** 처음부터 열어 둘 시트. 미리보기용 */
  initialSheet?: MealPlanSheet | null;
  /** 처음부터 ⋮ 시트를 열 식단 id. 미리보기용(p5-식단표-케밥 클릭) */
  initialSlotMenuId?: number;
  /** 하루 식단 시트를 펼친 채로 시작. 미리보기용(p5-식단표-바텀 시트 높이 조절) */
  initialExpanded?: boolean;
};

/**
 * p5 식단표. 달력(식단 기간 띠) + 고른 날의 식단 드래그 시트. 식단표가 하나도 없으면 빈 화면(식단표 만들기).
 * "2026년 11월 ∨"는 월 선택 휠, 시트의 +는 식단표 추가 시트, ⋮는 식단 삭제·레시피 교체(domain-rules 5).
 * 상태: 불러오는 중 / 식단표 / 시트 펼침 / ⋮ 시트 / 월 선택 / 식단표 추가 / 식단 없는 날 / 빈 화면.
 */
export function MealPlanContainer({ calendar, notice, ...rest }: MealPlanContainerProps) {
  return (
    <>
      <MainHeader
        tabs={MAIN_TABS}
        activeHref={CALENDAR_PATH}
        settingsHref={SETTINGS_HREF}
        notice={notice && { ...notice, href: NOTIFICATIONS_HREF }}
      />
      {calendar ? (
        <MealPlanBody calendar={calendar} {...rest} />
      ) : (
        <main className={styles.main} aria-busy="true">
          <div className={styles.monthRow}>
            <Skeleton width="6.625rem" height="1.375rem" />
          </div>
          <Skeleton width="100%" height="18.9375rem" radius="0" />
        </main>
      )}
    </>
  );
}

/** 값이 온 뒤의 화면. 오늘 날짜는 이때 읽는다(미리 그리는 빈 화면에서는 시각을 읽지 않음, cacheComponents) */
function MealPlanBody({
  calendar,
  slotsByDate = {},
  remainingPlans = 0,
  today: todayProp,
  initialDate,
  initialSheet = null,
  initialSlotMenuId,
  initialExpanded = false,
}: Omit<MealPlanContainerProps, "notice"> & { calendar: MealPlanCalendar }) {
  const router = useRouter();
  const [today] = useState(() => todayProp ?? dayjs().format("YYYY-MM-DD"));
  const [month, setMonth] = useState(calendar.month);
  const [selectedDate, setSelectedDate] = useState(initialDate ?? today);
  const [sheet, setSheet] = useState<MealPlanSheet | null>(initialSheet);
  const [slots, setSlots] = useState(slotsByDate);
  const [menuSlotId, setMenuSlotId] = useState<number | null>(initialSlotMenuId ?? null);
  const toast = useTimedToast<string>();

  const daySlots = slots[selectedDate] ?? [];
  const menuSlot = daySlots.find((slot) => slot.slotId === menuSlotId) ?? null;
  const isPast = selectedDate < today;
  // 이번 달 식단표가 하나도 없으면 빈 화면(피그마 p5-빈 리스트)
  const isEmpty = calendar.periods.length === 0 && month === today.slice(0, 7);

  const goSetup = () => {
    // 다른 화면으로 가기 전에 시트를 먼저 닫는다(blueprint §7.1)
    setSheet(null);
    router.push(MEAL_PLAN_SETUP_PATH);
  };

  const handleMore = (slotId: number) => {
    // 지난 날은 보기만 한다. ⋮는 남기고 누르면 토스트(domain-rules 5)
    if (isPast) toast.show(getErrorMessageByCode("PAST_DATE_NOT_ALLOWED"));
    else setMenuSlotId(slotId);
  };

  const handleSlotAction = (action: SlotAction) => {
    const slotId = menuSlotId;
    setMenuSlotId(null);
    if (action === "DELETE") {
      // TODO: API 41(DELETE /user/meal-plan-slots/{slotId}) 뒤 달력·하루 식단 쿼리 무효화
      setSlots((prev) => ({
        ...prev,
        [selectedDate]: (prev[selectedDate] ?? []).filter((slot) => slot.slotId !== slotId),
      }));
    }
    // TODO: REPLACE는 API 42(레시피 교체, 하루 레시피 횟수 1회 차감). 실패·횟수 초과는 오류 토스트
  };

  const quotaNotice = <MealPlanQuotaNotice remaining={remainingPlans} />;

  if (isEmpty) {
    return (
      <>
        <main className={styles.emptyMain}>
          <div className={styles.empty}>
            <p className={styles.emptyLabel}>이번 주 식단표가 아직 없어요</p>
            <p className={styles.emptyTitle}>
              냉장고 재료를 활용해서
              <br />
              식단을 만들어 드려요
            </p>
          </div>
        </main>
        <BottomBar topNotice={quotaNotice} bottomNotice={MEAL_PLAN_FAIL_NOTICE}>
          <Button disabled={remainingPlans === 0} onClick={goSetup}>
            식단표 만들기
          </Button>
        </BottomBar>
      </>
    );
  }

  return (
    <>
      <main className={styles.main}>
        <div className={styles.monthRow}>
          <button
            type="button"
            className={styles.monthButton}
            aria-haspopup="dialog"
            onClick={() => setSheet("month")}
          >
            {formatMonthTitle(month)}
            <AltArrowDownIcon className={styles.monthArrow} aria-hidden="true" />
          </button>
        </div>
        <MonthCalendar
          month={month}
          // TODO: 다른 달로 바꾸면 API 39(?month=)로 그 달 기간을 받는다
          periods={month === calendar.month ? calendar.periods : []}
          selectedDate={selectedDate}
          onSelectDate={setSelectedDate}
        />
      </main>

      <DraggableSheet
        collapsedHeight="15.6875rem"
        expandedHeight="min(38.8125rem, calc(100dvh - 10rem))"
        label={formatDayTitle(selectedDate)}
        defaultExpanded={initialExpanded}
      >
        <div className={styles.day}>
          <div className={styles.dayTitleRow}>
            <h2 className={styles.dayTitle}>{formatDayTitle(selectedDate)}</h2>
            <IconButton
              icon={<AddIcon strokeWidth={1.8} />}
              iconSize={24}
              label="식단표 추가"
              onClick={() => setSheet("add")}
              className={styles.add}
            />
          </div>
          {daySlots.length === 0 ? (
            // 피그마 p5-식단표-바텀 시트-빈 리스트
            <EmptyState>이 날은 식단이 없어요</EmptyState>
          ) : (
            // TODO: 날짜를 바꾸면 API 40(/user/meal-plans/days/{date})으로 그날 식단을 받는다
            <ul className={styles.slots}>
              {daySlots.map((slot) => (
                <li key={slot.slotId}>
                  <MealSlotRow slot={slot} onMore={() => handleMore(slot.slotId)} />
                </li>
              ))}
            </ul>
          )}
        </div>
      </DraggableSheet>

      {sheet === "month" && (
        <MonthPickerSheet
          month={month}
          onClose={() => setSheet(null)}
          onConfirm={(next) => {
            setMonth(next);
            setSelectedDate(`${next}-01`);
            setSheet(null);
          }}
        />
      )}

      {/* 피그마 p5-식단표-식단표 추가: 제목·닫기 없는 시트. 남은 횟수 + 식단표 만들기 + 안내 */}
      <BottomSheet
        isOpen={sheet === "add"}
        onClose={() => setSheet(null)}
        title="식단표 추가"
        isHeaderHidden
        isSquare
        footer={
          <div className={styles.addFooter}>
            <p className={styles.addNotice}>{quotaNotice}</p>
            <Button disabled={remainingPlans === 0} onClick={goSetup}>
              식단표 만들기
            </Button>
            <p className={styles.addFail}>{MEAL_PLAN_FAIL_NOTICE}</p>
          </div>
        }
      >
        {null}
      </BottomSheet>

      <SelectSheet
        isOpen={menuSlot !== null}
        onClose={() => setMenuSlotId(null)}
        title={menuSlot?.title ?? ""}
        options={SLOT_ACTIONS}
        defaultValues={[]}
        notice="레시피를 교체하게 될 경우 레시피 사용 횟수를 1회 차감 합니다"
        onConfirm={([action]) => handleSlotAction(action)}
      />

      {/* 문구를 바꿔 끼우면 읽히도록 항상 그려 두는 영역(blueprint §6.3) */}
      <div className={styles.toastArea} role="status" aria-live="polite">
        {toast.value && (
          <Toast variant="error" isLeaving={toast.isLeaving} onLeft={toast.remove}>
            {toast.value}
          </Toast>
        )}
      </div>
    </>
  );
}

/** 2자리 연도·월 휠 값 */
function range(from: number, to: number, label: (n: number) => string): WheelOption[] {
  return Array.from({ length: to - from + 1 }, (_, i) => ({
    value: String(from + i),
    label: label(from + i),
  }));
}

/** 피그마 p5-식단표-날짜 수정: "캘린더 선택" 입력 시트, 연·월 휠 2열(26 / 11), "수정" */
function MonthPickerSheet({
  month,
  onClose,
  onConfirm,
}: {
  month: string;
  onClose: () => void;
  onConfirm: (month: string) => void;
}) {
  const [year, setYear] = useState(month.slice(0, 4));
  const [monthNumber, setMonthNumber] = useState(String(Number(month.slice(5, 7))));
  const baseYear = Number(month.slice(0, 4));

  return (
    <InputSheet
      isOpen
      onClose={onClose}
      title="캘린더 선택"
      footer={
        <Button size="md" onClick={() => onConfirm(`${year}-${monthNumber.padStart(2, "0")}`)}>
          수정
        </Button>
      }
    >
      <WheelPicker
        columns={[
          {
            label: "연도",
            options: range(baseYear - 1, baseYear + 1, (n) => String(n).slice(-2)),
            value: year,
            onChange: setYear,
          },
          {
            label: "월",
            options: range(1, 12, String),
            value: monthNumber,
            onChange: setMonthNumber,
          },
        ]}
      />
    </InputSheet>
  );
}
