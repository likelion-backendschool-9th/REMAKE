"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";

import { Button } from "@/components/Button/Button";

import { HouseholdSizeField } from "../../../../_components/HouseholdSizeField/HouseholdSizeField";
import { QuestionLayout } from "../../../../_components/QuestionLayout/QuestionLayout";
import { getHouseholdError, type HouseholdForm, householdSchema } from "../../../../_lib/household";

import styles from "./SettingHouseholdContainer.module.css";

const FORM_ID = "setting-household-form";

type SettingHouseholdContainerProps = {
  /** 처음 입력값. 미리보기용, 기본 빈칸(피그마 p1.3.1) */
  defaultHouseholdSize?: string;
};

/**
 * p1.3.1 가구원 설정. 숫자 1~50을 넣으면 버튼이 "가구원 N명 저장"으로 바뀌고 활성이 된다(입력 전은 비활성 "저장").
 * 상태: 입력 전(p1.3.1-가구원 설정) / 입력됨 / 범위 밖 오류.
 */
export function SettingHouseholdContainer({
  defaultHouseholdSize = "",
}: SettingHouseholdContainerProps) {
  const { register, handleSubmit, control } = useForm<HouseholdForm>({
    resolver: zodResolver(householdSchema),
    defaultValues: { householdSize: defaultHouseholdSize },
  });
  const value = useWatch({ control, name: "householdSize" });
  const isValid = Boolean(value) && !getHouseholdError(value);

  const onSubmit = () => {
    // TODO: API 6(PATCH /user/profile { householdSize })으로 저장하고 E14 setting_changed를 보낸 뒤 설정으로
  };

  return (
    <QuestionLayout
      title="가구원 설정을 해주세요!"
      footer={
        <Button type="submit" form={FORM_ID} disabled={!isValid}>
          {/* 값이 맞을 때만 "가구원 N명 저장", 입력 전·범위 밖은 "저장"(피그마는 입력 전에도 "가구원 N명 저장") */}
          {isValid ? `가구원 ${Number(value)}명 저장` : "저장"}
        </Button>
      }
    >
      <form id={FORM_ID} className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        <HouseholdSizeField
          field={register("householdSize")}
          value={value}
          placeholder="숫자만 작성 가능합니다."
        />
      </form>
    </QuestionLayout>
  );
}
