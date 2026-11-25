"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { TrashBinTrashIcon } from "@solar-icons/react/linear";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { ConfirmModal } from "@/components/ConfirmModal/ConfirmModal";
import { IconButton } from "@/components/IconButton/IconButton";
import {
  IngredientForm,
  IngredientFormSkeleton,
  type IngredientSheet,
} from "@/components/IngredientForm/IngredientForm";
import { IngredientRow } from "@/components/IngredientRow/IngredientRow";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import {
  describeIngredient,
  type IngredientDraft,
  isDraftChanged,
  isDraftComplete,
  toDraft,
} from "@/lib/ingredient-form";
import { getCategoryImage } from "@/lib/ingredients";

import {
  draftToScanItem,
  RECEIPT_TEXT_SHEET_COPY,
  SCAN_RESULT_PATH,
  type ScanItem,
  scanItemToDetail,
} from "../../../../../_lib/scan";

import styles from "./ReceiptItemEditContainer.module.css";

type ReceiptItemEditContainerProps = {
  /** 고칠 OCR 품목(화면 상태). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  item?: ScanItem;
  /** 품목 순서(주소의 [index]) */
  index?: number;
  /** 처음 입력 값. 미리보기용, 기본은 OCR 값 그대로 */
  initialDraft?: IngredientDraft;
  /** 처음부터 열어 둘 값 시트. 미리보기용 */
  initialSheet?: IngredientSheet | null;
  /** 처음부터 삭제 확인 모달을 연다. 미리보기용 */
  initialDeleteOpen?: boolean;
};

/**
 * p3.3.1 영수증 재료 수정. 냉장고 재료 수정(p2.1)과 같은 화면: 위에 OCR 값 요약, 아래 입력 카드, 오른쪽 위 휴지통.
 * 고친 값은 서버로 보내지 않고 OCR 결과 화면 상태만 바꾼다. 등록(API 21) 때 한꺼번에 보낸다(domain-rules 3).
 * 상태: 불러오는 중(p3.3.1-재료 수정-스켈레톤) / 기본 / 값 시트(재료명·카테고리·보관 위치·소비 기한·가격) / 삭제 모달.
 */
export function ReceiptItemEditContainer({
  item,
  index = 0,
  ...rest
}: ReceiptItemEditContainerProps) {
  if (!item) return <ReceiptItemEditLoading />;
  return <ReceiptItemEditBody key={index} item={item} index={index} {...rest} />;
}

/** 피그마 p3.3.1-재료 수정-스켈레톤: 요약 줄 자리 + 값 자리만 회색인 카드 */
function ReceiptItemEditLoading() {
  return (
    <>
      <SubHeader title="재료 수정" />
      <main className={styles.main} aria-busy="true">
        <Skeleton width="100%" height="2.25rem" />
        <div className={styles.form}>
          <IngredientFormSkeleton />
        </div>
      </main>
      <BottomBar>
        <Button disabled>재료 수정</Button>
      </BottomBar>
    </>
  );
}

function ReceiptItemEditBody({
  item,
  index,
  initialDraft,
  initialSheet = null,
  initialDeleteOpen = false,
}: ReceiptItemEditContainerProps & { item: ScanItem; index: number }) {
  const router = useRouter();
  const detail = scanItemToDetail(item, index);
  const [draft, setDraft] = useState(() => initialDraft ?? toDraft(detail));
  const [isDeleteOpen, setIsDeleteOpen] = useState(initialDeleteOpen);

  const canSubmit = isDraftComplete(draft) && isDraftChanged(draft, detail);

  const handleSubmit = () => {
    const next = draftToScanItem(draft);
    if (!next) return;
    // TODO: OCR 결과 화면 상태의 index번째 품목을 next로 바꾼다(상태를 둘 곳은 API 연동 때 정함)
    router.replace(SCAN_RESULT_PATH);
  };

  const handleDelete = () => {
    // 다른 화면으로 가기 전에 먼저 닫는다(blueprint §7.1)
    setIsDeleteOpen(false);
    // TODO: OCR 결과 화면 상태에서 index번째 품목을 뺀다
    router.replace(SCAN_RESULT_PATH);
  };

  return (
    <>
      <SubHeader
        title="재료 수정"
        right={
          <IconButton
            icon={<TrashBinTrashIcon strokeWidth={1.8} />}
            iconSize={16}
            label="재료 삭제"
            onClick={() => setIsDeleteOpen(true)}
          />
        }
      />
      <main className={styles.main}>
        <IngredientRow
          name={item.name}
          imageSrc={getCategoryImage(item.categoryCode)}
          description={describeIngredient(detail)}
        />
        <div className={styles.form}>
          <IngredientForm
            draft={draft}
            onChange={setDraft}
            textSheetCopy={RECEIPT_TEXT_SHEET_COPY}
            initialSheet={initialSheet}
          />
        </div>
      </main>
      <BottomBar>
        <Button disabled={!canSubmit} onClick={handleSubmit}>
          재료 수정
        </Button>
      </BottomBar>

      {/* 피그마 p3.3.1-재료 수정-삭제 모달 */}
      <ConfirmModal
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        onConfirm={handleDelete}
        title="재료를 삭제할까요?"
        message="해당 재료를 삭제하시더라도 재료를 직접 추가하실 수 있어요. 재료를 삭제하시겠어요?"
        cancelLabel="취소"
        confirmLabel="삭제하기"
        tone="danger"
      />
    </>
  );
}
