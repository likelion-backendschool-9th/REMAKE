"use client";

import { useRouter } from "next/navigation";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";

import { Button } from "@/components/Button/Button";
import { TextField } from "@/components/TextField/TextField";

import { OnboardingLayout } from "../../../_components/OnboardingLayout/OnboardingLayout";
import { HOUSEHOLD_MAX, type HouseholdForm, householdSchema } from "../../_lib/schema";

import styles from "./HouseholdContainer.module.css";

const FORM_ID = "onboarding-household-form";
const HELPER_TEXT = "정확한 인원 수에 따라 레시피의 정확도가 올라갑니다";

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
    mode: "onChange",
  });
  const value = useWatch({ control, name: "householdSize" });
  const field = register("householdSize");
  // 입력한 값으로 바로 검사한다(빈칸이면 오류 없이 "다음"만 비활성). 미리보기에서 넘긴 처음 값도 같은 방식으로 보인다
  const errorMessage = value
    ? householdSchema.safeParse({ householdSize: value }).error?.issues[0]?.message
    : undefined;
  const isValid = Boolean(value) && !errorMessage;

  const onSubmit = () => {
    // TODO: 두 단계 값을 마지막(p1.2 완료)에 한 번에 저장하므로(API 4) 가구원 수를 온보딩 상태에 보관한다
    router.push("/user/onboarding/allergies");
  };

  return (
    <OnboardingLayout
      title="가구원 수를 알려주세요!"
      footer={
        <Button type="submit" form={FORM_ID} disabled={!isValid}>
          다음
        </Button>
      }
    >
      <form id={FORM_ID} className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          label="가구원 수"
          placeholder="가구원 수를 작성해 주세요"
          inputMode="numeric"
          autoComplete="off"
          maxLength={String(HOUSEHOLD_MAX).length}
          unit={value ? "명" : undefined}
          helperText={HELPER_TEXT}
          errorMessage={errorMessage}
          {...field}
          onChange={(event) => {
            // 숫자가 아닌 글자는 입력하는 즉시 지운다(정책: 숫자만)
            event.target.value = event.target.value.replace(/\D/g, "");
            return field.onChange(event);
          }}
        />
      </form>
    </OnboardingLayout>
  );
}
