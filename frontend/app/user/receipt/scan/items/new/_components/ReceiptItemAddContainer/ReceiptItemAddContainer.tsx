"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import {
  IngredientForm,
  IngredientFormSkeleton,
  type IngredientSheet,
} from "@/components/IngredientForm/IngredientForm";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { EMPTY_DRAFT, type IngredientDraft, isDraftComplete } from "@/lib/ingredient-form";

import { draftToScanItem, RECEIPT_TEXT_SHEET_COPY, SCAN_RESULT_PATH } from "../../../../_lib/scan";

import styles from "./ReceiptItemAddContainer.module.css";

type ReceiptItemAddContainerProps = {
  /** 불러오는 중(OCR 결과 화면 상태를 아직 못 읽음). 미리보기용 */
  isLoading?: boolean;
  /** 처음 값. 미리보기용, 기본 빈 값(피그마 p3.3.2-재료 추가) */
  initialDraft?: IngredientDraft;
  /** 처음부터 열어 둘 값 시트. 미리보기용 */
  initialSheet?: IngredientSheet | null;
};

/**
 * p3.3.2 영수증 재료 추가. 냉장고 재료 추가(p2.2)와 같은 화면. 가격을 뺀 값을 모두 채우면 "재료 추가"가 켜진다.
 * 추가한 품목은 서버로 보내지 않고 OCR 결과 화면 상태에 더한다. 등록(API 21) 때 한꺼번에 보낸다(domain-rules 3).
 * 상태: 불러오는 중(p3.3.2-재료 추가-스켈레톤) / 빈 값 / 값 시트(재료명·카테고리·보관 위치·소비 기한·가격).
 */
export function ReceiptItemAddContainer({
  isLoading = false,
  initialDraft = EMPTY_DRAFT,
  initialSheet = null,
}: ReceiptItemAddContainerProps) {
  const router = useRouter();
  const [draft, setDraft] = useState(initialDraft);

  const handleSubmit = () => {
    const item = draftToScanItem(draft);
    if (!item) return;
    // TODO: OCR 결과 화면 상태의 품목 끝에 item을 더한다(상태를 둘 곳은 API 연동 때 정함)
    router.replace(SCAN_RESULT_PATH);
  };

  return (
    <>
      <SubHeader title="재료 추가" />
      <main className={styles.main} aria-busy={isLoading || undefined}>
        {isLoading ? (
          <IngredientFormSkeleton />
        ) : (
          <IngredientForm
            draft={draft}
            onChange={setDraft}
            textSheetCopy={RECEIPT_TEXT_SHEET_COPY}
            initialSheet={initialSheet}
          />
        )}
      </main>
      <BottomBar>
        <Button disabled={isLoading || !isDraftComplete(draft)} onClick={handleSubmit}>
          재료 추가
        </Button>
      </BottomBar>
    </>
  );
}
