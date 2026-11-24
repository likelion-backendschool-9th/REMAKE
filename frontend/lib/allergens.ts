// 제외 재료(알레르기) 칩 19개. ERD 부록 B·피그마 p1.2 순서. 코드는 API allergenCodes와 같다.
// 온보딩(p1.2)과 설정 > 알레르기 재료 제외(p1.3.2)에서 쓴다.

export const ALLERGENS = [
  { code: "EGG", name: "난류" },
  { code: "MILK", name: "우유" },
  { code: "BUCKWHEAT", name: "메밀" },
  { code: "PEANUT", name: "땅콩" },
  { code: "SOYBEAN", name: "대두" },
  { code: "WHEAT", name: "밀" },
  { code: "MACKEREL", name: "고등어" },
  { code: "CRUSTACEAN", name: "갑각류" },
  { code: "PORK", name: "돼지고기" },
  { code: "SHRIMP", name: "새우" },
  { code: "SULFITE", name: "아황산류" },
  { code: "PEACH", name: "복숭아" },
  { code: "SQUID", name: "오징어" },
  { code: "CHICKEN", name: "닭고기" },
  { code: "BEEF", name: "쇠고기" },
  { code: "OYSTER", name: "굴" },
  { code: "CRAB", name: "게" },
  { code: "PINE_NUT", name: "잣" },
  { code: "SHELLFISH", name: "어패류" },
] as const;

export type AllergenCode = (typeof ALLERGENS)[number]["code"];
