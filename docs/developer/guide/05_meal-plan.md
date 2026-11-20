# 05. 식단표 (한윤수)

## 범위

월 달력 식단표 생성·조회, 식단 추가, 칸 상세 레시피 지연 생성, 부족 재료 계산과 장바구니 연결.

## 화면 (피그마 프레임)

식단표-없을 경우, 식단표, 식단표-식단 레시피 설정(2개 상태, 끼니 선택 포함)

## 화면 규칙

- 월 달력. 날짜를 누르면 그날 점심·저녁(설정에 따라 아침 포함) 칸을 보여줍니다. 지난 달·지난 날짜도 조회.
- 식단이 없으면 생성 안내 화면. 생성 설정: **시작일, 식단 목적 1개, 넣을 끼니, 식사 인원**, "이번 달 남은 N회" 표시(`GET /user/usage-quotas`의 `mealPlan`, 식단표 부분은 이 영역이 만듦).
- 식단 목적 5개: `BALANCED`(균형 잡힌, 기본), `DIET`, `HIGH_PROTEIN`, `SIMPLE`, `SAVING`. 생성 직전에 고르고 마지막 선택을 기억(`profile.lastMealPurpose`). 의료 목적·칼로리 목표는 없고 "참고용 식단" 문구를 보여줍니다.
- 칸을 누르면 바로 레시피 상세. **칸 메뉴 교체·칸 재생성·식단 삭제는 없습니다.**
- **식단 추가는 필수**: 빈 끼니에 저장한 레시피를 넣습니다(AI 재생성 없음, 방식은 제안).
- 재료가 부족하면 7일을 채우지 않고 **만들 수 있는 일수까지만** 생성하고 "재료가 부족해 N일치만 만들었어요" 안내 → 부족한 재료는 장바구니로.

## API

| 메서드 | 경로 | 비고 |
|---|---|---|
| POST | `/api/v1/user/meal-plans/generate` | `{ startDate, purpose, mealTypes, servings, overwrite? }` → `{ mealPlanId, generatedDays, shortage[], cartAddedCount }` |
| GET | `/api/v1/user/meal-plans/calendar` | `?month=YYYY-MM` 날짜별 끼니 요약(점 표시) |
| GET | `/api/v1/user/meal-plans/days/{date}` | 그날 칸 목록 |
| POST | `/api/v1/user/meal-plan-slots` | 식단 추가 `{ date, mealType, recipeId }` |
| POST | `/api/v1/user/meal-plan-slots/{slotId}/detail` | 메뉴명만 있는 칸의 상세 레시피 생성 → `recipeId` |

## 테이블

`meal_plans`(생성 한 번), `meal_plan_slots`(`source` AI/MANUAL), `meal_plan_slot_ingredients`(칸별 예정 재료·수량).

## 구현 규칙

- **가장 어려운 부분: 재료 수량 중복 배정 방지.** 같은 재료를 여러 칸에 쓰면 가진 양을 넘기지 않게 합니다. `meal_plan_slot_ingredients`에 칸별 재료·수량을 저장하고 SQL `SUM`으로 "냉장고 재고 − 이미 배정된 양"을 계산합니다. 단순한 규칙(소비기한 임박 재료 우선)부터 만들고, 부족하면 일수를 줄이는 부분 생성으로 보완하세요.
- **2단계 생성**: ① 메뉴명과 칸별 재료만 먼저 생성(목표 30초 이내) ② 칸을 눌렀을 때 상세 레시피를 생성(`slots/{id}/detail`). 상세 생성은 레시피 하루 2회에서 차감하지 않는 안(제안).
- 횟수: 기본 월 1회·최대 5일(구독 월 4회·7일). 성공했을 때만 차감하고 실패는 차감 없음. `usage_logs`로 동시 클릭 방지(04와 같은 방식). 한도 초과는 `MEAL_PLAN_LIMIT_EXCEEDED`와 `quota_exceeded` 이벤트.
- 같은 기간에 이미 식단표가 있으면 `MEAL_PLAN_OVERLAP`(`overwrite`로 덮어쓰기 가능). 재료로 하루도 못 채우면 `MEAL_PLAN_NO_INGREDIENTS`(422).
- 식단 추가: 이미 찬 칸은 `SLOT_ALREADY_EXISTS`, 지난 날짜는 `PAST_DATE_NOT_ALLOWED`, 레시피가 없으면 `RECIPE_NOT_FOUND`.
- 알레르기(제외 재료)와 가구원 수·식사 인원을 생성에 반영. AI 입력은 id·enum만(자유 입력 금지), 응답 검증은 04와 같은 클라이언트·검증기를 재사용.
- 부족한 재료를 `cart_items`에 넣는 일은 최유리(07)의 서비스를 호출합니다(레시피별 묶음, 합산하지 않음).
- 프론트는 60초 타임아웃과 생성 중 화면이 필요합니다(진행 표시 방식은 미결).

## 에러 코드

`MEAL_PLAN_LIMIT_EXCEEDED`, `MEAL_PLAN_NO_INGREDIENTS`, `MEAL_PLAN_OVERLAP`, `SLOT_ALREADY_EXISTS`, `SLOT_NOT_FOUND`, `PAST_DATE_NOT_ALLOWED`, `RECIPE_NOT_FOUND`, `AI_UPSTREAM_ERROR`, `AI_TIMEOUT`, `AI_INVALID_RESPONSE`, `INVALID_INPUT`.

## 발행 이벤트

서버(5개): `meal_plan_generated`, `meal_plan_generate_failed`, `meal_slot_detail_generated`, `meal_slot_detail_failed`, `meal_added`(+ `quota_exceeded`). 부족 재료를 장바구니에 담을 때의 `cart_item_added`는 장바구니 담기 UseCase(07)가 보내므로 여기서는 보내지 않습니다. 프론트: `meal_plan_setup_viewed`, `meal_plan_generate_started`, `meal_plan_shortage_shown`, `meal_calendar_viewed`, `meal_day_viewed`, `meal_slot_opened`.

## 체크리스트

- [ ] 재료 중복 배정 방지와 부분 생성(일수 줄이기) 테스트
- [ ] 목적 5개·끼니·인원 반영, 마지막 목적 기억
- [ ] 월 달력 조회(지난 달 포함), 칸 → 레시피 상세
- [ ] 식단 추가(빈 끼니), 삭제·재생성 없음
- [ ] 월 한도·실패 시 차감 없음
