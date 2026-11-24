"use client";

import { useState } from "react";

import { NotificationsContainer } from "../../../user/notification/notifications/_components/NotificationsContainer/NotificationsContainer";
import { createSampleNotifications } from "../../../user/notification/notifications/_lib/notifications";

/** 예시 알림 날짜를 "오늘" 기준으로 만든다. 서버(cacheComponents)에서 new Date()를 쓰지 않도록 브라우저에서 만든다 */
export function SampleNotifications() {
  const [notifications] = useState(() => createSampleNotifications(new Date()));
  return <NotificationsContainer notifications={notifications} />;
}
