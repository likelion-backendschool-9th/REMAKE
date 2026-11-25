import "dayjs/locale/ko";

import dayjs from "dayjs";
import isoWeek from "dayjs/plugin/isoWeek";
import relativeTime from "dayjs/plugin/relativeTime";

dayjs.extend(relativeTime);
dayjs.extend(isoWeek);
dayjs.locale("ko");

export { dayjs };

/**
 * 오늘 날짜 "2026-11-25"를 KST로. 기기 시간대가 달라도 서버(KST)와 같은 날로 센다.
 * 화면에서 "오늘"(월 고르기, 미래 날짜 막기, D-N 계산)을 정할 때는 dayjs() 대신 이것을 쓴다
 */
export function todayInKst(now: Date = new Date()): string {
  // en-CA 형식이 YYYY-MM-DD
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul" }).format(now);
}
