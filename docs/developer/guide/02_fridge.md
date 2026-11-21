# 02. 냉장고 (김재연)

## 범위

로그인 후 첫 화면(인덱스). 재료 목록·요약, 재료 추가·수정, 사용했어요/버렸어요 기록, 재료 사전·카테고리 기준 데이터.

## 화면 (피그마 프레임)

p2-냉장고(+스켈레톤, 재료 스크롤, 재료 카테고리 선택, 위치 필터 선택, 임박·여유 버튼, 재료 위치), p2-재료 카테고리, p2-냉장고-재료 사용 여부, p2.1-재료 수정(+카테고리·보관 위치·소비 기한·재료명 수정), p2.2-재료 추가(+재료명·카테고리·보관 위치·소비 기한·재료 가격 수정, 스켈레톤)

## 화면 규칙

- 상단 "냉장고에 재료가 N개 있어요"와 위험·임박·여유 개수(`summary` API). 임박 순 기본 정렬, 위치(냉장·냉동·실온)·종류(카테고리 12개) 필터.
- 재료를 열면 "사용했어요 / 버렸어요 + 뺄 수량". 수량이 0이 되면 목록에서 사라지지만 DB에는 `FINISHED`로 남습니다.
- 직접 추가·수정: 이름, 카테고리, 보관 위치, 소비기한, 수량·단위, 가격(선택). 소비기한을 고치면 "추정" 표시가 없어집니다.
- 수량 버튼 간격: g·ml 50, kg·L 0.5, 개·팩·단·쪽·장 1. 직접 입력은 숫자·소수만(분수 없음).
- 냉장고 재료 단위는 9개만: G, KG, ML, L, EA, PACK, BUNCH, PIECE, SHEET (기본 EA).

## API

| 메서드 | 경로 | 비고 |
|---|---|---|
| GET | `/api/v1/user/ingredients/summary` | `{ total, danger, soon, ok }` |
| GET | `/api/v1/user/ingredients` | `storageType`, `categoryCode`, `q`. 항목에 `dDay`, `status`, `isExpirationEstimated` |
| GET | `/api/v1/user/ingredients/{id}` | 상세 |
| POST | `/api/v1/user/ingredients` | 필수 `name, categoryCode, storage, expirationDate, amount, unit`, 선택 `price` |
| PATCH | `/api/v1/user/ingredients/{id}` | 이름·카테고리·보관 위치·소비기한·가격·수량 (보낸 값만 수정, 단위는 변경 불가) |
| POST | `/api/v1/user/ingredients/{id}/consume` | `USED`/`DISCARDED` + 수량 |
| DELETE | `/api/v1/user/ingredients/{id}` | 기록이 있으면 불가 |
| GET | `/api/v1/user/ingredient-catalog` | 자동완성, `commonExclusion=true`면 대표 알레르기 재료 상단. `q`는 이름에 포함(대소문자 무시, 50자 이하·넘으면 `INVALID_INPUT`, 없으면 전체), 앞부분이 맞는 이름이 먼저, 최대 50개. 항목: `id`, `name`, `categoryCode`, `shelfLifeDays`(항목 값이 없으면 카테고리 기본값), `isCommonExclusion` |
| GET | `/api/v1/user/ingredient-categories` | 카테고리 12개 기준값 |

## 테이블

`ingredient_categories`, `ingredient_catalog`(기준 데이터, 시드 필요), `ingredients`(`version` 낙관적 락), `ingredient_logs`(사용·버림 이력, `unit` 포함, 버린 금액 통계의 원본).

## 구현 규칙

- **소비기한 상태는 서버가 계산**: 지났거나 D-DAY면 DANGER, D-1~D-3 SOON, 그 외 OK. 기준일(3일)은 상수 한 곳에.
- 알림의 임박 기준은 화면과 다릅니다(의도한 차이). 오늘이 기한인 재료(D-DAY)는 화면에서는 DANGER(위험), 알림에서는 임박(`EXPIRY_SOON`)입니다. 알림용 조회는 재료 쪽 UseCase(`GetExpiryAlertIngredientsUseCase`)가 따로 판정합니다.
- **재료 수정(PATCH)**: 피그마 p2.1에 맞춰 이름·카테고리·보관 위치·소비기한·가격·수량을 고칠 수 있습니다. 보내지 않은 키는 그대로이고, `"price": null`이면 가격을 지웁니다. 단위는 등록한 뒤 바꾸지 않습니다(`unit`을 보내도 무시). 소비기한을 고치면 추정 표시(`isExpirationEstimated`)가 꺼집니다. 수량은 사용·폐기한 적이 없고 완료되지 않은 재료만 고칠 수 있고, 입력 실수를 바로잡는 용도라 남은 양과 처음 양을 함께 바꿉니다. 이미 쓰거나 버린 재료의 수량을 고치면 `INVALID_INPUT`입니다.
- **단위**: 냉장고는 단위 9개(G, KG, ML, L, EA, PACK, BUNCH, PIECE, SHEET)만 받습니다. 그 밖의 값(레시피 단위 `TBSP` 등 포함)은 `INVALID_UNIT`입니다.
- 같은 재료를 두 기기에서 동시에 수정하면 `version` 충돌 → `CONCURRENT_UPDATE`. 프론트는 최신 데이터를 다시 불러옵니다.
- `consume`: 가진 양보다 많이 빼면 `INSUFFICIENT_QUANTITY`. 이미 `FINISHED`면 `INGREDIENT_ALREADY_FINISHED`. 버렸어요는 `cost_amount`(가격 비례)를 로그에 남기고, 가격이 없으면 금액 집계에서 빼고 "제외 N개"로 안내.
- 요리 후 재료 차감(04 영역)도 `ingredient_logs`에 `USED`로 남기므로, 차감 로직은 이 영역의 서비스를 재사용하세요.
- 단위 환산은 같은 종류끼리만(무게↔무게, 부피↔부피). 종류가 다르면 자동 차감하지 않습니다. 공통 유틸은 `global`이나 `ingredient`에 한 곳만 두고 04가 함께 씁니다.
- 영수증에서 수량을 못 읽으면 1개(EA)로 등록(03 영역이 처리).
- 카테고리별 기본 보관 기간(채소 7일, 육류 3일 등)은 PRD 부록 B 초안 값이며 정확한 값은 개발자가 정합니다(소비기한 세부는 위임).
- 카테고리 이미지는 `ingredient_categories.image_url`(소비 패턴 화면에서도 씀).

## 에러 코드

`INGREDIENT_NOT_FOUND`, `INSUFFICIENT_QUANTITY`, `INVALID_UNIT`, `INGREDIENT_ALREADY_FINISHED`, `INGREDIENT_HAS_LOGS`, `CONCURRENT_UPDATE`, `INVALID_INPUT`.

## 발행 이벤트

서버(3개): `ingredient_added`, `ingredient_used`, `ingredient_discarded`. 프론트: `fridge_list_viewed`, `fridge_filter_applied`, `ingredient_add_started`, `ingredient_edited`, `ingredient_deleted`. `ingredient_expired`는 알림 배치(06)가 발행합니다. 속성에는 재료 이름 대신 `category_code`만 넣습니다.

## 체크리스트

- [ ] 요약 수치와 목록의 위험·임박·여유 상태 일치
- [ ] 필터·정렬 동작, 빈 목록 화면
- [ ] 사용·버림 후 수량 0 처리, 버린 금액이 소비 패턴에 반영
- [ ] 동시 수정 충돌, 기록 있는 재료 삭제 불가 테스트
- [x] 카테고리·재료 사전 시드 데이터
