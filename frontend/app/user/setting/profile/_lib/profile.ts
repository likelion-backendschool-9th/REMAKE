// 설정 화면 값. 모양은 API 5(GET /user/profile) 응답에서 화면이 쓰는 것만 따랐다.
// TODO: API 5를 붙이면 SAMPLE_PROFILE 대신 서버 값을 쓴다.

export type Provider = "KAKAO" | "NAVER" | "GOOGLE";

export type Profile = {
  provider: Provider;
  householdSize: number;
  excludedIngredientCount: number;
  recipeSupplementEnabled: boolean;
  notificationEnabled: boolean;
};

export const PROVIDER_LABEL: Record<Provider, string> = {
  KAKAO: "카카오 로그인",
  NAVER: "네이버 로그인",
  GOOGLE: "구글 로그인",
};

/** 마크업 확인용 값(피그마 p1.3-설정과 같음). 미리보기(app/dev)에서만 쓴다 */
export const SAMPLE_PROFILE: Profile = {
  provider: "KAKAO",
  householdSize: 3,
  excludedIngredientCount: 2,
  recipeSupplementEnabled: false,
  notificationEnabled: true,
};
