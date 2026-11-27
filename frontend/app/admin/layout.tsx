import { Suspense } from "react";

import { AdminLayout } from "./_components/AdminLayout/AdminLayout";

/**
 * 모든 /admin 화면의 공통 틀(사이드 메뉴 + 본문). 지금 메뉴는 주소로 정한다.
 * 처리 대기 신고 숫자(API 65)와 권한 없음(a0, 프로필 role·서버 403)은 API 연동 때 붙인다
 */
export default function AdminRootLayout({ children }: LayoutProps<"/admin">) {
  // 메뉴가 주소(usePathname)를 읽으므로 Suspense로 감싼다. 값을 미리 알 수 없는 동적 주소(예: /admin/operation/reports/[sharedRecipeId])를 빌드할 때 필요하다(cacheComponents, blueprint §5)
  return (
    <Suspense>
      <AdminLayout>{children}</AdminLayout>
    </Suspense>
  );
}
