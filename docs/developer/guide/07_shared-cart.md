# 07. 공유 공간 + 장바구니 (최유리)

## 범위

레시피 공유(완전 익명), 이번 주 레시피 랭킹(TOP 100)과 금주 TOP 3, 공유 레시피 상세·저장·신고, 주간 보상 지급, 장바구니(부족 재료 자동 담기).

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
- 담기 함수를 하나 만들어(`CartService.addShortages(userId, recipeId, shortages)`) 04·05가 호출하게 하고, 담은 개수를 응답(`cartAddedCount`)으로 돌려줍니다.

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

서버(5개): `cart_item_added`(담은 경로당 1건, `cart_source`·`item_count`), `shared_recipe_published`, `shared_recipe_saved`, `shared_recipe_reported`, `weekly_reward_granted`(+ 공유 저장 때 `recipe_saved`). 프론트: `ranking_viewed`, `shared_recipe_viewed`, `shared_write_started`, `cart_viewed`, `cart_amount_changed`, `cart_items_deleted`. 레시피·재료 이름은 속성에 넣지 않습니다.

## 체크리스트

- [ ] 익명 보장(응답·로그·이벤트에 작성자 정보 없음)
- [ ] 사진 필수·형식·용량 검증, 사진 저장소
- [ ] 이번 주 저장 수 랭킹, 순위 변동, 최신순 표시
- [ ] 저장 사본 생성·장바구니 자동 담기
- [ ] 월요일 배치(보상, 지난주 TOP 100 기록), 보상 한 달 유효
