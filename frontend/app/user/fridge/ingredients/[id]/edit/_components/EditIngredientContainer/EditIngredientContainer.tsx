"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { TrashBinTrashIcon } from "@solar-icons/react/linear";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { ConfirmModal } from "@/components/ConfirmModal/ConfirmModal";
import { IconButton } from "@/components/IconButton/IconButton";
import { IngredientRow } from "@/components/IngredientRow/IngredientRow";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { getCategoryImage } from "@/lib/ingredients";

import {
  IngredientForm,
  type IngredientSheet,
} from "../../../../_components/IngredientForm/IngredientForm";
import {
  describeIngredient,
  type IngredientDetail,
  type IngredientDraft,
  isDraftChanged,
  isDraftComplete,
  toDraft,
} from "../../../../_lib/ingredient-form";

import styles from "./EditIngredientContainer.module.css";

type EditIngredientContainerProps = {
  /** 저장된 재료(API 13). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  ingredient?: IngredientDetail;
  /** 처음 입력 값. 미리보기용, 기본은 저장된 값 그대로 */
  initialDraft?: IngredientDraft;
  /** 처음부터 열어 둘 값 시트. 미리보기용 */
  initialSheet?: IngredientSheet | null;
  /** 처음부터 삭제 확인 모달을 연다. 미리보기용 */
  initialDeleteOpen?: boolean;
};

/**
 * p2.1 재료 수정. 위에 저장된 값 요약, 아래 입력 카드(재료 추가와 같음), 오른쪽 위 휴지통=삭제.
 * 바뀐 값이 있고 필수 값이 모두 있을 때만 "재료 수정" 버튼이 켜진다(API 16은 바뀐 값만 보냄).
 * 상태: 불러오는 중 / 기본 / 값을 바꿈(피그마 p2.1) / 값 시트 / 삭제 확인 모달.
 */
export function EditIngredientContainer({ ingredient, ...rest }: EditIngredientContainerProps) {
  if (!ingredient) return <EditIngredientLoading />;
  // 재료가 바뀌면 입력 값을 처음부터 다시 잡는다
  return <EditIngredientBody key={ingredient.id} ingredient={ingredient} {...rest} />;
}

/** 불러오는 중: 요약 줄과 카드 자리만 회색으로, 버튼은 막는다 */
function EditIngredientLoading() {
  return (
    <>
      <SubHeader title="재료 수정" />
      <main className={styles.main} aria-busy="true">
        <div className={styles.loadingRow}>
          <Skeleton width="2.75rem" height="1.8125rem" radius="var(--radius-xs)" />
          <Skeleton width="10rem" height="2.25rem" />
        </div>
        <div className={styles.form}>
          <Skeleton width="100%" height="25rem" radius="var(--radius-lg)" />
        </div>
      </main>
      <BottomBar>
        <Button disabled>재료 수정</Button>
      </BottomBar>
    </>
  );
}

function EditIngredientBody({
  ingredient,
  initialDraft,
  initialSheet = null,
  initialDeleteOpen = false,
}: EditIngredientContainerProps & { ingredient: IngredientDetail }) {
  const router = useRouter();
  const [draft, setDraft] = useState(() => initialDraft ?? toDraft(ingredient));
  const [isDeleteOpen, setIsDeleteOpen] = useState(initialDeleteOpen);

  const canSubmit = isDraftComplete(draft) && isDraftChanged(draft, ingredient);

  const handleSubmit = () => {
    // TODO: API 16(PATCH /user/ingredients/{id})으로 바뀐 값만 보내고 목록·상세 쿼리를 무효화한 뒤 냉장고로.
    // 사용·폐기 기록이 있으면 수량 변경은 INVALID_INPUT(fieldErrors)으로 거절된다
  };

  const handleDelete = () => {
    // 다른 화면으로 가기 전에 먼저 닫는다(blueprint §7.1)
    setIsDeleteOpen(false);
    // TODO: API 17(DELETE /user/ingredients/{id}) 뒤 목록 쿼리를 무효화한다
    router.replace("/user/fridge/ingredients");
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
          name={ingredient.name}
          imageSrc={getCategoryImage(ingredient.categoryCode)}
          description={describeIngredient(ingredient)}
        />
        <div className={styles.form}>
          <IngredientForm draft={draft} onChange={setDraft} initialSheet={initialSheet} />
        </div>
      </main>
      <BottomBar>
        <Button disabled={!canSubmit} onClick={handleSubmit}>
          재료 수정
        </Button>
      </BottomBar>

      {/* 삭제 확인 모달은 피그마에 아직 없다(domain-rules 2 "screen pending"). 문구는 다른 확인 모달처럼 설명 뒤 질문(#206 리뷰) */}
      <ConfirmModal
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        onConfirm={handleDelete}
        title="재료 삭제"
        message={`삭제한 재료는 되돌릴 수 없습니다. ${ingredient.name}을(를) 냉장고에서 삭제하시겠습니까?`}
        confirmLabel="삭제"
        tone="danger"
      />
    </>
  );
}
