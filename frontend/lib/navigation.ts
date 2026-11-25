// 메인 헤더 상단 탭 5개와 설정 주소(blueprint §4). 냉장고·영수증·레시피·식단표·장바구니 화면이 같이 쓴다.

import type { TopTab } from "@/components/TopTabs/TopTabs";

export const MAIN_TABS: readonly TopTab[] = [
  { href: "/user/fridge/ingredients", label: "냉장고" },
  { href: "/user/receipt/receipts", label: "영수증" },
  { href: "/user/recipe/saved-recipes", label: "레시피" },
  { href: "/user/meal-plan/calendar", label: "식단표" },
  { href: "/user/cart/cart-items", label: "장바구니" },
];

export const SETTINGS_HREF = "/user/setting/profile";
export const NOTIFICATIONS_HREF = "/user/notification/notifications";
