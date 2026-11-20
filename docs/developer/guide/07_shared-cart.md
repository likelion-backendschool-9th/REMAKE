# 07. 공유 공간 + 장바구니 (최유리)

## 범위

레시피 공유(완전 익명), 이번 주 레시피 랭킹(TOP 100)과 금주 TOP 3, 공유 레시피 상세·저장·신고, 주간 보상 지급, 장바구니(부족 재료 자동 담기).

## 구현·이벤트 검증 우선순위 ([#58](https://github.com/likelion-backendschool-9th/REMAKE/issues/58) 확정)

- 공유·장바구니·주간 보상 기능은 P1을 유지합니다. 이 기능들의 이벤트 발행·검증도 각 P1 기능 일정에 맞춰 진행하며, 11.8의 P0 시연 검증을 위해 기능 전체를 P0로 올리지 않습니다.
- 공유·장바구니·보상 이벤트는 `cart_item_added`, `cart_viewed`, `cart_amount_changed`, `cart_items_deleted`, `ranking_viewed`, `shared_recipe_viewed`, `shared_write_started`, `shared_recipe_published`, `shared_recipe_saved`, `shared_recipe_reported`, `weekly_reward_granted`입니다. 공유 저장에서 함께 발행하는 `recipe_saved`도 이 경로는 P1 일정에 맞춥니다.
- 이벤트 이름·속성·발행 책임은 기존 계약을 유지합니다. 04의 내 레시피 저장처럼 다른 기능도 사용하는 이벤트는 호출 경로별 일정으로 구분합니다([08의 시연 범위](08_event-tracking.md#시연-우선순위와-선행-관계-58-확정)).

## 화면 (피그마 프레임)

4.1.4-이번 주 레시피 랭킹(+스켈레톤, 최신순, 인풋 포커스), 4.1.4.1-공유 레시피 상세(+저장, 신고하기, 스켈레톤), 4.1.1.1-레시피 공유 작성(+인풋 포커스, 사진 수정, 완료), 장바구니(+항목 선택, 빈 리스트, 삭제), 모달(공유 확인 등)

## 공유 공간 규칙

- **완전 익명**: 작성자·닉네임 등 어떤 정보도 응답에 없습니다. 내 레시피/공유 공간 탭 전환은 없고, 레시피 탭 첫 화면에 금주 TOP 3 + "전체 보기"로 랭킹에 갑니다. 프론트 경로 `/recipe/community`, API 자원 `shared-recipes`.
- 공유할 수 있는 건 **Remake가 생성한 내 레시피**만. 제목 필수, **조리 사진 1장 이상 필수**(실제 조리 인증), 공유 전 확인 모달. 이미 공유한 레시피는 `ALREADY_SHARED`.
- 상세에는 "AI가 생성한 레시피" 문구, 사진, 저장 수(하트 옆 숫자, `saveCount`), 내가 저장했는지(`saved`).
- **저장하면 사본**이 내 저장 목록에 생기고(원본 참조 아님) 부족한 재료가 장바구니로 담깁니다. 본인 글 저장은 집계에서 제외. 저장 응답 `{ saveCount, cartAddedCount }`로 저장 수를 바로 갱신.
- 신고 사유 4개: `PHOTO`(사진이 이상해요), `OFFENSIVE`(불쾌·광고), `DANGEROUS`(위험·잘못된 조리법), `OTHER`. 자동 숨김 없음, 어드민(06)이 처리. 같은 사용자의 중복 신고는 `REPORT_ALREADY_EXISTS`.
- 랭킹: 인기순(기본)은 **이번 주 저장 수**로 TOP 100, 지난주 대비 순위 변동 ↑↓ 표시(`rankChange` = 지난주 순위 − 이번 순위, 없거나 같으면 null → "-"). 최신순은 순위·변동 없이 "-"와 공유 시간. 레시피 탭의 TOP 3 카드에는 순위 번호만 보이고 순위 변동은 없습니다.
- 인기순은 `saved_recipes.saved_at`으로 이번 주 저장 수를 세고, `shared_recipes.save_count`(누적)는 상세 화면 표시용으로만 씁니다.
- 지난주 순위는 매주 월요일 0시 배치가 `shared_recipe_weekly_ranks`에 지난주 최종 TOP 100을 저장해 둔 값으로 계산. **이 집계가 리셋 기준**이라 집계할 때까지 남아 있는 저장만 세고(집계 전에 저장을 취소하면 빠짐), 새 주는 0부터 셉니다. 집계·순위 저장·보상 지급은 한 트랜잭션에서 하고, 작업 중에 생긴 저장·취소는 새 주에 들어갑니다(자세한 건 [ERD](../erd.md) §24).
- **주간 보상**: 같은 배치에서 지난주 저장 수 상위 **레시피 3개**의 작성자에게 레시피 생성 횟수 지급(1위 +10, 2위 +6, 3위 +3). 보상은 레시피 기준이라 한 작성자가 여러 번 받을 수 있습니다. `generation_quota_grants`에 기록하고 **지급일로부터 한 달 안에 써야** 합니다(`expires_at`).

## 장바구니 규칙

- 직접 추가 기능은 없습니다. **식단표 생성(05)과 레시피 저장(내 레시피·공유 레시피)에서 부족한 재료가 자동으로 담깁니다.** 외부 이동·결제 없음.
- 레시피 이름별로 묶어서 보여주고 같은 재료도 합산하지 않습니다(제안). 사용자는 수량 변경, 선택 삭제(전체 선택 포함)만 가능. 삭제 모드 해제는 프론트에서.
- 등록된 재료와 같은 이름의 항목 정리(영수증 등록 후)는 정책 #29를 따릅니다.
- 공통 담기·칸 단위 삭제 계약은 [#46](https://github.com/likelion-backendschool-9th/REMAKE/issues/46)에서 확정했습니다. 아래 구현 규칙을 따릅니다.

## 장바구니 구현 규칙 (#46 확정)

두 UseCase는 `cart/application/port/in`에 두고, 04·05·공유 저장은 구현 서비스 대신 이 인터페이스로 호출합니다.

```java
// AddCartItemsUseCase
int add(Long userId, CartSource source, List<CartGroup> groups);

// RemoveCartItemsByMealPlanSlotsUseCase (반환형은 #46에서 정하지 않음)
// remove(userId, slotIds)
```

- `CartGroup`: `recipeId?`, `mealPlanId?`, `mealPlanSlotId?`, `groupName`, `items`. `?`는 NULL 허용을 뜻합니다.
- `CartItem`: `name`, `amount`(`BigDecimal`), `unit`(기존 14개 단위 코드).
- `CartSource`: `MEAL_PLAN`(식단표 생성), `RECIPE_SAVE`(내 레시피 저장), `SHARED_RECIPE_SAVE`(공유 레시피 저장).
- 식단표는 칸마다 묶음 하나를 전달합니다. `recipeId = null`, `mealPlanId`, `mealPlanSlotId`, `groupName = 메뉴명`을 채웁니다. 레시피 저장은 `recipeId`, `groupName = 레시피 제목`, `mealPlanSlotId = null`로 전달합니다.
- 반환값은 실제 추가한 `cart_items` 행 수입니다(묶음 수·수량 합계 아님). 호출 측은 이를 `cartAddedCount`로 응답합니다.
- 빈 목록과 재료가 없는 묶음은 건너뜁니다. 추가 건수가 0이면 0을 반환하고 이벤트를 발행하지 않습니다.
- 호출자의 트랜잭션에 `REQUIRED`로 참여합니다. `REQUIRES_NEW`는 사용하지 않고, 식단표·레시피 저장 실패 시 장바구니 변경도 함께 롤백합니다.
- 추가 건수가 있을 때 `cart_item_added`를 장바구니 담기 UseCase에서만 호출당 1건 발행합니다. `cart_source`와 전체 추가 행 수인 `item_count`를 기록하고, 04·05·공유 저장에서 중복 발행하지 않습니다. 롤백된 작업이 성공 이벤트로 남지 않도록 처리합니다.
- 식단표 생성의 같은 요청 재시도는 공통 `Idempotency-Key` 처리에 맡깁니다. 담기 UseCase에 별도 재시도 중복 방지를 추가하지 않습니다.

### 식단표 덮어쓰기

- `cart_items.meal_plan_slot_id`는 `meal_plan_slots`를 참조하는 nullable FK입니다. 레시피 저장 경로에서는 null이며, FK 삭제 동작은 `ON DELETE SET NULL`입니다([ERD 20번](../erd.md#20-cart_items-장바구니)).
- `RemoveCartItemsByMealPlanSlotsUseCase.remove(userId, slotIds)`는 해당 사용자의 지정된 칸에 연결된 장바구니 항목만 삭제합니다. 사용자가 수량을 수정한 항목도 삭제하며, 유지되는 칸의 항목은 보존합니다.
- 식단표는 동일 트랜잭션에서 **기존 칸의 장바구니 항목 삭제 → 기존 칸 삭제 → 새 칸 저장 → 새 장바구니 항목 추가** 순서로 호출합니다. `mealPlanId`로 장바구니 항목을 일괄 삭제하지 않습니다.
- 이 자동 정리에는 사용자 삭제 이벤트를 발행하지 않습니다. FK의 `SET NULL`은 칸 삭제 시의 동작이며, 덮어쓰기의 명시적인 UseCase 정리를 대체하지 않습니다.

## API

| 메서드 | 경로 | 비고 |
|---|---|---|
| GET | `/api/v1/user/shared-recipes/weekly-top` | TOP 3 `{ rank, id, title, ingredientChips, photoUrl }` |
| GET | `/api/v1/user/shared-recipes/ranking` | `?q=`, `?sort=popular\|latest` |
| POST | `/api/v1/user/shared-recipes` | multipart `recipeId`, `title`, `photos[]` |
| GET | `/api/v1/user/shared-recipes/{id}` | 상세 |
| DELETE | `/api/v1/user/shared-recipes/{id}` | 내 공유 내리기(화면 미결) |
| PUT·DELETE | `/api/v1/user/shared-recipes/{id}/save` | 저장·취소 |
| POST | `/api/v1/user/shared-recipes/{id}/reports` | `{ reason, detail? }` |
| GET | `/api/v1/user/cart-items` | 레시피별 묶음 |
| PATCH | `/api/v1/user/cart-items/{id}` | `{ amount }` |
| POST | `/api/v1/user/cart-items/delete` | `{ ids }` |

## 테이블

`shared_recipes`, `shared_recipe_photos`, `shared_recipe_reports`, `shared_recipe_weekly_ranks`, `generation_quota_grants`, `cart_items`.

## 에러 코드

`RECIPE_NOT_SHAREABLE`, `ALREADY_SHARED`, `PHOTO_REQUIRED`, `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, `SHARED_RECIPE_NOT_FOUND`, `REPORT_ALREADY_EXISTS`, `CART_ITEM_NOT_FOUND`, `FORBIDDEN`, `INVALID_INPUT`.

## 발행 이벤트

서버(5개): `cart_item_added`(담기 UseCase에서만, 실제 추가 건수가 있을 때 호출당 1건, `cart_source`·`item_count`), `shared_recipe_published`, `shared_recipe_saved`, `shared_recipe_reported`, `weekly_reward_granted`(+ 공유 저장 때 `recipe_saved`). 프론트: `ranking_viewed`, `shared_recipe_viewed`, `shared_write_started`, `cart_viewed`, `cart_amount_changed`, `cart_items_deleted`. 레시피·재료 이름은 속성에 넣지 않습니다.

## 체크리스트

- [ ] 익명 보장(응답·로그·이벤트에 작성자 정보 없음)
- [ ] 사진 필수·형식·용량 검증, 사진 저장소
- [ ] 이번 주 저장 수 랭킹, 순위 변동, 최신순 표시
- [ ] 저장 사본 생성·장바구니 자동 담기
- [ ] 월요일 배치(보상, 지난주 TOP 100 기록), 보상 한 달 유효
