// p3(영수증) 화면 상태 목록. /dev/p3이 이 순서로 목록을 보이고, /dev/p3/[state]가 하나를 그린다.
// 상태를 더하면 여기에 한 줄 추가한다(blueprint §3.1 7번). 실제 라우트는 컨테이너 기본값(불러오는 중)을 쓰고, 마크업 확인용 값은 여기서만 넘긴다.

import { PatternsContainer } from "../../../user/receipt/patterns/_components/PatternsContainer/PatternsContainer";
import {
  SAMPLE_DAILY_ITEMS,
  SAMPLE_DISCARDED,
  SAMPLE_EMPTY_DAY,
  SAMPLE_PATTERN,
} from "../../../user/receipt/patterns/_lib/patterns";
import { DiscardedContainer } from "../../../user/receipt/patterns/discarded/_components/DiscardedContainer/DiscardedContainer";
import { ReceiptsContainer } from "../../../user/receipt/receipts/_components/ReceiptsContainer/ReceiptsContainer";
import {
  SAMPLE_RECEIPT_DETAIL,
  SAMPLE_RECEIPT_MONTH,
} from "../../../user/receipt/receipts/_lib/receipts";
import { ReceiptDetailContainer } from "../../../user/receipt/receipts/[id]/_components/ReceiptDetailContainer/ReceiptDetailContainer";
import { SAMPLE_SCAN_EDIT_ITEM, SAMPLE_SCAN_RESULT } from "../../../user/receipt/scan/_lib/scan";
import { ReceiptItemEditContainer } from "../../../user/receipt/scan/items/[index]/edit/_components/ReceiptItemEditContainer/ReceiptItemEditContainer";
import { ReceiptItemAddContainer } from "../../../user/receipt/scan/items/new/_components/ReceiptItemAddContainer/ReceiptItemAddContainer";
import { ReceiptResultContainer } from "../../../user/receipt/scan/result/_components/ReceiptResultContainer/ReceiptResultContainer";
import type { PreviewState } from "../../_lib/preview";

const ROUTE = "/user/receipt/receipts";
const DETAIL_ROUTE = "/user/receipt/receipts/[id]";
const RESULT_ROUTE = "/user/receipt/scan/result";
const ITEM_EDIT_ROUTE = "/user/receipt/scan/items/[index]/edit";
const ITEM_ADD_ROUTE = "/user/receipt/scan/items/new";
const PATTERNS_ROUTE = "/user/receipt/patterns";
const DISCARDED_ROUTE = "/user/receipt/patterns/discarded";
/** 피그마 p3.3.1처럼 재료명을 우둔살로 바꾼 상태 */
// 수정 중인 입력 값(재료명만 바꿈). 예시 품목은 양이 있어 0으로 바꿀 일이 없다
const EDITED_DRAFT = {
  ...SAMPLE_SCAN_EDIT_ITEM,
  amount: SAMPLE_SCAN_EDIT_ITEM.amount ?? 0,
  name: "우둔살",
};

const ITEM_SHEETS = [
  ["name", "재료명 수정"],
  ["category", "카테고리 수정"],
  ["storage", "보관 위치"],
  ["expiry", "소비 기한"],
  ["price", "재료 가격 수정"],
] as const;
const NOTICE = { message: "당근의 소비기한이 임박했어요!", unreadCount: 2 };
const TODAY = "2026-11-24";

export const P3_STATES: PreviewState[] = [
  {
    slug: "receipts",
    label: "p3-영수증",
    route: ROUTE,
    render: () => (
      <ReceiptsContainer receiptMonth={SAMPLE_RECEIPT_MONTH} notice={NOTICE} today={TODAY} />
    ),
  },
  {
    slug: "receipts-skeleton",
    label: "p3-영수증-스켈레톤 (실제 라우트 처음 상태)",
    route: ROUTE,
    render: () => <ReceiptsContainer notice={NOTICE} />,
  },
  {
    slug: "receipts-empty",
    label: "p3-영수증-빈 리스트",
    route: ROUTE,
    render: () => (
      <ReceiptsContainer
        receiptMonth={{ month: "2026-11", totalAmount: 0, averageDiffAmount: null, weeks: [] }}
        notice={NOTICE}
        today={TODAY}
      />
    ),
  },
  {
    slug: "receipts-month",
    label: "p3-영수증-날짜 선택",
    route: ROUTE,
    render: () => (
      <ReceiptsContainer
        receiptMonth={SAMPLE_RECEIPT_MONTH}
        notice={NOTICE}
        today={TODAY}
        initialMonthSheetOpen
      />
    ),
  },
  {
    slug: "detail",
    label: "p3.2-영수증 디테일",
    route: DETAIL_ROUTE,
    render: () => <ReceiptDetailContainer receipt={SAMPLE_RECEIPT_DETAIL} />,
  },
  {
    slug: "detail-skeleton",
    label: "p3.2-영수증 디테일-스켈레톤 (실제 라우트 처음 상태)",
    route: DETAIL_ROUTE,
    render: () => <ReceiptDetailContainer />,
  },
  {
    slug: "detail-delete",
    label: "p3.2-영수증 디테일-영수증 삭제",
    route: DETAIL_ROUTE,
    render: () => <ReceiptDetailContainer receipt={SAMPLE_RECEIPT_DETAIL} initialDeleteOpen />,
  },
  {
    slug: "result",
    label: "p3.3-ocr 결과",
    route: RESULT_ROUTE,
    render: () => <ReceiptResultContainer result={SAMPLE_SCAN_RESULT} today={TODAY} />,
  },
  {
    slug: "result-skeleton",
    label: "p3.3-ocr 결과-스켈레톤 (실제 라우트 처음 상태)",
    route: RESULT_ROUTE,
    render: () => <ReceiptResultContainer />,
  },
  {
    slug: "item-edit",
    label: "p3.3.1-재료 수정",
    route: ITEM_EDIT_ROUTE,
    render: () => (
      <ReceiptItemEditContainer item={SAMPLE_SCAN_EDIT_ITEM} initialDraft={EDITED_DRAFT} />
    ),
  },
  {
    slug: "item-edit-skeleton",
    label: "p3.3.1-재료 수정-스켈레톤 (실제 라우트 처음 상태)",
    route: ITEM_EDIT_ROUTE,
    render: () => <ReceiptItemEditContainer />,
  },
  ...ITEM_SHEETS.map(([sheet, name]): PreviewState => ({
    slug: `item-edit-${sheet}`,
    label: `p3.3.1-재료 수정-${name}`,
    route: ITEM_EDIT_ROUTE,
    render: () => (
      <ReceiptItemEditContainer
        item={SAMPLE_SCAN_EDIT_ITEM}
        initialDraft={EDITED_DRAFT}
        initialSheet={sheet}
      />
    ),
  })),
  {
    slug: "item-edit-delete",
    label: "p3.3.1-재료 수정-삭제 모달",
    route: ITEM_EDIT_ROUTE,
    render: () => <ReceiptItemEditContainer item={SAMPLE_SCAN_EDIT_ITEM} initialDeleteOpen />,
  },
  {
    slug: "item-add",
    label: "p3.3.2-재료 추가",
    route: ITEM_ADD_ROUTE,
    render: () => <ReceiptItemAddContainer />,
  },
  {
    slug: "item-add-skeleton",
    label: "p3.3.2-재료 추가-스켈레톤",
    route: ITEM_ADD_ROUTE,
    render: () => <ReceiptItemAddContainer isLoading />,
  },
  ...ITEM_SHEETS.map(([sheet, name]): PreviewState => ({
    slug: `item-add-${sheet}`,
    label: `p3.3.2-재료 추가-${name}`,
    route: ITEM_ADD_ROUTE,
    render: () => <ReceiptItemAddContainer initialSheet={sheet} />,
  })),
  {
    slug: "patterns",
    label: "p3.4-소비 패턴",
    route: PATTERNS_ROUTE,
    render: () => (
      <PatternsContainer pattern={SAMPLE_PATTERN} dayItems={SAMPLE_DAILY_ITEMS} today={TODAY} />
    ),
  },
  {
    slug: "patterns-empty",
    label: "p3.4-소비 패턴-빈 리스트",
    route: PATTERNS_ROUTE,
    render: () => (
      <PatternsContainer pattern={SAMPLE_PATTERN} dayItems={SAMPLE_EMPTY_DAY} today={TODAY} />
    ),
  },
  {
    slug: "patterns-skeleton",
    label: "p3.4-소비 패턴-스켈레톤 (실제 라우트 처음 상태)",
    route: PATTERNS_ROUTE,
    render: () => <PatternsContainer today={TODAY} />,
  },
  {
    slug: "discarded",
    label: "p3.4.1-이번 달 버린 식재료",
    route: DISCARDED_ROUTE,
    render: () => <DiscardedContainer discarded={SAMPLE_DISCARDED} />,
  },
  {
    slug: "discarded-skeleton",
    label: "p3.4.1-이번 달 버린 식재료-불러오는 중 (피그마 없음)",
    route: DISCARDED_ROUTE,
    render: () => <DiscardedContainer />,
  },
];
