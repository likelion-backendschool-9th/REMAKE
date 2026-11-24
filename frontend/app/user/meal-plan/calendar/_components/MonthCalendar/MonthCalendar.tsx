import { buildMonthGrid, type MealPlanPeriod } from "../../_lib/calendar";

import styles from "./MonthCalendar.module.css";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"] as const;

type MonthCalendarProps = {
  /** "2026-11" */
  month: string;
  periods: readonly MealPlanPeriod[];
  /** 식단이 있는 날. 이 날에만 파란 띠 */
  datesWithMeals: readonly string[];
  /** 오늘("2026-11-17"). 파란 글자 */
  today: string;
  /** 고른 날짜. 주황 굵은 글자(오늘을 골랐으면 주황이 우선) */
  selectedDate: string;
  onSelectDate: (date: string) => void;
};

/**
 * 피그마 p5-식단표 달력: 일~토 머리줄 + 날짜 칸(51.43 정사각형). 식단이 있는 날은 파란 띠(이어진 묶음의 첫날·끝날 둥글게),
 * 오늘은 파란 글자, 고른 날은 주황 굵은 글자(오늘을 골랐으면 주황), 다른 달 날짜는 흐린 글자.
 * 지난 날·지난 달도 볼 수 있다(domain-rules 5).
 */
export function MonthCalendar({
  month,
  periods,
  datesWithMeals,
  today,
  selectedDate,
  onSelectDate,
}: MonthCalendarProps) {
  const weeks = buildMonthGrid(month, periods, datesWithMeals);

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
                data-meals={day.hasMeals || undefined}
                data-start={day.isRunStart || undefined}
                data-end={day.isRunEnd || undefined}
              >
                <button
                  type="button"
                  className={styles.day}
                  data-other={day.isOtherMonth || undefined}
                  data-today={day.date === today || undefined}
                  data-selected={day.date === selectedDate || undefined}
                  aria-pressed={day.date === selectedDate}
                  aria-label={`${Number(day.date.slice(5, 7))}월 ${day.day}일${day.hasMeals ? ", 식단 있음" : ""}`}
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
