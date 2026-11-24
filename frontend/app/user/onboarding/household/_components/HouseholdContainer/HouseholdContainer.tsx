"use client";

import { useRouter } from "next/navigation";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";

import { Button } from "@/components/Button/Button";

import { HouseholdSizeField } from "../../../../_components/HouseholdSizeField/HouseholdSizeField";
import { QuestionLayout } from "../../../../_components/QuestionLayout/QuestionLayout";
import { getHouseholdError, type HouseholdForm, householdSchema } from "../../../../_lib/household";

import styles from "./HouseholdContainer.module.css";

const FORM_ID = "onboarding-household-form";

type HouseholdContainerProps = {
  /** 처음 입력값. 미리보기용, 기본 빈칸 */
  defaultHouseholdSize?: string;
};

/**
 * p1.1 가구원 수(온보딩 1단계). 숫자 1~50만 받고, 입력 전에는 "다음"이 비활성이다.
 * 상태: 입력 전(p1.1-가구원 수) / 입력됨·포커스(p1.1-가구원 수 포커스) / 범위 밖 오류.
 */
export function HouseholdContainer({ defaultHouseholdSize = "" }: HouseholdContainerProps) {
  const router = useRouter();
  const { register, handleSubmit, control } = useForm<HouseholdForm>({
    resolver: zodResolver(householdSchema),
    defaultValues: { householdSize: defaultHouseholdSize },
  });
  const value = useWatch({ control, name: "householdSize" });
  const isValid = Boolean(value) && !getHouseholdError(value);

  const onSubmit = () => {
    // TODO: 두 단계 값을 마지막(p1.2 완료)에 한 번에 저장하므로(API 4) 가구원 수를 온보딩 상태에 보관한다
    router.push("/user/onboarding/allergies");
  };

  return (
    <QuestionLayout
      title="가구원 수를 알려주세요!"
      footer={
        <Button type="submit" form={FORM_ID} disabled={!isValid}>
          다음
        </Button>
      }
    >
      <form id={FORM_ID} className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        <HouseholdSizeField
          field={register("householdSize")}
          value={value}
          placeholder="가구원 수를 작성해 주세요"
          helperText="정확한 인원 수에 따라 레시피의 정확도가 올라갑니다"
        />
      </form>
    </QuestionLayout>
  );
}
