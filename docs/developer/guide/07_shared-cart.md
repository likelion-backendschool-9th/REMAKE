# 07. 공유 공간·장바구니 (최유리)

기준: PRD v4·정책정의서 v4·ERD v4·API 정의서 v4·에러 정의서 v2·이벤트 정의서 v2(모두 피그마 기준, 2026-11-22, 노션). 표는 원본에서 그대로 옮겼고 피그마 노드 근거 칸만 뺐어요.

## 범위

장바구니(자동 담기, 수량 변경, 선택 삭제, 냉장고로 옮기기), 공유 공간(금주의 레시피 TOP 3, 인기·최신 목록, 공유 작성, 공유 상세·저장·신고), 주간 순위 기록과 보상 작업.

## 화면

화면 코드는 이벤트의 `screen` 값이에요. "추가될 페이지"는 아직 피그마에 없는 화면이에요.

| 화면 코드 | 피그마 화면 |
| --- | --- |
| `cart_list` | p7 장바구니 |
| `shared_write` | p4.1.1.1 레시피 공유 작성 |
| `recipe_ranking` | p4.1.4 이번 주 레시피 랭킹 |
| `shared_detail` | p4.1.4.1 공유 레시피 상세 |
| `shared_report` | p4.1.4.1 신고 시트 |

## 정책

상태: 확정 = 그대로 구현, 제안 = 상수로 분리해 구현하고 PR에 적기, 미결 = 구현 전에 준호님께 질문, 화면 대기 = 규칙은 정해졌고 화면이 추가될 예정.

### 장바구니

| 항목 | 규칙 | 상태 |
| --- | --- | --- |
| 위치 | 상단 탭 5번째 "장바구니" | 확정(피그마) |
| 담기는 경로 | 자동으로만 담김: 식단표 생성 때 부족 재료, 내 레시피 저장 때 재료 보충으로 들어간 부족 재료, 공유 레시피 저장 때 내게 없는 재료. 직접 추가 없음 | 확정 |
| 카테고리 | 담길 때 서버가 카테고리를 정해 저장 | 확정 |
| 목록 | 행마다 체크박스·이미지·출처 레시피명·재료명+수량·"수량 변경". 같은 재료라도 출처 레시피가 다르면 따로 | 확정(피그마) |
| 항목 조작 | 수량 변경 시트, 전체 선택(N건), "선택 삭제" → 삭제 모드(빨간 체크, "삭제 해제"로 되돌림) → "N개의 재료 삭제" | 확정(피그마) |
| 냉장고에 추가 | "N개의 재료 냉장고에 추가" → 입력 없이 냉장고로. 보관 위치·소비기한은 카테고리 기본값, 가격 없음. 넣은 항목은 장바구니에서 빠지고 "냉장고에 N개 넣었어요" 토스트 | 확정 / 토스트는 화면 대기 |
| 자동 빼기 | 영수증으로 같은 이름 재료를 등록하면 자동으로 빠짐 | 확정 |
| 빈 목록 | "장바구니가 비어있습니다", 버튼 비활성 | 확정(피그마) |
| 구매·결제 | 외부 쇼핑몰 이동·결제 없음 | 확정 |
| 일정 밀림 시 | 장바구니·공유 공간(P1)부터 2차로 | 확정 |

### 공유 공간

| 항목 | 규칙 | 상태 |
| --- | --- | --- |
| 위치 | 레시피 탭 "금주의 레시피" TOP 3 카드 + 전체 보기 → "금주의 인기 레시피"(/user/recipe/shared-recipes). 내 레시피/공유 공간 전환 탭 없음 | 확정 |
| TOP 3 카드 | 순위 번호·사진·제목·재료 요약 +N. 순위 변동은 보여주지 않음. 3개보다 적으면 빈 화면(추가 예정) | 확정(피그마) / 빈 화면은 화면 대기 |
| 인기 레시피 목록 | "11월 셋째 주", 인기순(기본)·최신순, 검색, 행(순위·사진·제목·재료 요약 +N·순위 변동). 인기순은 이번 주 저장 수 TOP 100 | 확정 |
| 순위 변동 | 지난주 순위와 비교해 파란 ↑N·빨간 ↓N. 지난주 순위가 없으면 "—". 순위가 같을 때 표기는 제안("—") | 확정 / 같은 순위는 제안 |
| 최신순 | 순위·변동 대신 "—", 오른쪽에 공유한 시간(1분 전~30일 전) | 확정(피그마) |
| 인기순 집계 | 이번 주(월요일 시작)에 받은 저장 수, 동점이면 먼저 공유한 것이 앞 | 확정 / 동점은 제안 |
| 검색 | 공유 레시피 검색, 결과 0건이면 빈 화면(추가 예정) | 확정(피그마) / 빈 화면은 화면 대기 |
| 공유 대상 | 내가 Remake로 만든 레시피만. 다른 사람 레시피를 저장한 것은 공유 못 함 | 확정 |
| 익명 | 닉네임·작성자 정보를 보여주지 않음 | 확정 |
| 공유 작성 | 상세 ⋮ "공유 하기" → 사진 1장(필수, "탭해서 이미지 수정")·레시피 제목(필수)·레시피 내용(읽기 전용) → "레시피 공유 하기". 제목과 사진이 있어야 버튼 활성, 확인 모달 없음 | 확정 |
| 다시 공유 | 같은 레시피는 한 주에 한 번(다음 주에 다시 가능) | 확정 |
| 공유 상세 | 사진, 제목, 깃발(신고), 재료 준비·인분·시간, 재료(내게 없는 재료는 빨간 테두리), 만드는 방법·재생하기, Tips!, 하트 + 저장 수, 레시피 저장, "레시피 저장 시 부족한 재료는 장바구니로 들어가요" | 확정(피그마) |
| 저장 | 하트와 "레시피 저장"은 같은 저장. 원본 레시피를 내 저장 목록에 넣음(사본 없음). 저장 수 +1, 다시 누르면 취소 -1. 내게 없는 재료는 장바구니로 | 확정 |
| 데이터 분리 | 내 저장 목록에서 지워도 공유 글은 그대로 | 확정 |
| 신고 | 깃발 → 사유 4개(불쾌한 이미지 사용·광고성 제목·위험한 조리법 공유·기타) → 신고 하기. 기타도 입력칸 없음 | 확정(피그마) |
| 신고 제한 | 한 사람이 같은 레시피를 한 번만. 다시 하면 REPORT_ALREADY_EXISTS 토스트 | 확정 |
| 신고 처리 | 자동으로 숨기지 않고 운영자가 검수. 처리 화면은 어드민 디자인 때 | 확정 / 처리 방식은 보류 |
| 내 공유 내리기 | 없음(화면 없음) | 확정 |
| 보상 대상 | 지난주 저장 수 1~3위 레시피를 올린 회원. 레시피 순위마다 줘서 한 사람이 여러 번 받을 수 있음 | 확정 |
| 보상 횟수 | 1위 +10회, 2위 +6회, 3위 +3회(AI 레시피 생성 횟수), 받은 날부터 한 달 안에 사용 | 확정 |
| 집계·지급 | 월요일 00:00~일요일 24:00(한국 시간)이 한 주. 매주 월요일 0시 작업이 지난주 TOP 100 순위를 기록하고 보상을 줌. 그때까지 남아 있는 저장만 셈. 같은 주 보상은 두 번 주지 않음 | 확정 |
| 집계 예외 | 본인 저장은 세지 않음, 저장 수 0이면 보상 없음 | 제안 |
| 보상 안내 | 보상 화면·알림 없음. 남은 생성 횟수에만 반영 | 확정 |

## API

경로의 `/api/v1`은 줄였어요. 요청의 `?`는 선택 값, `|`는 그중 하나예요. 번호는 API 정의서 번호예요(다른 문서도 이 번호로 가리켜요).

| 번호 | 메서드 | 경로 | 설명 | 요청 | 응답 | 관련 테이블 |
| --- | --- | --- | --- | --- | --- | --- |
| 45 | GET | `/user/cart-items` | 장바구니 목록 | `-` | `200 { items: [{ id, recipeId, recipeTitle, name, categoryCode, imageUrl, amount, unit }] }` | cart_items, recipes |
| 46 | PATCH | `/user/cart-items/{id}` | 수량 변경 | `{ amount }` | `200 { id, amount, unit }` | cart_items |
| 47 | POST | `/user/cart-items/move-to-fridge` | 고른 항목을 입력 없이 냉장고에 넣음. 보관 위치·소비기한은 카테고리 기본값, 가격 없음(11/21 답 31). 넣은 항목은 장바구니에서 빠짐(API 답 3) | `{ ids: [] }` | `200 { addedCount }` | cart_items, ingredients, ingredient_categories |
| 48 | POST | `/user/cart-items/delete` | 선택 삭제 | `{ ids: [] }` | `204` | cart_items |
| 49 | GET | `/user/shared-recipes/weekly-top` | 이번 주 저장 수 TOP 3 | `-` | `200 { items: [{ rank, id, title, imageUrl, ingredientSummary, extraIngredientCount }] }` | shared_recipes, saved_recipes |
| 50 | GET | `/user/shared-recipes/ranking` | 인기순 = 이번 주 저장 수 TOP 100, 순위 변동은 지난주 순위와 비교(11/20 결정). 최신순 = 공유 시각순, 순위 대신 "—" | `?sort=popular\|latest &q= &page=0&size=20` | `200 { weekLabel, content: [{ id, rank?, rankChange?, title, imageUrl, ingredientSummary, extraIngredientCount, sharedAt }], page, size, totalElements, hasNext }` | shared_recipes, saved_recipes, shared_recipe_weekly_ranks |
| 51 | POST | `/user/shared-recipes` | 공유. 제목·사진 1장 필수, 확인 모달 없음(11/21 답 19). 같은 레시피는 한 주에 한 번(11/21 답 17) | `multipart image + payload { recipeId, title }` | `201 { id }` | shared_recipes |
| 52 | GET | `/user/shared-recipes/{id}` | 공유 상세. 완전 익명(작성자 정보 없음). 재료마다 내가 가졌는지(빨간 테두리, 11/21 답 20) | `-` | `200 { id, title, imageUrl, servings, cookMinutes, tips, ingredients: [{ name, amount, unit, have }], steps: [{ stepNo, content }], saveCount, saved }` | shared_recipes, recipes, recipe_ingredients, recipe_steps, saved_recipes, ingredients |
| 53 | PUT | `/user/shared-recipes/{id}/save` | 저장(사본 없이 원본을 내 목록에). 부족한 재료는 장바구니로 | `본문 없음` | `200 { saveCount, saved: true, cartAddedCount }` | saved_recipes, cart_items |
| 54 | DELETE | `/user/shared-recipes/{id}/save` | 저장 취소(장바구니 항목은 그대로) | `-` | `200 { saveCount, saved: false }` | saved_recipes |
| 55 | POST | `/user/shared-recipes/{id}/reports` | 신고. 기타도 입력칸 없음, 검수는 운영자(11/21 답 21). 같은 글은 한 번만 신고(에러 정의서 답 3) | `{ reason: "OFFENSIVE_IMAGE"\|"AD_TITLE"\|"DANGEROUS_RECIPE"\|"OTHER" }` | `201` | shared_recipe_reports |

장바구니에 직접 담는 API와 내 공유 글을 내리는 API는 없어요.

## 테이블

`cart_items`, `shared_recipes`, `shared_recipe_reports`, `shared_recipe_weekly_ranks`, `generation_quota_grants`. 저장은 `saved_recipes`(04)를 같이 써요. 컬럼은 [`../erd.md`](../erd.md)를 보세요.

## 구현 메모

- 장바구니는 자동으로만 담겨요: 식단표 생성(05)의 부족 재료, 재료 보충으로 만든 내 레시피 저장(04), 공유 레시피 저장(53). 담을 때 서버가 카테고리를 정해요.
- 결정 [#46](https://github.com/likelion-backendschool-9th/REMAKE/issues/46): 담기는 `AddCartItemsUseCase` 하나로 하고, 이 UseCase만 `cart_item_added`를 발행해요(`cart_source` = meal_plan·own_recipe·shared_recipe, `item_count` = 넣은 행 수). 넣은 행이 0이면 발행하지 않고, 부르는 쪽(식단표·레시피 API)은 따로 발행하지 않아요.
- 같은 재료라도 출처 레시피가 다르면 행을 따로 둬요.
- 냉장고로 옮기기(API 47): 카테고리의 기본 보관 위치·보관 일수로 채우고 가격은 비워요. 02 UseCase로 재료를 만들고 옮긴 행은 지워요.
- 영수증 등록(03)에서 같은 이름 재료를 등록하면 장바구니 항목을 자동으로 지워요. 이 영역에 이름으로 지우는 UseCase를 둬요.
- 공유는 내가 만든 레시피만, 같은 레시피는 한 주(월요일 시작)에 한 번. 사진 1장·제목 필수, 확인 모달 없음. 완전 익명이라 어떤 응답에도 작성자 정보를 넣지 않아요.
- 공유 저장은 사본 없이 원본을 내 저장 목록에 넣어요(`saved_recipes`). 내게 없는 재료는 장바구니로. 저장을 취소해도 장바구니 항목은 그대로예요.
- 인기순은 이번 주 저장 수 TOP 100, 순위 변동은 지난주 기록(`shared_recipe_weekly_ranks`)과 비교해요. 동점은 먼저 공유한 것이 앞(제안).
- 신고는 사유 4개, 같은 글은 한 사람이 한 번만(`REPORT_ALREADY_EXISTS`). 자동 숨김 없이 운영자가 검수하고, 처리 화면은 어드민 디자인 때 정해요.
- 매주 월요일 0시 작업: 지난주 TOP 100 순위를 기록하고 1~3위 레시피를 올린 회원에게 +10·+6·+3회(레시피 순위마다, 한 달 유효)를 `generation_quota_grants`로 줘요. 그때 남아 있는 저장만 세고, 같은 주는 두 번 주지 않아요. 본인 저장 제외·저장 0이면 보상 없음은 제안이에요.
- 일정이 밀리면 장바구니·공유 공간(P1)부터 2차로 넘겨요.

## 지금 코드와 다른 점

- 아직 이 영역 코드는 없어요. 이전 가이드의 레시피 묶음 표시, 공유 확인 모달, 사진 여러 장, 공유 저장 사본, `shared_recipe_saved` 이벤트는 없어졌어요.
- 장바구니 → 냉장고와 영수증 등록 때 자동 빼기가 새로 생겼어요.

## 에러 코드

`INVALID_INPUT`, `CART_ITEM_NOT_FOUND`, `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, `RECIPE_NOT_FOUND`, `RECIPE_NOT_SHAREABLE`, `ALREADY_SHARED`, `PHOTO_REQUIRED`, `SHARED_RECIPE_NOT_FOUND`, `REPORT_ALREADY_EXISTS`. 공통 코드(`UNAUTHORIZED`, `TOKEN_EXPIRED`, `RATE_LIMITED`, `INTERNAL_ERROR`, `SERVICE_UNAVAILABLE`)는 모든 API에 나올 수 있어요. API별 코드와 화면 표시는 [`docs/agent/spec/errors.md`](../../agent/spec/errors.md)에 있어요.

## 발행 이벤트

발송 FE는 화면 담당 프론트가 API 56으로 보내고, BE는 이 영역 담당이 발행 API가 끝나는 지점에서 `EventTracker`로 기록해요([08](08_event-tracking.md)).

| ID | 이벤트 | 발생 시점 | 속성 | 발송 | 우선순위 | 발행 API |
| --- | --- | --- | --- | --- | --- | --- |
| E62 | `cart_item_added` | 부족한 재료가 장바구니에 담겼을 때(담은 경로당 1건) | cart_source, item_count | BE | P1 | 34·53 레시피 저장, 38 식단표 생성 |
| E63 | `cart_viewed` | 장바구니가 열릴 때 | item_count | FE | P1 | 56 POST /user/events(프론트 전송) |
| E64 | `cart_amount_changed` | 수량 변경 시트에서 확인을 눌렀을 때 | 공통 속성만 | FE | P2 | 56 POST /user/events(프론트 전송) |
| E65 | `cart_items_deleted` | 삭제 모드에서 고른 항목을 지웠을 때 | deleted_count, was_select_all | FE | P1 | 56 POST /user/events(프론트 전송) |
| E82 | `cart_moved_to_fridge` | "N개의 재료 냉장고에 추가"로 냉장고에 넣었을 때 | moved_count, was_select_all | BE | P1 | 47 장바구니 → 냉장고 |
| E69 | `ranking_viewed` | 금주의 레시피 TOP 3나 이번 주 레시피 랭킹이 보일 때, 정렬·검색을 바꿨을 때 | ranking_view, ranking_sort, query_used | FE | P1 | 56 POST /user/events(프론트 전송) |
| E70 | `shared_recipe_viewed` | 공유 레시피 상세가 열릴 때 | shared_recipe_id, view_from, rank_bucket | FE | P1 | 56 POST /user/events(프론트 전송) |
| E71 | `shared_write_started` | 상세 ⋮ "공유 하기"로 공유 작성 화면을 열 때 | recipe_id | FE | P1 | 56 POST /user/events(프론트 전송) |
| E72 | `shared_recipe_published` | 공유 레시피를 올렸을 때 | shared_recipe_id | BE | P1 | 51 공유 |
| E74 | `shared_recipe_reported` | 공유 레시피를 신고했을 때 | shared_recipe_id, report_reason | BE | P1 | 55 신고 |
| E75 | `weekly_reward_granted` | 금주의 레시피 보상을 지급했을 때(레시피 순위마다) | reward_rank, reward_amount | BE | P1 | 서버 정기 작업 금주의 레시피 보상(API 정의서 5장) |

## 체크리스트

- [ ] 세 경로 자동 담기와 `cart_item_added` 한 번만 발행
- [ ] 수량 변경·선택 삭제·냉장고로 옮기기(기본값 채움)
- [ ] 영수증 등록 때 같은 이름 자동 빼기
- [ ] 공유 한 주 한 번, 사진·제목 필수, 작성자 정보 없음
- [ ] 공유 저장(원본)·취소, 저장 수, 부족 재료 장바구니
- [ ] 신고 중복 거부, 월요일 순위 기록·보상 중복 지급 없음
