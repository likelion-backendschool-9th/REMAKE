import { buildMonthGrid, type MealPlanPeriod } from "../../_lib/calendar";

import styles from "./MonthCalendar.module.css";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"] as const;

type MonthCalendarProps = {
  /** "2026-11" */
  month: string;
  periods: readonly MealPlanPeriod[];
  /** 고른 날짜("2026-11-17"). 주황 굵은 글자 */
  selectedDate: string;
  onSelectDate: (date: string) => void;
};

/**
 * 피그마 p5-식단표 달력: 일~토 머리줄 + 날짜 칸(51.43 정사각형). 식단 기간은 파란 띠(시작·끝 둥글게),
 * 기간 시작일은 파란 글자, 고른 날은 주황 굵은 글자, 다른 달 날짜는 흐린 글자.
 * 지난 날·지난 달도 볼 수 있다(domain-rules 5).
 */
export function MonthCalendar({ month, periods, selectedDate, onSelectDate }: MonthCalendarProps) {
  const weeks = buildMonthGrid(month, periods);

  return (
    <table className={styles.calendar}>
      <thead>
        <tr>
          {WEEKDAYS.map((weekday, index) => (
            <th
              key={weekday}
              scope="col"
              className={styles.weekday}
              data-weekend={index === 0 || index === 6 || undefined}
            >
              {weekday}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {weeks.map((week) => (
          <tr key={week[0].date}>
            {week.map((day) => (
              <td
                key={day.date}
                className={styles.cell}
                data-period={day.inPeriod || undefined}
                data-start={day.isPeriodStart || undefined}
                data-end={day.isPeriodEnd || undefined}
              >
                <button
                  type="button"
                  className={styles.day}
                  data-other={day.isOtherMonth || undefined}
                  data-start={day.isPeriodStart || undefined}
                  data-selected={day.date === selectedDate || undefined}
                  aria-pressed={day.date === selectedDate}
                  aria-label={`${Number(day.date.slice(5, 7))}월 ${day.day}일${day.inPeriod ? ", 식단 있음" : ""}`}
                  onClick={() => onSelectDate(day.date)}
                >
                  {day.day}
                </button>
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
