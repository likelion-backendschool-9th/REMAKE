"use client";

import { useState } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";

import { ConfirmModal } from "@/components/ConfirmModal/ConfirmModal";
import { IngredientRow } from "@/components/IngredientRow/IngredientRow";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { TextButton } from "@/components/TextButton/TextButton";
import { formatWon } from "@/lib/format";
import { formatAmount, getCategoryImage, getStorage } from "@/lib/ingredients";

import { StoreBadge } from "../../../../_components/StoreBadge/StoreBadge";
import { formatPurchaseDate, type ReceiptDetail } from "../../../_lib/receipts";

import styles from "./ReceiptDetailContainer.module.css";

const RECEIPTS_PATH = "/user/receipt/receipts";

type ReceiptDetailContainerProps = {
  /** 영수증 상세(API 23). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  receipt?: ReceiptDetail;
  /** 삭제 확인 모달을 처음부터 연다. 미리보기용 */
  initialDeleteOpen?: boolean;
};

/**
 * p3.2 영수증 디테일. 위(회색): 매장 배지·매장 이름, 구매일·식재료 수, 총 금액. 아래: 구매한 식재료 목록, 영수증 원본.
 * 오른쪽 위 "삭제" → 확인 모달 → 삭제하면 목록으로(API 24, 식비·소비 패턴에서 빠지고 냉장고 재료는 남음).
 * 등록한 영수증은 고칠 수 없고 지우기만 한다(blueprint Receipt flow).
 * 상태: 기본(p3.2-영수증 디테일) / 불러오는 중(p3.2-영수증 디테일-스켈레톤) / 삭제 확인(p3.2-영수증 디테일-영수증 삭제).
 */
export function ReceiptDetailContainer({
  receipt,
  initialDeleteOpen = false,
}: ReceiptDetailContainerProps) {
  const router = useRouter();
  const [isDeleteOpen, setIsDeleteOpen] = useState(initialDeleteOpen);

  const handleDelete = () => {
    // TODO: API 24(DELETE /user/receipts/{id})를 붙이면 성공한 뒤 목록 캐시를 지우고 이동한다
    setIsDeleteOpen(false);
    router.push(RECEIPTS_PATH);
  };

  return (
    <>
      <div className={styles.summaryArea}>
        <SubHeader
          title="영수증"
          className={styles.header}
          right={
            <TextButton tone="danger" disabled={!receipt} onClick={() => setIsDeleteOpen(true)}>
              삭제
            </TextButton>
          }
        />
        {receipt ? <ReceiptSummary receipt={receipt} /> : <SummarySkeleton />}
      </div>

      <main className={styles.main} aria-busy={!receipt || undefined}>
        <section aria-labelledby="receipt-items-title">
          <h2 id="receipt-items-title" className={styles.sectionTitle}>
            구매한 식재료
            {receipt && <span className={styles.count}>{receipt.items.length}</span>}
          </h2>
          {receipt ? (
            <ul className={styles.items}>
              {receipt.items.map((item, index) => (
                // 영수증 품목은 id가 없다(API 23). 같은 영수증 안에서 순서는 바뀌지 않는다

                <li key={index}>
                  <IngredientRow
                    name={item.name}
                    imageSrc={item.imageUrl ?? getCategoryImage(item.categoryCode)}
                    description={`${getStorage(item.storage).place} · ${formatAmount(item.amount, item.unit)}`}
                    trailing={<span className={styles.price}>{formatWon(item.price)}</span>}
                    className={styles.item}
                  />
                </li>
              ))}
            </ul>
          ) : (
            // 피그마 p3.2-영수증 디테일-스켈레톤: 품목 자리 2개
            <div className={styles.itemSkeletons}>
              <Skeleton width="100%" height="2.75rem" />
              <Skeleton width="100%" height="2.75rem" />
            </div>
          )}
        </section>

        {receipt?.imageUrl && (
          <section aria-labelledby="receipt-image-title" className={styles.original}>
            <h2 id="receipt-image-title" className={styles.sectionTitle}>
              영수증 원본
            </h2>
            {/* TODO: 실제 이미지 주소가 정해지면 images.remotePatterns에 호스트를 넣는다 */}
            <div className={styles.imageBox}>
              <Image
                src={receipt.imageUrl}
                alt={`${receipt.storeName} 영수증 원본`}
                fill
                sizes="(max-width: 480px) 100vw, 448px"
                className={styles.image}
              />
            </div>
          </section>
        )}
      </main>

      <ConfirmModal
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        onConfirm={handleDelete}
        title="영수증 삭제"
        message="영수증을 삭제하시겠습니까? 삭제하면 식비·소비 패턴에서도 빠져요. 등록한 냉장고 재료는 남아요."
        cancelLabel="취소하기"
        confirmLabel="삭제하기"
        tone="danger"
      />
    </>
  );
}

function ReceiptSummary({ receipt }: { receipt: ReceiptDetail }) {
  return (
    <div className={styles.summary}>
      <div className={styles.store}>
        <StoreBadge storeCode={receipt.storeCode} decorative />
        <p className={styles.storeName}>{receipt.storeName}</p>
      </div>
      <dl className={styles.facts}>
        <div className={styles.fact}>
          <dt>구매일</dt>
          <dd>{formatPurchaseDate(receipt.purchasedOn)}</dd>
        </div>
        <div className={styles.fact}>
          <dt>식재료</dt>
          <dd>{receipt.items.length}개</dd>
        </div>
      </dl>
      <div className={styles.total}>
        <span className={styles.totalLabel}>총 금액</span>
        <span className={styles.totalAmount}>{formatWon(receipt.totalAmount)}</span>
      </div>
    </div>
  );
}

/** 피그마 p3.2-영수증 디테일-스켈레톤: 배지·이름 자리, 구매일·식재료 값 자리, 총 금액 값 자리(글자 이름은 그대로) */
function SummarySkeleton() {
  return (
    <div className={styles.summary}>
      <div className={styles.store}>
        <Skeleton width="2.75rem" height="2.75rem" />
        <Skeleton width="13.875rem" height="1.375rem" />
      </div>
      <dl className={styles.facts}>
        <div className={styles.fact}>
          <dt>구매일</dt>
          <dd>
            <Skeleton width="9.375rem" height="1.25rem" />
          </dd>
        </div>
        <div className={styles.fact}>
          <dt>식재료</dt>
          <dd>
            <Skeleton width="9.375rem" height="1.25rem" />
          </dd>
        </div>
      </dl>
      <div className={styles.total}>
        <span className={styles.totalLabel}>총 금액</span>
        <Skeleton width="4.6875rem" height="1.6875rem" />
      </div>
    </div>
  );
}
