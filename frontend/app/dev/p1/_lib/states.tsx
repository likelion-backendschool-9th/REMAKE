// p1(로그인·온보딩·설정) 화면 상태 목록. /dev/p1이 이 순서로 목록을 보이고, /dev/p1/[state]가 하나를 그린다.
// 상태를 더하면 여기에 한 줄 추가한다(blueprint §3.1 7번). 실제 라우트는 컨테이너 기본값을 쓴다.

import { LoginContainer } from "../../../public/auth/login/_components/LoginContainer/LoginContainer";
import { AllergiesContainer } from "../../../user/onboarding/allergies/_components/AllergiesContainer/AllergiesContainer";
import { HouseholdContainer } from "../../../user/onboarding/household/_components/HouseholdContainer/HouseholdContainer";
import { AccountDeletionContainer } from "../../../user/setting/account-deletion/_components/AccountDeletionContainer/AccountDeletionContainer";
import { ExcludedIngredientsContainer } from "../../../user/setting/excluded-ingredients/_components/ExcludedIngredientsContainer/ExcludedIngredientsContainer";
import { SettingHouseholdContainer } from "../../../user/setting/household/_components/SettingHouseholdContainer/SettingHouseholdContainer";
import { SettingContainer } from "../../../user/setting/profile/_components/SettingContainer/SettingContainer";
import { SubscriptionContainer } from "../../../user/setting/subscription/_components/SubscriptionContainer/SubscriptionContainer";
import type { PreviewState } from "../../_lib/preview";

export const P1_STATES: PreviewState[] = [
  {
    slug: "login",
    label: "p1-로그인",
    route: "/public/auth/login",
    render: () => <LoginContainer recentProvider={null} />,
  },
  {
    slug: "login-recent",
    label: "p1-로그인-최근 로그인 (카카오)",
    route: "/public/auth/login",
    render: () => <LoginContainer recentProvider="kakao" />,
  },
  {
    slug: "login-terms",
    label: "common-약관드로워 (이용 약관)",
    route: "/public/auth/login",
    render: () => <LoginContainer recentProvider={null} initialOpenTerms="terms" />,
  },
  {
    slug: "login-privacy",
    label: "common-약관드로워 (개인정보 처리 방침)",
    route: "/public/auth/login",
    render: () => <LoginContainer recentProvider={null} initialOpenTerms="privacy" />,
  },
  {
    slug: "onboarding-household",
    label: "p1.1-가구원 수",
    route: "/user/onboarding/household",
    render: () => <HouseholdContainer />,
  },
  {
    slug: "onboarding-household-filled",
    label: "p1.1-가구원 수 포커스 (4명 입력)",
    route: "/user/onboarding/household",
    render: () => <HouseholdContainer defaultHouseholdSize="4" />,
  },
  {
    slug: "onboarding-household-error",
    label: "p1.1-가구원 수 (범위 밖 51명, 피그마 없음)",
    route: "/user/onboarding/household",
    render: () => <HouseholdContainer defaultHouseholdSize="51" />,
  },
  {
    slug: "onboarding-allergies",
    label: "p1.2-알러지 선택 (0개)",
    route: "/user/onboarding/allergies",
    render: () => <AllergiesContainer />,
  },
  {
    slug: "onboarding-allergies-selected",
    label: "p1.2-알러지 선택 포커스 (갑각류·굴)",
    route: "/user/onboarding/allergies",
    render: () => <AllergiesContainer defaultSelected={["CRUSTACEAN", "OYSTER"]} />,
  },
  {
    slug: "setting",
    label: "p1.3-설정",
    route: "/user/setting/profile",
    render: () => <SettingContainer />,
  },
  {
    slug: "setting-logout",
    label: "p1.3-설정-로그아웃",
    route: "/user/setting/profile",
    render: () => <SettingContainer initialModal="logout" />,
  },
  {
    slug: "setting-delete",
    label: "p1.3-설정-계정 삭제",
    route: "/user/setting/profile",
    render: () => <SettingContainer initialModal="delete" />,
  },
  {
    slug: "setting-household",
    label: "p1.3.1-가구원 설정",
    route: "/user/setting/household",
    render: () => <SettingHouseholdContainer />,
  },
  {
    slug: "setting-household-filled",
    label: "p1.3.1-가구원 설정 (3명 입력, 피그마 없음)",
    route: "/user/setting/household",
    render: () => <SettingHouseholdContainer defaultHouseholdSize="3" />,
  },
  {
    slug: "setting-excluded",
    label: "p1.3.2-알러지 재료 제외 (쇠고기)",
    route: "/user/setting/excluded-ingredients",
    render: () => <ExcludedIngredientsContainer />,
  },
  {
    slug: "setting-excluded-empty",
    label: "p1.3.2-알러지 재료 제외 (0개, 피그마 없음)",
    route: "/user/setting/excluded-ingredients",
    render: () => <ExcludedIngredientsContainer defaultSelected={[]} />,
  },
  {
    slug: "account-deletion",
    label: "p1.3.3-계정 삭제",
    route: "/user/setting/account-deletion",
    render: () => <AccountDeletionContainer />,
  },
  {
    slug: "account-deletion-typing",
    label: "p1.3.3-계정 삭제-포커스 (1자리)",
    route: "/user/setting/account-deletion",
    render: () => <AccountDeletionContainer defaultDigits={["1", "", "", "", "", ""]} />,
  },
  {
    slug: "account-deletion-complete",
    label: "p1.3.3-계정 삭제-액티브 (6자리)",
    route: "/user/setting/account-deletion",
    render: () => <AccountDeletionContainer defaultDigits={["1", "2", "3", "4", "5", "6"]} />,
  },
  {
    slug: "account-deletion-failed",
    label: "p1.3.3-계정 삭제-실패 (오류 토스트 3초)",
    route: "/user/setting/account-deletion",
    render: () => (
      <AccountDeletionContainer
        defaultDigits={["1", "2", "3", "4", "5", "6"]}
        defaultErrorCode="DELETION_CODE_INVALID"
      />
    ),
  },
  {
    slug: "subscription",
    label: "p1.3.4-구독하기 (약관 동의 전)",
    route: "/user/setting/subscription",
    render: () => <SubscriptionContainer />,
  },
  {
    slug: "subscription-agreed",
    label: "p1.3.4-구독하기 (약관 동의, 피그마 없음)",
    route: "/user/setting/subscription",
    render: () => <SubscriptionContainer defaultAgreed />,
  },
  {
    slug: "subscription-terms",
    label: "p1.3.4 결제 약관 드로워",
    route: "/user/setting/subscription",
    render: () => <SubscriptionContainer initialOpenTerms />,
  },
  {
    slug: "subscription-active",
    label: "p1.3.4-구독 해지 (구독 중)",
    route: "/user/setting/subscription",
    render: () => (
      <SubscriptionContainer
        subscription={{ status: "ACTIVE", price: 9900, nextBillingDate: "2026-12-22" }}
      />
    ),
  },
  {
    slug: "subscription-cancel",
    label: "p1.3.4-구독 해지 모달",
    route: "/user/setting/subscription",
    render: () => (
      <SubscriptionContainer
        subscription={{ status: "ACTIVE", price: 9900, nextBillingDate: "2026-12-22" }}
        initialOpenCancel
      />
    ),
  },
];
