// p2(냉장고) 화면 상태 목록. /dev/p2가 이 순서로 목록을 보이고, /dev/p2/[state]가 하나를 그린다.
// 상태를 더하면 여기에 한 줄 추가한다(blueprint §3.1 7번). 실제 라우트는 컨테이너 기본값을 쓴다.

import { FridgeContainer } from "../../../user/fridge/ingredients/_components/FridgeContainer/FridgeContainer";
import type { PreviewState } from "../../_lib/preview";

export const P2_STATES: PreviewState[] = [
  {
    slug: "fridge",
    label: "p2-냉장고",
    route: "/user/fridge/ingredients",
    render: () => <FridgeContainer />,
  },
  {
    slug: "fridge-skeleton",
    label: "p2-냉장고-스켈레톤",
    route: "/user/fridge/ingredients",
    render: () => <FridgeContainer isLoading />,
  },
  {
    slug: "fridge-use",
    label: "p2-냉장고-재료 사용 여부 (쪽파)",
    route: "/user/fridge/ingredients",
    render: () => <FridgeContainer initialUseId={3} />,
  },
  {
    slug: "fridge-location",
    label: "p2-냉장고-재료 위치",
    route: "/user/fridge/ingredients",
    render: () => <FridgeContainer initialSheet="location" />,
  },
  {
    slug: "fridge-category",
    label: "p2-재료 카테고리",
    route: "/user/fridge/ingredients",
    render: () => <FridgeContainer initialSheet="category" />,
  },
  {
    slug: "fridge-empty",
    label: "p2-냉장고-빈 리스트",
    route: "/user/fridge/ingredients",
    render: () => <FridgeContainer ingredients={[]} />,
  },
];
