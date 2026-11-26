"use client";

import { AdminLoadError } from "../../_components/AdminLoadError/AdminLoadError";
import { AdminPageHeader } from "../../_components/AdminPageHeader/AdminPageHeader";

export default function Error({ reset }: { reset: () => void }) {
  return (
    <>
      <AdminPageHeader title="사용자 관리" />
      <AdminLoadError title="화면을 불러오지 못했어요" onRetry={reset} />
    </>
  );
}
