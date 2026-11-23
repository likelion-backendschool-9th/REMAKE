import { z } from "zod";

export const HOUSEHOLD_MIN = 1;
export const HOUSEHOLD_MAX = 50;

/** 가구원 수 입력 형식(정책: 1~50명, 숫자만). 값의 최종 검사는 서버가 한다(INVALID_INPUT). */
export const householdSchema = z.object({
  householdSize: z
    .string()
    .regex(/^\d+$/, "숫자만 입력해 주세요")
    .refine((value) => {
      const size = Number(value);
      return size >= HOUSEHOLD_MIN && size <= HOUSEHOLD_MAX;
    }, `${HOUSEHOLD_MIN}~${HOUSEHOLD_MAX}명까지 입력할 수 있어요`),
});

export type HouseholdForm = z.infer<typeof householdSchema>;
