// p2(냉장고) 화면 상태 목록. /dev/p2가 이 순서로 목록을 보이고, /dev/p2/[state]가 하나를 그린다.
// 상태를 더하면 여기에 한 줄 추가한다(blueprint §3.1 7번). 실제 라우트는 컨테이너 기본값을 쓴다.

import { FridgeContainer } from "../../../user/fridge/ingredients/_components/FridgeContainer/FridgeContainer";
import {
  SAMPLE_EDITED_DRAFT,
  SAMPLE_FILLED_DRAFT,
} from "../../../user/fridge/ingredients/_lib/ingredient-form";
import { EditIngredientContainer } from "../../../user/fridge/ingredients/[id]/edit/_components/EditIngredientContainer/EditIngredientContainer";
import { AddIngredientContainer } from "../../../user/fridge/ingredients/new/_components/AddIngredientContainer/AddIngredientContainer";
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
  {
    slug: "add",
    label: "p2.2-재료 추가",
    route: "/user/fridge/ingredients/new",
    render: () => <AddIngredientContainer />,
  },
  {
    slug: "add-filled",
    label: "p2.2-재료 추가-모두 채움(버튼 켜짐)",
    route: "/user/fridge/ingredients/new",
    render: () => <AddIngredientContainer initialDraft={SAMPLE_FILLED_DRAFT} />,
  },
  {
    slug: "add-name",
    label: "p2.2-재료 추가-재료명 시트",
    route: "/user/fridge/ingredients/new",
    render: () => <AddIngredientContainer initialSheet="name" />,
  },
  {
    slug: "add-category",
    label: "p2.2-재료 추가-카테고리 시트",
    route: "/user/fridge/ingredients/new",
    render: () => <AddIngredientContainer initialSheet="category" />,
  },
  {
    slug: "add-storage",
    label: "p2.2-재료 추가-보관 위치 시트",
    route: "/user/fridge/ingredients/new",
    render: () => <AddIngredientContainer initialSheet="storage" />,
  },
  {
    slug: "add-expiry",
    label: "p2.2-재료 추가-소비 기한 시트",
    route: "/user/fridge/ingredients/new",
    render: () => <AddIngredientContainer initialSheet="expiry" />,
  },
  {
    slug: "add-price",
    label: "p2.2-재료 추가-재료 가격 시트",
    route: "/user/fridge/ingredients/new",
    render: () => <AddIngredientContainer initialSheet="price" />,
  },
  {
    slug: "edit",
    label: "p2.1-재료 수정-기본(바뀐 값 없음)",
    route: "/user/fridge/ingredients/1/edit",
    render: () => <EditIngredientContainer />,
  },
  {
    slug: "edit-changed",
    label: "p2.1-재료 수정",
    route: "/user/fridge/ingredients/1/edit",
    render: () => <EditIngredientContainer initialDraft={SAMPLE_EDITED_DRAFT} />,
  },
  {
    slug: "edit-expiry",
    label: "p2.1-재료 수정-소비 기한 시트",
    route: "/user/fridge/ingredients/1/edit",
    render: () => <EditIngredientContainer initialSheet="expiry" />,
  },
  {
    slug: "edit-delete",
    label: "p2.1-재료 수정-삭제 확인(피그마 없음)",
    route: "/user/fridge/ingredients/1/edit",
    render: () => <EditIngredientContainer initialDeleteOpen />,
  },
];
