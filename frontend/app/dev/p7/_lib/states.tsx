// p7(장바구니) 화면 상태 목록. /dev/p7이 이 순서로 목록을 보이고, /dev/p7/[state]가 하나를 그린다.
// 상태를 더하면 여기에 한 줄 추가한다(blueprint §3.1 7번). 실제 라우트는 컨테이너 기본값(불러오는 중)을 쓰고, 마크업 확인용 값은 여기서만 넘긴다.

import { CartContainer } from "../../../user/cart/cart-items/_components/CartContainer/CartContainer";
import { SAMPLE_CART_ITEMS } from "../../../user/cart/cart-items/_lib/cart";
import type { PreviewState } from "../../_lib/preview";

const ROUTE = "/user/cart/cart-items";
const NOTICE = { message: "당근의 소비기한이 임박했어요!", unreadCount: 2 };
/** 피그마 p7-장바구니·수량 변경은 4건(목살 200g 줄 없음) */
const FOUR_ITEMS = SAMPLE_CART_ITEMS.filter((item) => item.id !== 2);
const SELECTED_FOUR = [1, 2, 3, 4];

export const P7_STATES: PreviewState[] = [
  {
    slug: "cart",
    label: "p7-장바구니",
    route: ROUTE,
    render: () => <CartContainer items={FOUR_ITEMS} notice={NOTICE} />,
  },
  {
    slug: "cart-selected",
    label: "p7-장바구니-항목 선택",
    route: ROUTE,
    render: () => (
      <CartContainer items={SAMPLE_CART_ITEMS} notice={NOTICE} initialSelectedIds={SELECTED_FOUR} />
    ),
  },
  {
    slug: "cart-delete",
    label: "p7-장바구니-항목 삭제",
    route: ROUTE,
    render: () => (
      <CartContainer
        items={SAMPLE_CART_ITEMS}
        notice={NOTICE}
        initialSelectedIds={SELECTED_FOUR}
        initialDeleteMode
      />
    ),
  },
  {
    slug: "cart-amount",
    label: "p7-장바구니-수량 변경 (목살)",
    route: ROUTE,
    render: () => <CartContainer items={FOUR_ITEMS} notice={NOTICE} initialAmountId={1} />,
  },
  {
    slug: "cart-empty",
    label: "p7-장바구니-빈 리스트",
    route: ROUTE,
    render: () => <CartContainer items={[]} notice={NOTICE} />,
  },
  {
    slug: "cart-loading",
    label: "p7-장바구니-불러오는 중 (실제 라우트 처음 상태, 피그마 없음)",
    route: ROUTE,
    render: () => <CartContainer />,
  },
];
