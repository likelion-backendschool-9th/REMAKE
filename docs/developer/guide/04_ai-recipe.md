# 04. AI 레시피 (김종수)

기준: PRD v4·정책정의서 v4·ERD v4·API 정의서 v4·에러 정의서 v2·이벤트 정의서 v2(모두 피그마 기준, 2026-11-22, 노션). 표는 원본에서 그대로 옮겼고 피그마 노드 근거 칸만 뺐어요.

## 범위

빠른·커스텀 레시피 생성, 다시 받기, 레시피 상세, 저장·즐겨찾기, 요리 완료와 재료 차감, 생성 횟수(남은 횟수·차감·보상 횟수 사용).

## 화면

화면 코드는 이벤트의 `screen` 값이에요. "추가될 페이지"는 아직 피그마에 없는 화면이에요.

| 화면 코드 | 피그마 화면 |
| --- | --- |
| `recipe_home` | p4.1 레시피(금주의 레시피 + 저장한 레시피) |
| `recipe_create` | p4.1 빠른 레시피 생성 모달 |
| `recipe_custom` | p4.1.2 커스텀 레시피 생성 |
| `recipe_loading` | AI 생성 중 로딩(추가될 페이지 14번) |
| `recipe_generate_error` | 생성 실패·횟수 소진 화면(추가될 페이지 8·12번) |
| `recipe_result` | p4.1.3 레시피 생성 결과 |
| `recipe_detail` | p4.1.1 레시피 상세 |
| `cook_deduct` | p4.1.1.2 레시피 사용 후 재료 차감 |

## 정책

상태: 확정 = 그대로 구현, 제안 = 상수로 분리해 구현하고 PR에 적기, 미결 = 구현 전에 준호님께 질문, 화면 대기 = 규칙은 정해졌고 화면이 추가될 예정.

| 항목 | 규칙 | 상태 |
| --- | --- | --- |
| 두 가지 방식 | 빠른 레시피(냉장고 재료·가구원 수 기준, 소비기한 임박 재료 우선)와 커스텀 레시피(취향 기준) | 확정 |
| 빠른 생성 모달 | "가구원 수와 냉장고의 재료에 맞게 레시피를 생성 합니다. 레시피를 생성하면 하루 생성 횟수를 차감 합니다" → 생성 하기 | 확정(피그마) |
| 커스텀 입력 | 식사 인원(필수) + 재료 선택(내 냉장고 재료)·원하는 맛·조리 시간·조리 도구·조리 방식·요리 유형·알레르기 재료. 시트마다 하나만 고름(선택 전 확인 비활성). 자유 입력 없음 | 확정 |
| 커스텀 식사 인원 기본값 | 설정의 가구원 수(피그마 예시는 1인) | 제안 |
| 응답 방식 | 스트리밍 없이 한 번에. 생성 중 로딩 화면(최대 60초) | 확정 / 로딩은 화면 대기 |
| 재료 보충 OFF | 내 재료로만 만들고, 너무 적으면(냉장고가 빈 경우 포함) NO_INGREDIENTS → 생성 실패 화면 "설정에서 레시피 재료 보충을 켜 보세요". 차감 없음 | 확정 / 화면 대기 |
| 재료 보충 ON | 냉장고에 없는 재료도 넣어 만듦 | 확정 |
| AI 실패 | AI 오류·시간 초과·형식 검증 실패는 1회 자동 재시도 후 실패, 생성 실패 화면에 문구만 다르게, 차감 없음 | 확정 |
| 단위 검증 | AI에게 정해진 단위 코드만 쓰게 하고, 목록에 없는 단위는 서버가 거름 | 확정 |
| AI 표기 | "AI가 생성한 레시피" 문구는 넣지 않음(피그마 레시피 상세·공유 상세대로). AI 결과 면책은 이용 약관 제5조에만 둠(11/24 약관 결정) | 확정 |
| 생성 결과 | 레시피명, 북마크(저장 전 빈 / 저장 후 채움), 재료 준비·인분·시간, 재료, 만드는 방법·재생하기, 단계, Tips!, 레시피 다시 받기 N회, 요리 완료 | 확정(피그마) |
| 다시 받기 | 언제든 누를 수 있음(색만 흐림). 누르면 하루 횟수 차감 | 확정 |
| 저장하지 않은 레시피 | 생성하면 서버에 남지만 저장 전에는 목록에 보이지 않음 | 확정 |
| 저장 | 생성 결과 북마크로 저장. 재료 보충으로 부족 재료가 들어간 레시피는 저장할 때 그 재료를 장바구니에 담고 개수 토스트 | 확정 / 토스트는 화면 대기 |
| 저장한 레시피 목록 | "저장한 레시피 N", 필터 칩 전체·즐겨찾기(즐겨찾기한 것만)·바로 가능(지금 냉장고 재료로 만들 수 있는 것만), 정렬 최신 저장순(기본)·오래된 순(정렬 시트 추가 예정) | 확정 / 정렬 시트는 화면 대기 |
| 즐겨찾기 | 저장 목록 행의 별, 레시피 상세 ☆, 식단 끼니 행 ★로 켜고 끔. 저장을 취소하면 같이 사라짐 | 확정 |
| 저장 개수 한도 | 없음 | 제안 |
| 레시피 상세 | 레시피명, ☆, ⋮, "재료 준비 · N인분 · N분 이내", 재료(이름+양), 만드는 방법(단계 번호), Tips!, 요리 완료 | 확정(피그마) |
| 재생하기 | "만드는 방법 재생하기"를 누르면 기기 음성으로 단계를 하나씩 읽어 줌, 재생 중에는 "중단하기" | 확정 |
| 재료 보유 표시 | 내 레시피 상세에는 보유 표시 없음. 공유 레시피 상세는 내게 없는 재료를 빨간 테두리로 | 확정 |
| ⋮ 삭제 하기 | 저장 목록에서만 뺌. 공유한 글과 다른 사람이 저장한 것은 그대로 | 확정 |
| ⋮ 공유 하기 | 공유 작성으로 이동. 그 주(월요일 시작)에 이미 공유한 레시피는 비활성 | 확정 |
| 요리 완료 | 상세·생성 결과의 "요리 완료" → 재료 차감 화면. 요리 기록은 테이블 없이 이벤트(cook_completed)로 남김 | 확정 |
| 재료 차감 화면 | "총 N개의 재료를 차감해 주세요", 내 냉장고에 있는 레시피 재료를 체크, 행 > 로 수량 변경 시트, "N개의 재료 차감 하기"(N = 체크 개수). 냉장고에 있는 재료가 없으면 빈 화면(추가 예정) | 확정(피그마) |
| 단위 환산 | 같은 종류 단위끼리만 자동 환산, 종류가 다르면 사용자가 사용량 확인, 꼬집·약간은 차감 안 함 | 확정 |
| 식단에서 요리 | 오늘 식단 칸에서 연 레시피로 요리 완료하면 그 칸을 요리함으로 표시(다른 날 칸은 안 함) | 확정 |
| 레시피 추천 여부 | 재료 차감 뒤 냉장고(p2)로 이동해 바텀 시트 "이 레시피 어땠나요?"(추천해요 / 추천하지 않아요 → 보내기). 기능에 쓰지 않는 추적 데이터(성공률 확인·고도화용)라 이벤트 E50으로만 수집, 새 API·테이블 없음 | 확정(11/22) |

## API

경로의 `/api/v1`은 줄였어요. 요청의 `?`는 선택 값, `|`는 그중 하나예요. 번호는 API 정의서 번호예요(다른 문서도 이 번호로 가리켜요).

| 번호 | 메서드 | 경로 | 설명 | 요청 | 응답 | 관련 테이블 |
| --- | --- | --- | --- | --- | --- | --- |
| 27 | GET | `/user/usage-quotas` | 남은 횟수. 레시피는 하루(일반 2회·구독 4회 + 보상), 식단표는 이번 달(일반 1회·구독 4회) | `-` | `200 { recipe: { limit, used, bonusRemaining, remaining }, mealPlan: { limit, used, remaining, maxDays } }` | usage_logs, generation_quota_grants, users |
| 28 | POST | `/user/recipes/generate` | 레시피 생성(한 번에 응답). 성공할 때만 하루 횟수 차감. 빠른 생성 인원은 가구원 수. 커스텀 항목은 하나씩(11/21 답 23). 재료 보충이 꺼져 있으면 내 냉장고 재료로만 만들고, 재료가 너무 부족하면 실패(횟수 차감 없음, 실패 화면은 추가될 페이지 8번). 켜져 있으면 부족한 재료를 더해 만듦(API 답 2) | `빠른 { mode: "QUICK" } / 커스텀 { mode: "CUSTOM", servings, ingredientIds: [], taste?, cookTime?, tool?, method?, cuisine?, allergenCodes: [] }` | `201 (레시피 상세 모양)` | recipes, recipe_ingredients, recipe_steps, usage_logs |
| 29 | POST | `/user/recipes/{id}/regenerate` | 같은 조건으로 새 레시피. 언제든 누를 수 있고 하루 횟수 차감(11/21 답 18) | `본문 없음` | `201 (레시피 상세 모양)` | recipes, usage_logs |
| 30 | GET | `/user/recipes/{id}` | 레시피 상세. "AI가 생성한 레시피" 문구는 넣지 않음(PRD 답 2). 공유 가능 여부는 그 주에 공유했는지(11/21 답 17) | `-` | `200 { id, title, servings, cookMinutes, tips, ingredients: [{ name, amount, unit }], steps: [{ stepNo, content }], saved, favorite, shareable }` | recipes, recipe_ingredients, recipe_steps, saved_recipes, shared_recipes |
| 31 | GET | `/user/recipes/{id}/cook-preview` | 차감할 내 냉장고 재료와 레시피 양 | `?mealPlanSlotId=(식단 칸에서 열었으면)` | `200 { items: [{ ingredientId, name, imageUrl, amount, unit, remainingAmount }] }` | recipe_ingredients, ingredients |
| 32 | POST | `/user/recipes/{id}/cook` | 체크한 재료를 USED로 차감. 오늘 식단 칸에서 열었으면 그 칸에 요리 완료 표시(ERD 답 8). 차감 뒤 냉장고(p2)로 이동해 추천 여부 바텀 시트를 띄움(답은 E50 이벤트로만) | `{ items: [{ ingredientId, amount }], mealPlanSlotId? }` | `200 { deductedCount }` | ingredient_logs, ingredients, meal_plan_slots |
| 33 | GET | `/user/saved-recipes` | 저장 목록. 바로 가능 = 지금 냉장고 재료로 만들 수 있는 것(11/21 답 15) | `?filter=ALL\|FAVORITE\|AVAILABLE &sort=SAVED_DESC\|SAVED_ASC(최신 저장순·오래된 순, 선택 화면은 추가될 페이지 9번) &page=0&size=20` | `200 { totalCount, content: [{ recipeId, title, ingredientSummary, extraIngredientCount, favorite }], page, size, totalElements, hasNext }` | saved_recipes, recipes, recipe_ingredients, ingredients |
| 34 | PUT | `/user/saved-recipes/{recipeId}` | 내 레시피 저장(이미 저장돼 있어도 같은 결과). 재료 보충으로 들어간 부족 재료는 장바구니에 자동으로 담김(API 답 2) | `본문 없음` | `200 { saved: true, cartAddedCount }` | saved_recipes, cart_items |
| 35 | DELETE | `/user/saved-recipes/{recipeId}` | 저장 목록에서만 뺌. 공유 글은 그대로(ERD 답 6) | `-` | `204` | saved_recipes |
| 36 | PUT | `/user/saved-recipes/{recipeId}/favorite` | 즐겨찾기 켜기 | `본문 없음` | `200 { favorite: true }` | saved_recipes |
| 37 | DELETE | `/user/saved-recipes/{recipeId}/favorite` | 즐겨찾기 끄기(저장은 유지) | `-` | `200 { favorite: false }` | saved_recipes |

커스텀 선택값 코드: taste MILD/SPICY/SWEET/SALTY/SOUR/NUTTY, cookTime ANY/WITHIN_15/WITHIN_30/WITHIN_60, tool FRYING_PAN/POT/MICROWAVE/AIR_FRYER/OVEN/RICE_COOKER, method STIR_FRY/GRILL/STEAM/BRAISE/SOUP_STEW/SEASONED/DEEP_FRY, cuisine KOREAN/WESTERN/CHINESE/JAPANESE/BUNSIK/SIMPLE.

## 테이블

`recipes`, `recipe_ingredients`, `recipe_steps`, `saved_recipes`, `usage_logs`. 차감은 `ingredients`·`ingredient_logs`(02), 보상 횟수는 `generation_quota_grants`(07). 컬럼은 [`../erd.md`](../erd.md)를 보세요.

## 구현 메모

- 장바구니 연동은 #57의 확정 계약을 적용해요. 내 레시피 저장 취소는 저장 목록에서만 제거하고 원본 레시피·장바구니 행·수정 수량을 유지해요. 취소 후 다시 저장하면 현재 부족한 재료 중 같은 사용자·원본 recipeId·정확한 재료명·단위의 장바구니에 없는 재료만 추가해요. 이미 저장된 PUT 재시도는 멱등성을 유지하며 새 담기를 실행하지 않아요. 추가 개수는 실제 새 행 수이고, 0행이면 E62를 발행하지 않아요. 수량 없는 재료(`amount=null`, PINCH, TO_TASTE)는 내 레시피·공유 레시피 저장 시 장바구니에서 제외하고 레시피에는 그대로 표시해요(PR #216 담당자 확정). 상세 계약과 미결 단위 정책은 [장바구니 가이드](07_shared-cart.md#erd와-맞춰야-하는-장바구니-계약57)를 보세요.
- AI에 자유 입력 금지. 생성은 고른 옵션(id·enum)만 받고 사용자가 쓴 글을 프롬프트에 넣지 않아요. 설정의 제외 재료(알레르기)는 생성에 항상 반영해서, 만든 레시피에 그 재료가 들어가지 않아요.
- 응답은 스트리밍 없이 한 번에(최대 60초). AI 오류·시간 초과·형식 실패는 1회 자동 재시도 후 실패예요.
- 횟수: 빠른·커스텀 생성, 다시 받기, 식단 레시피 교체(05)가 하루 횟수를 같이 써요. 검증을 통과해 저장한 직후 차감하고, 차감과 결과 저장은 한 트랜잭션이에요. 기본 횟수를 먼저 쓰고, 다 쓰면 보상 횟수를 만료가 가까운 것부터 써요.
- 실패(AI 오류·시간 초과·형식 실패·`NO_INGREDIENTS`)는 차감하지 않고 `usage_logs`에도 남기지 않아요. 실패 기록은 이벤트가 유일해요.
- 재료 보충 OFF면 내 냉장고 재료로만 만들고, 너무 적으면 `NO_INGREDIENTS`. ON이면 냉장고에 없는 재료도 넣고, 저장할 때 그 재료를 장바구니에 담아요(07의 `AddCartItemsUseCase`).
- AI 응답 단위는 14개 코드만 허용하고 나머지는 서버가 걸러요. "AI가 생성한 레시피" 문구는 넣지 않아요.
- 생성한 레시피는 저장 전에도 서버에 남지만 목록에는 안 보여요. 레시피는 만든 뒤 고치지 않아요.
- 저장 목록 필터 `AVAILABLE`(바로 가능)은 지금 냉장고 재료로 만들 수 있는 레시피예요. 물을 뺀 모든 재료가 내 냉장고에 같은 이름으로 있어야 하고(소비기한 지난 재료는 없는 것으로 봄), 양은 단위 환산(#63) 전이라 아직 비교하지 않아요(레시피 영역 담당 결정).
- 목록 한 줄의 재료 요약은 레시피 재료 순서대로 앞 3개를 `" · "`로 이은 `ingredientSummary`와 나머지 개수 `extraIngredientCount`(화면은 "+N", 0이면 생략)예요. 물·꼬집·약간도 재료 줄이면 그대로 세요. 저장 목록(33)·식단표(40·42)·공유(49·50)가 같은 규칙을 쓰고, 코드는 `IngredientSummary`예요(피그마 p4.1·p4.1.4 기준, 레시피 영역 담당 결정).
- 요리 완료: `cook-preview`로 차감할 내 재료를 보여 주고 `cook`에서 체크한 재료를 USED로 빼요. 같은 종류 단위끼리만 환산하고, 꼬집·약간은 빼지 않아요. 오늘 식단 칸에서 열었으면 그 칸 `cooked_at`을 기록해요. 요리 기록 테이블은 없고 `cook_completed` 이벤트가 기록이에요.
- 요리 미리보기(31)는 레시피 재료마다 같은 이름이고 소비기한이 지나지 않은 내 냉장고 재료 중 소비기한이 가장 빠른 것 하나와 짝지어요("바로 가능"과 같은 이름 비교라 저장한 공유 레시피도 돼요). 물·꼬집·약간·양 없는 재료는 빠져요. 차감 예정량은 냉장고 단위로 바꾸고 남은 양을 넘지 않아요. 단위 종류가 달라 바꿀 수 없으면(양파 1개 ↔ 300g, 영수증에서 `PACK`으로 들어온 재고 ↔ g, #145) `amount`가 null이고 사용자가 직접 정해요. 같은 냉장고 재료에 레시피 두 줄이 걸리면 양을 더해요(레시피 영역 담당 결정).
- 요리 완료(32)는 체크한 재료만 냉장고 영역의 `ConsumeIngredientUseCase`(via `COOK`)로 빼요. 이 UseCase가 E25를 남기고 다 쓴 재료 행을 지워요. 하나라도 실패하면 모두 되돌리고, 빈 목록이면 차감 없이 요리 완료만 남겨요. 내 재료가 아니거나 양이 0 이하이거나 같은 재료가 두 번 오면 `COOK_ITEM_INVALID`, 남은 양보다 많으면 `INSUFFICIENT_QUANTITY`예요. 식단 칸(`mealPlanSlotId`)은 잠그고 읽어서 오늘 칸일 때만 `cooked_at`을 남겨요(05의 `MarkMealSlotCookedUseCase`). 레시피를 연 뒤 칸이 지워졌거나(삭제·덮어쓰기) 다른 레시피로 바뀌었으면(교체) 칸 표시만 건너뛰고 요리 완료는 성공해요. 사용자는 실제로 요리했으니까요(레시피 영역 담당 결정, #254·#262·#268). E49 `adjusted_count`는 미리보기 양과 다르게 뺀 재료 수(미리보기가 양을 못 정한 재료 포함)예요.
- 단위 환산은 `recipe/domain`의 `UnitConversion` 한 곳에서만 해요(무게↔무게, 부피↔부피, 큰술 15·작은술 5·컵 200ml, 개수 단위는 같은 단위끼리만, 소수 셋째 자리 반올림). 식단표 배정(`StockUnits`)과 장바구니 → 냉장고(`CartFridgeQuantity`)도 이 표를 써요.
- 동시 생성 잠금(회원 단위)과 횟수 초기화 시각(00:00)은 제안이라 상수로 두세요.

## 지금 코드와 다른 점

- 레시피 테이블(V7: `recipes`, `recipe_ingredients`, `recipe_steps`, `usage_logs`), 레시피 도메인·저장 어댑터, AI 포트와 응답 검증기, Gemini 어댑터(`gemini-3.5-flash-lite`)가 있어요(#74). 남은 횟수(27), 생성(28), 다시 받기(29), 상세(30) API가 있어요(#75). 저장 목록·저장·저장 취소·즐겨찾기(33~37)가 있어요(#76). `saved_recipes` 테이블은 공유(#214)의 V13이 만들고, 목록용 인덱스는 V16에 있어요. 요리 후 차감 미리보기·요리 완료(31·32)와 단위 환산 공용 클래스 `UnitConversion`이 있어요(#77, #63). 레시피 재료 카테고리(`recipe_ingredients.category_code`, V28)와 저장 때 장바구니 담기가 있어요(#76). AI 연결 규칙은 [백엔드 컨벤션 "AI 생성"](../backend-convention.md#ai-생성)에 있어요.
- 재료 보충 OFF의 "너무 적으면"은 양념(SEASONING)을 뺀 재료 2개 미만으로 정했어요(레시피 영역 담당 결정, 소비기한 지난 재료는 세지 않음). 이때는 AI를 부르지 않아요.
- 보상 횟수(`generation_quota_grants`)는 #92에서 연결했어요. 만료 전 잔여 횟수를 조회하고 기본 횟수 소진 후 만료 시각·ID 순으로 차감해요. 알레르기는 설정의 제외 재료를 빠른 생성·커스텀·다시 받기 모두에 항상 넣고, 커스텀은 고른 칩을 더해요. 칩 코드는 `AllergenCode`(ERD 부록 B)만 받고 모르는 코드는 `400 INVALID_INPUT`이에요.
- 다시 받기: 커스텀은 저장해 둔 선택값을 다시 쓰고 그 사이 없어진 재료는 빼요. 빠른 생성은 지금의 냉장고·가구원 수, 재료 보충은 지금 설정을 써요. 식단표 레시피는 다시 받기 대신 식단 칸 교체(05)라서 400이에요.
- 상세(30)는 내가 만들었거나 저장한 레시피(저장한 공유 레시피 포함)만 열려요. `shareable`은 내가 만든 레시피이고 이번 주(월요일 시작)에 아직 공유하지 않았을 때만 true예요. 저장한 남의 레시피는 false예요. 공유 쪽 `CheckRecipeSharedThisWeekUseCase`를 쓰고, 게시(51)의 `ALREADY_SHARED`와 같은 주 기준(`SharedRecipe.weekStart`)이에요.
- 저장(34)은 내가 만든 레시피만 돼요(공유 레시피 저장은 07의 API 53). 처음 저장할 때(저장 취소 뒤 다시 저장 포함) 부족 재료를 07의 `AddCartItemsUseCase`로 장바구니에 담아요. 부족 재료는 재료 보충으로 들어간 줄(냉장고 재료 id 없음) 중 지금 내 냉장고에 소비기한 안의 같은 이름 재료가 없는 것이에요("바로 가능"과 같은 기준). 장바구니에 이미 있는 재료는 담지 않고(#57), 물·꼬집·약간·양 없는 재료는 담기 쪽이 빼요. 카테고리를 정할 수 없는 줄(이 기능 전에 만든 레시피이고 사전에도 없는 이름)과 50자를 넘는 이름은 담지 않고 저장은 계속해요. `cartAddedCount`와 E44 `cart_added_count`는 실제로 담은 수예요. 저장 취소·즐겨찾기는 저장 목록에 있는 레시피면 되고, 볼 수 있지만 저장하지 않았으면 `SAVED_RECIPE_NOT_FOUND`예요.
- 식단표 하루 조회·칸 교체(05)는 `GetRecipeListItemsUseCase`로 제목·재료 요약·즐겨찾기를 레시피 id 목록으로 한 번에 읽어요.
- 로컬은 `application-local.yml`의 `remake.quota.enforced=false`라 횟수 한도에 막히지 않아요(기록은 남음). 한도를 확인하려면 `backend/.env`에 `QUOTA_ENFORCED=true`를 넣어요. Gemini 키는 각자 자기 무료 키를 `backend/.env`에 넣어요.
- 이벤트 속성 값은 이벤트 모듈 규칙대로 대문자예요(`recipe_mode`: `QUICK`·`CUSTOM`, `limit_feature`: `RECIPE`). 이벤트 정의서 예시의 소문자와 달라요.
- 기본 양념(#158, 문서 #345): **소금·식용유·후추·설탕**은 냉장고에 없어도 “있음”으로 봐요(재료 보충 OFF·바로 가능·부족 판정 포함). **물은 이 목록에 넣지 않아요**(물은 기존처럼 부족으로 치지 않을 수 있으나 기본 양념 목록과는 별개). 꼬집·약간으로만 쓰는 다른 양념은 목록에 없으면 냉장고에 있어야 해요. 요리 완료 차감은 냉장고에 **실제 등록된** 재료만 후보예요.
- 이전 가이드의 알레르기 경고 표시, 피드백 저장, "AI가 생성한 레시피" 문구, `Idempotency-Key`는 없어졌어요.
- 공유 레시피를 저장하면 사본을 만들지 않고 원본을 가리켜요(`saved_recipes`).

## 에러 코드

`INVALID_INPUT`, `INGREDIENT_NOT_FOUND`, `GENERATION_LIMIT_EXCEEDED`, `NO_INGREDIENTS`, `AI_UPSTREAM_ERROR`, `AI_TIMEOUT`, `AI_INVALID_RESPONSE`, `RECIPE_NOT_FOUND`, `INSUFFICIENT_QUANTITY`, `COOK_ITEM_INVALID`, `SAVED_RECIPE_NOT_FOUND`. 공통 코드(`UNAUTHORIZED`, `TOKEN_EXPIRED`, `RATE_LIMITED`, `INTERNAL_ERROR`, `SERVICE_UNAVAILABLE`)는 모든 API에 나올 수 있어요. API별 코드와 화면 표시는 [`docs/agent/spec/errors.md`](../../agent/spec/errors.md)에 있어요.

## 발행 이벤트

발송 FE는 화면 담당 프론트가 API 56으로 보내고, BE는 이 영역 담당이 발행 API가 끝나는 지점에서 `EventTracker`로 기록해요([08](08_event-tracking.md)).

| ID | 이벤트 | 발생 시점 | 속성 | 발송 | 우선순위 | 발행 API |
| --- | --- | --- | --- | --- | --- | --- |
| E39 | `recipe_generate_started` | 생성 버튼을 눌렀을 때(빠른 생성 모달 "생성 하기", 커스텀 "레시피 생성") | recipe_mode, servings, option_count, supplement_enabled | FE | P0 | 56 POST /user/events(프론트 전송) |
| E40 | `recipe_generated` | 레시피 생성·다시 받기에 성공했을 때 | recipe_id, recipe_mode, is_regenerated, duration_ms, expiring_used_count, shortage_count, remaining_quota | BE | P0 | 28 레시피 생성, 29 다시 받기 |
| E41 | `recipe_generate_failed` | 레시피 생성·다시 받기에 실패했을 때(차감 안 함) | recipe_mode, is_regenerated, error_code, duration_ms | BE | P0 | 28 레시피 생성, 29 다시 받기 |
| E42 | `recipe_regenerated` | 결과 화면에서 "레시피 다시 받기"를 눌렀을 때 | recipe_mode | FE | P1 | 56 POST /user/events(프론트 전송) |
| E43 | `recipe_detail_viewed` | 내 레시피 상세가 열릴 때 | recipe_id, detail_origin | FE | P0 | 56 POST /user/events(프론트 전송) |
| E44 | `recipe_saved` | 레시피를 저장했을 때(내 레시피 북마크·공유 레시피 하트) | recipe_id, save_source, cart_added_count | BE | P0 | 34 내 레시피 저장, 53 공유 레시피 저장 |
| E45 | `recipe_unsaved` | 저장을 취소했을 때(상세 ⋮ "삭제 하기", 북마크 끄기, 하트 다시 누름) | save_source | FE | P1 | 56 POST /user/events(프론트 전송) |
| E46 | `recipe_saved_list_viewed` | 레시피 화면의 저장한 레시피 목록을 보거나 필터·정렬을 바꿨을 때 | saved_count, list_filter, list_sort | FE | P1 | 56 POST /user/events(프론트 전송) |
| E47 | `step_play_started` | "만드는 방법 재생하기"를 눌렀을 때(기기 음성 읽기) | recipe_id | FE | P2 | 56 POST /user/events(프론트 전송) |
| E48 | `cook_deduct_viewed` | 요리 완료 재료 차감 화면이 열릴 때 | recipe_id, deduct_item_count | FE | P1 | 56 POST /user/events(프론트 전송) |
| E49 | `cook_completed` | "N개의 재료 차감 하기"로 차감이 끝났을 때 | recipe_id, deducted_count, adjusted_count, from_meal_plan | BE | P0 | 32 요리 완료 |
| E50 | `cook_feedback_answered` | 차감 뒤 냉장고 바텀 시트에서 추천 여부를 보냈을 때 | recipe_id, recommended | FE | P0 | 56 POST /user/events(프론트 전송) |
| E77 | `recipe_favorited` | 즐겨찾기 별을 켜거나 껐을 때 | is_favorite | FE | P2 | 56 POST /user/events(프론트 전송) |

## 체크리스트

- [x] 빠른·커스텀 생성, 다시 받기가 하루 횟수를 같이 씀
- [x] 실패·`NO_INGREDIENTS`는 차감 없음, 차감·저장 한 트랜잭션
- [x] 보상 횟수를 만료 가까운 것부터 사용
- [x] 재료 보충 ON 저장 때 장바구니 담기
- [x] 요리 완료 차감(단위 환산, 꼬집·약간 제외), 오늘 식단 칸 요리 표시
- [ ] 차감 뒤 냉장고 이동 + 추천 여부 바텀 시트(E50 `cook_feedback_answered` 전송)
- [x] 저장 목록 필터·정렬, 즐겨찾기
