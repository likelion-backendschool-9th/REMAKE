import "dayjs/locale/ko";

import dayjs from "dayjs";
import isoWeek from "dayjs/plugin/isoWeek";
import relativeTime from "dayjs/plugin/relativeTime";

dayjs.extend(relativeTime);
dayjs.extend(isoWeek);
dayjs.locale("ko");

export { dayjs };
