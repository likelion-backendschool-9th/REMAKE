"use client";

import { useId, useState } from "react";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { Checkbox } from "@/components/Checkbox/Checkbox";
import { EmptyState } from "@/components/EmptyState/EmptyState";
import { MainHeader } from "@/components/MainHeader/MainHeader";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SmallButton } from "@/components/SmallButton/SmallButton";
import { Toast } from "@/components/Toast/Toast";
import { useTimedToast } from "@/hooks/useTimedToast";
import { MAIN_TABS, NOTIFICATIONS_HREF, SETTINGS_HREF } from "@/lib/navigation";

import type { CartItem } from "../../_lib/cart";
import { CartAmountSheet } from "../CartAmountSheet/CartAmountSheet";
import { CartItemRow } from "../CartItemRow/CartItemRow";

import styles from "./CartContainer.module.css";

const CART_PATH = "/user/cart/cart-items";

type CartContainerProps = {
  /** 장바구니 항목(API 45). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  items?: CartItem[];
  /** 가장 급한 알림 한 줄(API 43). 없으면 알림 띠를 그리지 않는다 */
  notice?: { message: string; unreadCount: number };
  /** 처음 선택한 항목 id. 미리보기용 */
  initialSelectedIds?: number[];
  /** 삭제 모드로 시작. 미리보기용(p7-장바구니-항목 삭제) */
  initialDeleteMode?: boolean;
  /** 처음부터 수량 변경 시트를 열 항목 id. 미리보기용(p7-장바구니-수량 변경) */
  initialAmountId?: number;
};

/**
 * p7 장바구니. 자동으로 담긴 재료(식단표 부족분·레시피 저장 시 보충 재료)를 고른 뒤 냉장고에 넣거나 지운다(domain-rules 7).
 * "선택 삭제"를 누르면 삭제 모드(체크가 빨강, 버튼은 "삭제 해제"·"N개의 재료 삭제"), 다시 누르면 돌아온다.
 * 상태: 불러오는 중 / 기본(p7-장바구니) / 항목 선택 / 항목 삭제 / 수량 변경 시트 / 빈 리스트.
 */
export function CartContainer({
  items: initialItems,
  notice,
  initialSelectedIds = [],
  initialDeleteMode = false,
  initialAmountId,
}: CartContainerProps) {
  const selectAllId = useId();
  const isLoading = initialItems === undefined;
  // TODO: API 45를 붙이면 서버 값(쿼리)을 쓰고, 수량 변경·삭제·냉장고 넣기 뒤에는 쿼리를 무효화한다
  const [items, setItems] = useState(initialItems ?? []);
  const [selectedIds, setSelectedIds] = useState<ReadonlySet<number>>(
    () => new Set(initialSelectedIds),
  );
  const [isDeleteMode, setIsDeleteMode] = useState(initialDeleteMode);
  const [amountId, setAmountId] = useState<number | null>(initialAmountId ?? null);
  const toast = useTimedToast<string>();

  const isEmpty = !isLoading && items.length === 0;
  const selectedCount = items.filter((item) => selectedIds.has(item.id)).length;
  const isAllSelected = items.length > 0 && selectedCount === items.length;
  const amountItem = items.find((item) => item.id === amountId) ?? null;

  const toggle = (id: number) =>
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const toggleAll = () =>
    setSelectedIds(isAllSelected ? new Set() : new Set(items.map((item) => item.id)));

  const removeSelected = () => {
    setItems((prev) => prev.filter((item) => !selectedIds.has(item.id)));
    setSelectedIds(new Set());
  };

  const handleMoveToFridge = () => {
    // TODO: API 47(POST /user/cart-items/move-to-fridge { ids })의 addedCount로 토스트를 띄운다
    const count = selectedCount;
    removeSelected();
    // 피그마 없음(domain-rules 7 "screen pending"). 문구는 정책 그대로
    toast.show(`냉장고에 ${count}개 넣었어요`);
  };

  const handleDelete = () => {
    // TODO: API 48(POST /user/cart-items/delete { ids })
    removeSelected();
    setIsDeleteMode(false);
  };

  const handleAmount = (amount: number) => {
    // TODO: API 46(PATCH /user/cart-items/{id} { amount })
    setItems((prev) => prev.map((item) => (item.id === amountId ? { ...item, amount } : item)));
    setAmountId(null);
  };

  return (
    <>
      <MainHeader
        tabs={MAIN_TABS}
        activeHref={CART_PATH}
        settingsHref={SETTINGS_HREF}
        notice={notice && { ...notice, href: NOTIFICATIONS_HREF }}
      />
      <main className={styles.main} aria-busy={isLoading || undefined}>
        <div className={styles.band} aria-hidden="true" />

        <div className={styles.toolbar}>
          <label htmlFor={selectAllId} className={styles.selectAll}>
            <Checkbox
              id={selectAllId}
              checked={isAllSelected}
              onChange={toggleAll}
              disabled={isLoading || isEmpty}
            />
            {/* 피그마 p7-장바구니-빈 리스트: 비었으면 건수 없이 "전체 선택" */}
            {isLoading || isEmpty ? "전체 선택" : `전체 선택(${items.length}건)`}
          </label>
          <SmallButton
            tone="neutral"
            disabled={isLoading || isEmpty}
            aria-pressed={isDeleteMode}
            onClick={() => setIsDeleteMode((prev) => !prev)}
          >
            {isDeleteMode ? "삭제 해제" : "선택 삭제"}
          </SmallButton>
        </div>

        {isLoading ? (
          <div className={styles.skeletons}>
            <Skeleton width="100%" height="2.4375rem" />
            <Skeleton width="100%" height="2.4375rem" />
          </div>
        ) : isEmpty ? (
          <EmptyState className={styles.empty}>장바구니가 비어있습니다</EmptyState>
        ) : (
          <ul className={styles.list}>
            {items.map((item) => (
              <li key={item.id}>
                <CartItemRow
                  item={item}
                  isChecked={selectedIds.has(item.id)}
                  isDeleteMode={isDeleteMode}
                  onToggle={() => toggle(item.id)}
                  onChangeAmount={() => setAmountId(item.id)}
                />
              </li>
            ))}
          </ul>
        )}
      </main>

      <BottomBar>
        {isDeleteMode ? (
          <Button tone="danger" disabled={selectedCount === 0} onClick={handleDelete}>
            {selectedCount > 0 ? `${selectedCount}개의 재료 삭제` : "재료 삭제"}
          </Button>
        ) : (
          <Button disabled={selectedCount === 0} onClick={handleMoveToFridge}>
            {selectedCount > 0 ? `${selectedCount}개의 재료 냉장고에 추가` : "냉장고에 추가"}
          </Button>
        )}
      </BottomBar>

      <CartAmountSheet
        item={amountItem}
        onClose={() => setAmountId(null)}
        onConfirm={handleAmount}
      />

      {/* 문구를 바꿔 끼우면 읽히도록 항상 그려 두는 영역(blueprint §6.3) */}
      <div className={styles.toastArea} role="status" aria-live="polite">
        {toast.value && (
          <Toast variant="success" isLeaving={toast.isLeaving} onLeft={toast.remove}>
            {toast.value}
          </Toast>
        )}
      </div>
    </>
  );
}
