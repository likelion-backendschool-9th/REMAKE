# 02. 냉장고 (김재연)

기준: PRD v4·정책정의서 v4·ERD v4·API 정의서 v4·에러 정의서 v2·이벤트 정의서 v2(모두 피그마 기준, 2026-11-22, 노션). 표는 원본에서 그대로 옮겼고 피그마 노드 근거 칸만 뺐어요.

## 범위

로그인 후 첫 화면. 재료 목록·요약, 재료 추가·수정·삭제, 사용했어요·버렸어요 기록, 카테고리 기준 데이터, 재료명으로 단위를 정하는 재료 사전(`ingredient_catalog`, 서버 내부 전용).

## 화면

화면 코드는 이벤트의 `screen` 값이에요. "추가될 페이지"는 아직 피그마에 없는 화면이에요.

| 화면 코드 | 피그마 화면 |
| --- | --- |
| `fridge_list` | p2 냉장고(위치·종류 시트 포함) |
| `ingredient_consume` | p2 재료 사용 여부 시트 |
| `ingredient_edit` | p2.1 재료 수정 |
| `ingredient_add` | p2.2 재료 추가 |

## 정책

상태: 확정 = 그대로 구현, 제안 = 상수로 분리해 구현하고 PR에 적기, 미결 = 구현 전에 준호님께 질문, 화면 대기 = 규칙은 정해졌고 화면이 추가될 예정.

| 항목 | 규칙 | 상태 |
| --- | --- | --- |
| 첫 화면 | "냉장고에 재료가 N개 있어요", "AI를 활용해 레시피 생성하기 →"(레시피 메인으로), 위험·임박·여유 카드, 재료 목록 | 확정(피그마) |
| 재료 입력 항목 | 재료명·카테고리·보관 위치(냉장·냉동·실온)·소비기한·가격(선택)·수량. 직접 추가와 OCR 결과 수정·추가가 같은 항목 | 확정(피그마) |
| 필수 입력 | 재료명·카테고리·보관 위치·소비기한·수량 필수, 가격 선택. 필수값 전 버튼 비활성 | 확정 |
| 재료명 | 제품명이 아니라 재료명("비비고 냉동 만두 X → 만두 O") | 확정(피그마) |
| 단위 | 서버가 재료명(과 카테고리)에 맞춰 정함. 재료명을 정하기 전에는 수량 칸 비활성, 정하면 단위를 받아 켬. 카테고리를 바꿔도 다시 받음. 재료명이나 카테고리를 바꾸면 단위만 다시 정하고 숫자는 그대로. 추가·수정 모두 같고, 사용·버림 기록이 있어도 같음(기록은 당시 이름·단위를 따로 가짐) | 확정 |
| 수량 수정 | 입력 실수를 고치는 용도라 `amount`와 `initial_amount`를 같이 바꿈. 사용·버림 기록이 있으면 `INVALID_INPUT`으로 거절(#128) | 확정(#128) |
| 수량 | 분수 없이 숫자, 소수 셋째 자리까지. 냉장고 단위 9개(g·kg·ml·L·개·팩·단·쪽·장). 수량 변경 간격은 PRD 부록 A(제안) | 확정 / 간격은 제안 |
| 단위 통일 | 봉지·캔·병·통 → 팩, 마리·알·모·포기 → 개 | 확정 |
| 카테고리 | 12개, 가운뎃점 이름. 재료 추가·수정·OCR 수정·추가에는 "전체" 없음 | 확정 |
| 소비기한 상태 | 위험 = 기한이 지났거나 오늘(D-DAY), 임박 = D-1~D-3, 여유 = 그 뒤 | 확정 |
| 소비기한 표기 | D-N, 당일 D-DAY, 지나면 "폐기-N" | 확정(피그마) |
| 필터·정렬 | 위험·임박·여유 카드를 누르면 그 상태만, "재료 위치"·"재료 종류" 시트로 하나씩. 소비기한 가까운 순. 목록 제목은 "N개의 위험/임박/여유 재료" | 확정(피그마) |
| 검색 | 냉장고 재료 검색 없음 | 확정 |
| 재료 행 동작 | 행을 누르면 사용 여부 시트, 행 끝 > 는 재료 수정 | 확정 |
| 사용·버림 기록 | 사용 여부 시트에서 "사용 했어요 / 버렸어요" + 뺄 수량 + 확인. 버렸어요를 고르면 "이번 달 버린 식재료 금액에 더해져요" | 확정(피그마) |
| 가진 양보다 많이 | INSUFFICIENT_QUANTITY(400). 시트는 남은 수량까지만 올리게 막음 | 확정 / 막는 방식은 제안 |
| 남은 수량 0 | 0이 되면 재료 행을 지우고 기록은 이름과 함께 남음 | 확정 |
| 버린 금액 | 버린 금액 = 가격 × 버린 수량 ÷ 처음 수량. 가격이 없으면 금액 없음 | 확정 |
| 버린 금액 화면 | 이번 달 버린 식재료 금액을 보여 주는 화면(위치 미정) | 화면 대기 |
| 재료 삭제 | 재료 수정 화면 삭제 아이콘으로 지움. 사용·버림 기록이 있어도 지울 수 있고 기록은 이름과 함께 남음 | 확정 / 아이콘은 화면 대기 |
| 가격 | 직접 추가에도 가격(선택). "가격을 정확히 작성할수록 소비 패턴 분석률이 올라가요". 가격을 넣은 재료는 소비 패턴에 들어감 | 확정 |
| 소비기한 기본값 | 영수증·장바구니에서 들어온 재료는 카테고리 기본 보관 일수로 소비기한을 채움. 별도 "추정" 표시는 두지 않음(OCR 결과 화면 안내 문구만) | 확정 / 일수는 개발하면서 바꿈 |
| 소비기한 세부 규칙 | 추정 기준일, 냉동·실온 값, 다시 계산하는 조건은 개발자가 정함 | 확정 |
| 기한 지난 재료 | 자동으로 지우지 않고 "폐기-N"으로 남겨 사용자가 처리 | 제안 |
| 재구매 표시 | 없음(2차) | 확정 |

## API

경로의 `/api/v1`은 줄였어요. 요청의 `?`는 선택 값, `|`는 그중 하나예요. 번호는 API 정의서 번호예요(다른 문서도 이 번호로 가리켜요).

| 번호 | 메서드 | 경로 | 설명 | 요청 | 응답 | 관련 테이블 |
| --- | --- | --- | --- | --- | --- | --- |
| 11 | GET | `/user/ingredients/summary` | 전체 개수와 상태별 개수(필터와 무관) | `-` | `200 { total, danger, soon, ok }` | ingredients |
| 12 | GET | `/user/ingredients` | 내 재료 목록. 소비기한 가까운 순. 검색 없음(피그마에 없음). 냉장고 재료라 페이징 없이 전부 | `?status=DANGER\|SOON\|OK &storage=FRIDGE\|FREEZER\|PANTRY &categoryCode=` | `200 { items: [{ id, name, categoryCode, imageUrl, storage, amount, unit, expirationDate, dDay, status }] }` | ingredients, ingredient_categories |
| 13 | GET | `/user/ingredients/{id}` | 재료 하나 | `-` | `200 { id, name, categoryCode, imageUrl, storage, amount, unit, expirationDate, dDay, status, price }` | ingredients |
| 14 | GET | `/user/ingredient-units` | 재료명에 맞는 단위를 백엔드가 정해 줌(ERD 답 5). 재료명을 정하기 전에는 수량 칸 비활성, 재료명을 정하면 호출해 단위를 받고 수량 칸을 켬. 재료명이나 카테고리를 바꾸면 다시 호출(API 답 1). 등록 전인 OCR 재료 수정(p3.3.1)에서 바꿀 때도 호출(API 답 8). `categoryCode`는 선택이고, 넣으면 저장될 단위와 같음 | `?name=대파&categoryCode=VEGETABLE` | `200 { unit }` | - |
| 15 | POST | `/user/ingredients` | 직접 추가. 카테고리 필수("전체" 없음, ERD 답 4). 요청에 `unit`이 없고 서버가 재료명·카테고리로 정해 응답에 줌(API 14와 같은 규칙). 가격은 선택(넣으면 소비 패턴에 들어감, ERD 답 3) | `{ name, categoryCode, storage, expirationDate, amount, price? }` | `201 (재료 하나 모양)` | ingredients |
| 16 | PATCH | `/user/ingredients/{id}` | 바꾼 값만 보냄. 재료명이나 카테고리를 바꾸면 서버가 새 값에 맞게 단위를 다시 정해 응답에 줌(예: 대파 1단 → 양파 1개, API 답 8. API 14와 같은 결과). 같은 값을 다시 보내면 그대로. 기록이 있어도 같음. `amount`를 바꾸면 `initial_amount`도 같이 바뀌고, 사용·버림 기록이 있으면 `INVALID_INPUT`(#128) | `{ name?, categoryCode?, storage?, expirationDate?, price?, amount? }` | `200 (재료 하나 모양)` | ingredients |
| 17 | DELETE | `/user/ingredients/{id}` | 재료 삭제. 사용·버림 기록은 이름과 함께 남음 | `-` | `204` | ingredients, ingredient_logs |
| 18 | POST | `/user/ingredients/{id}/consume` | 사용·버림 기록. 버리면 버린 금액 계산(가격이 있을 때). 0이 되면 재료 삭제 | `{ type: "USED"\|"DISCARDED", amount }` | `200 { remainingAmount, deleted, costAmount? }` | ingredients, ingredient_logs |
| 19 | GET | `/user/ingredient-categories` | 카테고리 12개 | `-` | `200 { items: [{ code, name, imageUrl }] }` | ingredient_categories |

14는 저장(15·16·21)과 같은 규칙으로 단위를 정해요. `categoryCode`를 넣으면 저장될 단위와 같아요(예: "유기농 우유" + 달걀·유제품 → `PACK`). 넣지 않으면 사전 이름과 정확히 같을 때만 사전 단위이고 아니면 `EA`예요. 재료 추가(p2.2)·OCR 품목 수정(p3.3.1)은 저장 전에 카테고리가 정해져 있으니, 프론트는 카테고리가 있으면 항상 같이 보내고 이름이나 카테고리가 바뀔 때마다 다시 불러요. 숫자는 그대로 두고 단위만 바뀌므로, 미리 본 단위와 저장 단위가 다르면 입력한 양의 뜻이 바뀌기 때문이에요. `name`이 비어 있거나 없는 `categoryCode`면 `INVALID_INPUT`이에요.

재료 사전 자동완성 API(`GET /user/ingredient-catalog`)는 없어요(11/22 결정). `ingredient_catalog`는 서버 안에서 단위를 정할 때만 써요(API 14·15·16·21).

## 테이블

`ingredient_categories`(기준 데이터), `ingredient_catalog`(재료명 → 단위·카테고리·보관 일수, 기준 데이터), `ingredients`, `ingredient_logs`. 컬럼은 [`../erd.md`](../erd.md)를 보세요.

## 구현 메모

- 소비기한 상태는 서버가 계산해 `dDay`, `status`로 내려줘요. 위험 = 지났거나 D-DAY, 임박 = D-1~D-3, 여유 = 그 뒤. 기준(3일)은 상수 한 곳에 둬요.
- 알림의 임박 기준은 화면과 달라요(의도한 차이). D-DAY는 화면에서 위험, 알림에서 임박(`EXPIRY_SOON`)이에요. 알림 작업(06)이 이 영역 UseCase로 재료를 조회해요.
- 단위는 사용자가 고르지 않아요. 서버가 재료명으로 `ingredient_catalog`에서 정해요. 순서는 (1) 재료명이 사전 이름과 정확히 같으면 그 단위, (2) 아니면 재료명 안에 들어 있는 사전 이름 중 같은 카테고리의 두 글자 이상인 것(여러 개면 가장 긴 이름)의 단위, (3) 그래도 없으면 `EA`예요(#145 결정). 영수증에 찍힌 용량 단위("300g"의 g)는 사전 단위보다 앞서지 않아요. 직접 추가와 영수증 등록 API 21은 `ResolveIngredientUnitUseCase`를 써요. 장바구니 → 냉장고 API 47은 2026-11-24 사용자 승인에 따라 이미 정해진 수량·단위를 보존하고 큰술·작은술·컵만 ml로 환산해 `RegisterCartIngredientsUseCase`로 등록해요. 이름으로 단위만 다시 정하지 않아요.
- 재료 수정(PATCH)은 보낸 값만 바꿔요. 이름이나 카테고리가 실제로 바뀌면 단위만 다시 정하고 숫자는 그대로예요(대파 1단 → 양파 1개). 같은 이름·카테고리를 다시 보내면 단위는 그대로예요. 카테고리도 포함하는 이유는 화면이 이름·카테고리가 바뀔 때마다 API 14로 단위를 미리 보여 주므로, 저장 결과가 미리 본 것과 같아야 하기 때문이에요. 사용·버림 기록이 있어도 같아요. 기록은 당시 이름·단위·버린 금액을 따로 갖고 있어서 지난 기록은 바뀌지 않아요.
- 수량 수정은 입력 실수를 고치는 용도예요. `amount`와 `initial_amount`를 같이 바꿔요(버린 금액이 처음 수량으로 나누기 때문). 사용·버림 기록이 하나라도 있으면 `INVALID_INPUT`으로 거절해요(#128).
- 수량이 0이 되면 `ingredients` 행을 지우고 기록은 `ingredient_logs`에 이름과 함께 남아요. 재료 삭제도 기록이 있어도 돼요(기록·레시피 재료·알림의 연결만 비움).
- 버린 금액 = 가격 × 버린 수량 ÷ 처음 수량. 가격이 없으면 금액을 남기지 않아요. 이 값이 이번 달 버린 식재료 금액의 원본이에요.
- 같은 재료를 동시에 사용·폐기·수정·삭제해도 재료 행을 잠가서 차례로 처리해요. 늦은 요청은 앞 요청이 끝난 뒤의 수량을 보고, 이미 지워졌으면 404예요(낙관적 락 `version` 없음).
- 요리 후 재료 차감(04)과 장바구니 → 냉장고(07), 영수증 등록(03)도 이 영역 UseCase로 재료를 만들고 빼요. 단위 환산(같은 종류끼리만)은 한 곳에 두고 04와 같이 써요.
- 카테고리·재료 사전 데이터는 Flyway 마이그레이션으로만 넣어요. 머지된 마이그레이션 파일은 고치지 않고 새 번호를 추가해요.

## 지금 코드와 다른 점

- `ingredient_catalog`(PR #140, V5): `unit` 추가, 자동완성 API는 빼기.
- `GET /user/ingredients`의 `q`(검색)는 없어요(피그마에 검색 없음).

## 에러 코드

`INVALID_INPUT`, `INGREDIENT_NOT_FOUND`, `INGREDIENT_CATEGORY_NOT_FOUND`, `INSUFFICIENT_QUANTITY`. 공통 코드(`UNAUTHORIZED`, `TOKEN_EXPIRED`, `RATE_LIMITED`, `INTERNAL_ERROR`, `SERVICE_UNAVAILABLE`)는 모든 API에 나올 수 있어요. API별 코드와 화면 표시는 [`docs/agent/spec/errors.md`](../../agent/spec/errors.md)에 있어요.

## 발행 이벤트

발송 FE는 화면 담당 프론트가 API 56으로 보내고, BE는 이 영역 담당이 발행 API가 끝나는 지점에서 `EventTracker`로 기록해요([08](08_event-tracking.md)).

| ID | 이벤트 | 발생 시점 | 속성 | 발송 | 우선순위 | 발행 API |
| --- | --- | --- | --- | --- | --- | --- |
| E19 | `fridge_list_viewed` | 냉장고 목록이 열릴 때(요약 포함) | total_count, danger_count, soon_count, ok_count | FE | P0 | 56 POST /user/events(프론트 전송) |
| E20 | `fridge_filter_applied` | 상태 카드(위험·임박·여유)를 누르거나 위치·종류 칩을 골랐을 때 | filter_type, filter_value | FE | P1 | 56 POST /user/events(프론트 전송) |
| E21 | `ingredient_add_started` | 재료 추가 화면을 열었을 때 | add_source | FE | P0 | 56 POST /user/events(프론트 전송) |
| E22 | `ingredient_added` | 재료가 냉장고에 등록됐을 때(영수증·장바구니로 여러 개면 재료마다 1건) | add_source, category_code, storage, has_price, duration_sec | BE | P0 | 15 재료 추가, 21 영수증 등록, 47 장바구니 → 냉장고 |
| E23 | `ingredient_edited` | 재료 수정을 저장했을 때 | edit_field | FE | P1 | 56 POST /user/events(프론트 전송) |
| E24 | `ingredient_deleted` | 재료 수정 화면에서 삭제했을 때 | category_code | FE | P2 | 56 POST /user/events(프론트 전송) |
| E25 | `ingredient_used` | "사용 했어요"나 요리 완료 차감으로 수량을 뺐을 때(재료마다 1건) | use_via, category_code, expiry_status, used_ratio | BE | P0 | 18 사용·버림, 32 요리 완료 |
| E26 | `ingredient_discarded` | "버렸어요"로 수량을 뺐을 때 | category_code, expiry_status, used_ratio, has_price, cost_amount | BE | P0 | 18 사용·버림 |
| E27 | `ingredient_expired` | 소비기한 알림 작업(매일 오전 9시)이 기한 지난 재료를 처음 찾았을 때(재료당 1회) | category_code, has_price | BE | P0 | 서버 정기 작업 소비기한 알림(API 정의서 5장) |

## 체크리스트

- [x] 요약 수치와 목록의 위험·임박·여유 상태 일치
- [x] 상태 카드·위치·종류 필터 (API)
- [ ] 빈 목록 화면 (API는 빈 목록·0 요약을 돌려줌, 화면은 프론트 #99)
- [x] 재료명으로 단위 받기, 이름·카테고리를 바꾸면 단위만 다시(기록이 있어도)
- [x] 수량 수정 시 `initial_amount`도 같이 변경, 기록이 있으면 `INVALID_INPUT`
- [x] 사용·버림 후 수량 0이면 행 삭제, 버린 금액 계산
- [x] 기록 있는 재료 삭제
- [x] 냉장고 스키마 정리(#71): `ingredients`의 `is_expiration_estimated`·`status`·`version`, `ingredient_categories`의 `default_unit` 삭제, `sort_order` 추가, SEASONING 이름 '양념'
- [ ] 카테고리·재료 사전 마이그레이션
