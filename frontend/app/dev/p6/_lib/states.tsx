// p6(알림) 화면 상태 목록. /dev/p6이 이 순서로 목록을 보이고, /dev/p6/[state]가 하나를 그린다.
// 상태를 더하면 여기에 한 줄 추가한다(blueprint §3.1 7번). 실제 라우트는 컨테이너 기본값(불러오는 중)을 쓰고, 마크업 확인용 값은 여기서만 넘긴다.

import { NotificationsContainer } from "../../../user/notification/notifications/_components/NotificationsContainer/NotificationsContainer";
import type { PreviewState } from "../../_lib/preview";
import { SampleNotifications } from "./SampleNotifications";

export const P6_STATES: PreviewState[] = [
  {
    slug: "notifications",
    label: "p6-알람",
    route: "/user/notification/notifications",
    render: () => <SampleNotifications />,
  },
  {
    slug: "notifications-skeleton",
    label: "p6-알람-스켈레톤 (실제 라우트 처음 상태)",
    route: "/user/notification/notifications",
    render: () => <NotificationsContainer />,
  },
  {
    slug: "notifications-empty",
    label: "p6-알람-빈 목록 (피그마 없음)",
    route: "/user/notification/notifications",
    render: () => <NotificationsContainer notifications={[]} />,
  },
];
