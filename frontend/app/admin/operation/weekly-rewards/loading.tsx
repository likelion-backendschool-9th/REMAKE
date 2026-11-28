import { Skeleton } from "@/components/Skeleton/Skeleton";

import { AdminPageHeader } from "../../_components/AdminPageHeader/AdminPageHeader";

export default function Loading() {
  return (
    <>
      <AdminPageHeader title="랭킹·보상" />
      <div aria-busy="true" aria-label="불러오는 중">
        <Skeleton width="100%" height="24rem" />
      </div>
    </>
  );
}
