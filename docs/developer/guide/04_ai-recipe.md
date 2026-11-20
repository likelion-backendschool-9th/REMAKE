# 04. AI 레시피 (김종수)

## 범위

빠른·커스텀 레시피 생성, 레시피 상세, 요리 후 재료 차감, 저장·즐겨찾기, 생성 횟수(quota) 계산, 공통 `global`(예외·중복 요청 방지).

## 화면 (피그마 프레임)

4.1-레시피(+스크롤, 빠른 레시피 생성, 스켈레톤), 4.1.2-커스텀 레시피 생성(+원하는 맛·조리 시간·조리 도구·조리 방식·요리 유형·드로워·알레르기 재료), 4.1.3-레시피 생성(+레시피 저장), 4.1.1-레시피 상세(+스켈레톤, 케밥 메뉴, 레시피 재생), 4.1.1.2-레시피 사용 후 재료 차감(+수량 변경, 스켈레톤)

## 화면 규칙

- 생성 진입 두 가지: **빠른 레시피**(냉장고 재료 기반, 임박 재료 우선)와 **커스텀**(식사 인원 필수 + 재료·원하는 맛·조리 시간·도구·방식·요리 유형·알레르기 재료 선택). 모두 선택식이고 **자유 입력 칸은 없습니다**.
- 설정의 "레시피 재료 보충 여부"가 ON이면 냉장고에 없는 재료도 포함.
- 결과/상세: 재료(보유 상태 있음·부족·없음), 인분, 조리 시간, Tips!, 단계, "AI가 생성한 레시피" 문구, 알레르기 재료 포함 시 경고, 즐겨찾기 별표, 단계 "재생하기"(동작은 미결 #23).
- 응답은 스트리밍 없이 **한 번에** 옵니다. 생성 중에는 스켈레톤/로딩 화면.
- "요리 완료" → 사용 재료 선택·수량 변경 → 확정하면 재료 차감 + 요리 기록.

## API

| 메서드 | 경로 | 비고 |
|---|---|---|
| POST | `/api/v1/user/recipes/generate` | `{ mode: "QUICK"/"CUSTOM", servings, options? }` |
| GET | `/api/v1/user/recipes/{id}` | 재료(`have`), `tips`, `steps`, `aiGenerated`, `isFavorite`, `allergenWarning` |
| GET | `/api/v1/user/recipes/{id}/cook-preview` | 차감 후보와 기본 수량, 단위가 다르면 `needsCheck: true` |
| POST | `/api/v1/user/recipes/{id}/cook` | `{ items: [{ ingredientId, amount, unit }], servings, feedback? }` |
| GET | `/api/v1/user/saved-recipes` | 최신 저장순, `q`, `favorite=true` |
| PUT·DELETE | `/api/v1/user/saved-recipes/{recipeId}` | 저장·취소. PUT 응답 `{ cartAddedCount }` |
| PUT·DELETE | `/api/v1/user/saved-recipes/{recipeId}/favorite` | 별표 켜기·끄기 |

## 테이블

`recipes`, `recipe_ingredients`, `recipe_steps`, `recipe_catalog_items`(알레르기 경고용), `saved_recipes`(`is_favorite`), `recipe_cook_logs`, `usage_logs`, `idempotency_keys`. 레시피는 한 번 만들면 수정하지 않습니다.

## 횟수(quota) 규칙

- 기본 하루 2회(구독 4회, 1차는 UI만이라 기본값 적용). 하루 기본 2회를 먼저 쓰고, 다 쓰면 만료되지 않은 보상분(`generation_quota_grants`, 지급 후 한 달 유효)을 씁니다.
- **성공했을 때만 차감**. AI 실패·타임아웃·형식 검증 실패·서버 오류는 차감하지 않습니다.
- 동시에 여러 번 눌러도 한도를 넘지 않게 `usage_logs`에 `PENDING`을 먼저 기록 → 성공 시 `SUCCESS`, 실패 시 `FAILED`. 2분 넘게 남은 `PENDING`은 정리 배치로 `FAILED` 처리.
- 한도 초과 → `GENERATION_LIMIT_EXCEEDED`(429), `details`에 남은 횟수와 초기화 시각, 서버 이벤트 `quota_exceeded`.
- 남은 횟수 조회는 `GET /user/usage-quotas` 하나로 레시피·식단표를 모두 돌려줍니다. 레시피 부분은 김종수, 식단표 부분은 한윤수가 만듭니다(영수증과는 관계없음).

## AI 연동 규칙

- LLM 호출은 인터페이스 뒤 한 클라이언트로 감쌉니다. 입력은 요청의 id·enum과 DB 데이터(냉장고 재료, 제외 재료)만. **사용자 글을 프롬프트에 이어 붙이지 않습니다.**
- 구조화된 JSON으로 응답받고 검증합니다: 스키마, **단위는 14개 코드만**(목록에 없으면 거름), 단계 비어 있지 않음. 실패하면 저장하지 않고 `AI_INVALID_RESPONSE`(502), 차감 안 함.
- 제외(알레르기) 재료는 빼고 요청하며, 결과에 들어 있으면 `allergenWarning`.
- 타임아웃 60초 → `AI_TIMEOUT`. 공급자 오류 → `AI_UPSTREAM_ERROR`.
- 재료가 하나도 없으면 `NO_INGREDIENTS`.
- 소비기한 임박 재료를 우선 쓰고, 부족한 재료는 `have = SHORT/NONE`으로 표시.
- 비용 통제를 위해 입력 토큰을 줄입니다(필요한 재료 정보만).

## 요리 후 재료 차감 규칙

- 같은 종류 단위끼리만 자동 환산(큰술 15ml, 작은술 5ml, 컵 200ml). 다르면 자동 차감하지 않고 사용자 확인(`needsCheck`).
- 꼬집·약간은 차감하지 않음. 사용량이 가진 양보다 많으면 `INSUFFICIENT_QUANTITY`.
- 재료 차감은 02 영역 서비스를 호출해 `ingredient_logs`(USED)를 남기고, `recipe_cook_logs`에 요리 기록. 같은 요청을 두 번 보내도 한 번만(Idempotency-Key).
- `feedback`(레시피 추천 여부 LIKE/DISLIKE)은 이벤트로만 쌓는 안(미결 #23).

## 공유 레시피 저장과의 관계

공유 레시피를 저장하면 원본이 아니라 **사본**이 `recipes`에 생겨 내 저장 목록에 들어갑니다(요리 때 남의 재료가 차감되는 문제 방지). 구현은 07 영역과 함께.

## 에러 코드

`GENERATION_LIMIT_EXCEEDED`, `NO_INGREDIENTS`, `AI_UPSTREAM_ERROR`, `AI_TIMEOUT`, `AI_INVALID_RESPONSE`, `RECIPE_NOT_FOUND`, `COOK_ITEM_INVALID`, `SAVED_RECIPE_NOT_FOUND`, `INSUFFICIENT_QUANTITY`, `CONCURRENT_UPDATE`, `INVALID_INPUT`.

## 발행 이벤트

서버(4개): `recipe_generated`, `recipe_generate_failed`, `recipe_saved`, `cook_completed`(+ `quota_exceeded`는 레시피·식단표 담당이 각자). 레시피를 저장해 장바구니에 담길 때의 `cart_item_added`는 장바구니 담기 UseCase(07)가 보내므로 여기서는 보내지 않습니다. 프론트: `recipe_generate_started`, `recipe_regenerated`, `recipe_detail_viewed`, `recipe_unsaved`, `recipe_saved_list_viewed`, `step_play_started`, `cook_deduct_viewed`, `cook_feedback_answered`. 레시피 내용·재료 이름은 속성에 넣지 않습니다(`shortage_count` 같은 수치만).

## 체크리스트

- [ ] 빠른·커스텀 생성, 실패 시 차감 없음, 동시 클릭 테스트
- [ ] 단위 코드 검증·환산 단위 테스트
- [ ] 알레르기 경고, 재료 보충 ON/OFF 반영
- [ ] 요리 차감 미리보기·확정, 즐겨찾기, 저장 목록
- [ ] 60초 타임아웃·형식 오류 처리와 프론트 에러 화면
