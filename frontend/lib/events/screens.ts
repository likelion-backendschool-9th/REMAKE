// 화면 코드 41개(docs/agent/spec/events.md "Screen codes"). 이벤트의 screen·screen_name·previous_screen과
// SERVER 이벤트용 X-Screen 헤더에 이 값만 쓴다. 코드를 바꾸려면 이벤트 정의 문서부터 고친다.

export const SCREENS = {
  // common
  splash: "splash",
  login: "login",
  onboardingHousehold: "onboarding_household",
  onboardingAllergy: "onboarding_allergy",
  notificationList: "notification_list",
  // settings
  settingMain: "setting_main",
  logoutModal: "logout_modal",
  settingHousehold: "setting_household",
  settingAllergy: "setting_allergy",
  accountDeleteModal: "account_delete_modal",
  accountDeleteCode: "account_delete_code",
  subscriptionManage: "subscription_manage",
  // fridge
  fridgeList: "fridge_list",
  ingredientConsume: "ingredient_consume",
  ingredientEdit: "ingredient_edit",
  ingredientAdd: "ingredient_add",
  // receipts
  receiptList: "receipt_list",
  receiptScan: "receipt_scan",
  receiptLoading: "receipt_loading",
  receiptScanError: "receipt_scan_error",
  receiptResult: "receipt_result",
  receiptItemEdit: "receipt_item_edit",
  receiptItemAdd: "receipt_item_add",
  receiptDetail: "receipt_detail",
  receiptPattern: "receipt_pattern",
  // recipes
  recipeHome: "recipe_home",
  recipeCreate: "recipe_create",
  recipeCustom: "recipe_custom",
  recipeLoading: "recipe_loading",
  recipeGenerateError: "recipe_generate_error",
  recipeResult: "recipe_result",
  recipeDetail: "recipe_detail",
  cookDeduct: "cook_deduct",
  // sharing
  sharedWrite: "shared_write",
  recipeRanking: "recipe_ranking",
  sharedDetail: "shared_detail",
  sharedReport: "shared_report",
  // meal plan
  mealPlanCalendar: "meal_plan_calendar",
  mealPlanSetup: "meal_plan_setup",
  mealPlanOverlapModal: "meal_plan_overlap_modal",
  // cart
  cartList: "cart_list",
} as const;

export type ScreenCode = (typeof SCREENS)[keyof typeof SCREENS];
