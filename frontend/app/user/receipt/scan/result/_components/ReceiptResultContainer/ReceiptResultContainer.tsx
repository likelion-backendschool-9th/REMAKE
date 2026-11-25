"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { MenuDotsVerticalIcon } from "@solar-icons/react/bold";

import { BottomSheet } from "@/components/BottomSheet/BottomSheet";
import { Button } from "@/components/Button/Button";
import { ExpiryLabel } from "@/components/ExpiryLabel/ExpiryLabel";
import { IconButton } from "@/components/IconButton/IconButton";
import { IngredientRow } from "@/components/IngredientRow/IngredientRow";
import { SegmentedControl } from "@/components/SegmentedControl/SegmentedControl";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { todayInKst } from "@/lib/dayjs";
import { formatNumber, formatWon } from "@/lib/format";
import { formatAmount, getCategoryImage, getStorage } from "@/lib/ingredients";

import {
  EXPIRY_NOTICE,
  getExpiry,
  RESULT_MENU,
  type ResultMenuAction,
  SCAN_ITEM_NEW_PATH,
  SCAN_PATH,
  scanItemEditHref,
  type ScanResult,
  sumScanPrice,
} from "../../../_lib/scan";

import styles from "./ReceiptResultContainer.module.css";

type ReceiptResultContainerProps = {
  /** OCR 결과(API 20). 없으면 분석 중(최대 30초). 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  result?: ScanResult;
  /** 오늘("2026-11-24"). 미리보기용, 기본은 화면을 연 날(소비기한 D-N 계산) */
  today?: string;
  /** ⋮ 시트를 처음부터 연다. 미리보기용 */
  initialMenuOpen?: boolean;
};

/**
 * p3.3 OCR 결과("영수증 재료"). 인식한 식재료 목록(보관 위치·양·가격, 소비기한, > 수정), 소비기한 추정 안내,
 * 아래 고정: 총 금액 + "식재료 N개 등록하기". 오른쪽 위 ⋮ = 시트(재료 추가 / 영수증 재촬영, 피그마 p3.3-ocr 결과-케밥 선택).
 * 등록 전에는 아무것도 저장하지 않는다. 등록하지 않고 나가면 결과를 버린다(blueprint Receipt flow).
 * 상태: 기본(p3.3-ocr 결과) / 분석 중(p3.3-ocr 결과-스켈레톤) / ⋮ 시트(p3.3-ocr 결과-케밥 선택).
 */
export function ReceiptResultContainer({
  result,
  today,
  initialMenuOpen = false,
}: ReceiptResultContainerProps) {
  const router = useRouter();
  const [isMenuOpen, setIsMenuOpen] = useState(initialMenuOpen);
  // 맨 위 항목이 기본 선택(blueprint §6)
  const [menuAction, setMenuAction] = useState<ResultMenuAction>(RESULT_MENU[0].value);

  const handleMenu = (action: ResultMenuAction) => {
    // 다른 화면으로 가기 전에 먼저 닫는다(blueprint §7.1)
    setIsMenuOpen(false);
    // TODO: 재촬영은 지금 OCR 결과를 버린다(receipt_result_abandoned). 확인 모달이 필요한지는 피그마 대기
    if (action === "ADD") router.push(SCAN_ITEM_NEW_PATH);
    if (action === "RESCAN") router.replace(SCAN_PATH);
  };

  return (
    <>
      <SubHeader
        title="영수증 재료"
        className={styles.header}
        right={
          <IconButton
            icon={<MenuDotsVerticalIcon />}
            iconSize={16}
            label="영수증 재료 더보기"
            aria-haspopup="dialog"
            disabled={!result}
            onClick={() => setIsMenuOpen(true)}
          />
        }
      />
      <main className={styles.main} aria-busy={!result || undefined}>
        {result ? (
          <ResultItems result={result} today={today} />
        ) : (
          // 피그마 p3.3-ocr 결과-스켈레톤: 재료 줄 자리 2개
          <div className={styles.skeletons}>
            <Skeleton width="100%" height="2.1875rem" />
            <Skeleton width="100%" height="2.1875rem" />
          </div>
        )}
      </main>

      <div className={styles.footer}>
        <div className={styles.total}>
          <span className={styles.totalLabel}>총 금액</span>
          {result ? (
            // 피그마 표기 "₩ 23,400"
            <span className={styles.totalAmount} aria-label={formatWon(sumScanPrice(result.items))}>
              ₩ {formatNumber(sumScanPrice(result.items))}
            </span>
          ) : (
            <Skeleton width="3.6875rem" height="1.0625rem" />
          )}
        </div>
        <div className={styles.action}>
          {/* TODO: 장바구니 차감 시트(#57) → API 21 등록. 매장 이름·구매일이 비었으면 등록 전에 채우게 한다(화면 대기) */}
          <Button disabled={!result || result.items.length === 0}>
            식재료 {result?.items.length ?? 0}개 등록하기
          </Button>
        </div>
      </div>

      <BottomSheet
        isOpen={isMenuOpen}
        onClose={() => setIsMenuOpen(false)}
        title="영수증 재료"
        footer={<Button onClick={() => handleMenu(menuAction)}>확인</Button>}
      >
        <SegmentedControl
          label="영수증 재료 메뉴"
          orientation="vertical"
          options={RESULT_MENU}
          value={menuAction}
          onChange={setMenuAction}
        />
      </BottomSheet>
    </>
  );
}

/** 값이 온 뒤의 목록. D-N을 셀 오늘 날짜는 이때 읽는다(cacheComponents) */
function ResultItems({ result, today: todayProp }: { result: ScanResult; today?: string }) {
  const [today] = useState(() => todayProp ?? todayInKst());

  return (
    <>
      <ul className={styles.items} aria-label="인식한 식재료">
        {result.items.map((item, index) => {
          const expiry = getExpiry(item.expirationDate, today);
          return (
            // OCR 품목은 id가 없다(API 20). 수정 화면도 순서로 찾는다

            <li key={index}>
              <IngredientRow
                name={item.name}
                imageSrc={getCategoryImage(item.categoryCode)}
                description={[
                  getStorage(item.storage).place,
                  formatAmount(item.amount, item.unit),
                  item.price === null ? null : formatWon(item.price),
                ]
                  .filter(Boolean)
                  .join(" · ")}
                trailing={<ExpiryLabel status={expiry.status} text={expiry.text} />}
                editHref={scanItemEditHref(index)}
                className={styles.item}
              />
            </li>
          );
        })}
      </ul>
      <p className={styles.notice}>{EXPIRY_NOTICE}</p>
    </>
  );
}
