// p3(영수증) 화면 상태 목록. /dev/p3이 이 순서로 목록을 보이고, /dev/p3/[state]가 하나를 그린다.
// 상태를 더하면 여기에 한 줄 추가한다(blueprint §3.1 7번). 실제 라우트는 컨테이너 기본값(불러오는 중)을 쓰고, 마크업 확인용 값은 여기서만 넘긴다.

import { ReceiptsContainer } from "../../../user/receipt/receipts/_components/ReceiptsContainer/ReceiptsContainer";
import { SAMPLE_RECEIPT_MONTH } from "../../../user/receipt/receipts/_lib/receipts";
import type { PreviewState } from "../../_lib/preview";

const ROUTE = "/user/receipt/receipts";
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
];
