# 03. 영수증·OCR·소비 패턴 (이동준)

## 범위

영수증 스캔(OCR) → 확인·수정 → 확정 저장 → 냉장고 등록, 영수증 목록·상세·삭제, 월별 식비, 소비 패턴("나의 소비").

## 화면 (피그마 프레임)

p3-영수증(+스켈레톤, 최대 스크롤), p3.1-카메라, p3.3-ocr 결과(+스켈레톤), p3.3.1-재료 수정(+카테고리·보관 위치·소비 기한·재료명), p3.3.2-재료 추가(+각 수정, 스켈레톤), p3.2-영수증 디테일(+스켈레톤), p3.4-소비 패턴(+스켈레톤, 빈 리스트)

## 핵심 흐름 (순서가 중요)

```
스캔 화면 → [POST /receipts/scan] 로딩 화면(동기 대기) → OCR 결과·확인 화면
   → 수정·추가·삭제·등록 여부 선택 → [POST /receipts] 확정 저장
```

- **확정 전 OCR 결과는 서버에 저장하지 않습니다.** `scan`은 분석만 하고 결과를 응답으로 돌려줍니다. 사용자가 확인을 마친 뒤 `POST /receipts` 한 번에 영수증·품목·냉장고 재료를 한 트랜잭션으로 저장합니다.
- **확정한 영수증은 수정할 수 없고 삭제만** 됩니다(PATCH·confirm API 없음). 재료 정보 수정은 냉장고에서.
- OCR은 **동기 처리**(한 요청). 로딩 화면에서 결과를 기다리고, 스트리밍·완료 알림은 없습니다.
- 결과에는 **식재료만** 남깁니다. 수세미·냄비·세제 같은 비식재료는 응답에 나오지 않습니다(저장도 안 함).
- 카테고리는 AI가 추정해 채우고 사용자가 바꿀 수 있습니다. 소비기한이 없으면 카테고리 기본 보관 기간으로 추정하고 `isExpirationEstimated: true`("소비 기한은 카테고리 기본 보관 기간으로 추정했어요" 문구).
- 수량을 못 읽으면 1개(EA). 단위는 부록 A의 코드로 정규화(포장 단위는 PACK, 낱개는 EA).
- 영수증 스캔은 **횟수 제한이 없고** 횟수 표시도 없습니다. 호출은 비용·성공률 모니터링을 위해 `usage_logs`(type=OCR)와 이벤트로만 기록합니다.

## API

| 메서드 | 경로 | 비고 |
|---|---|---|
| POST | `/api/v1/user/receipts/scan` | multipart `image`(10MB, jpg·png·heic). `items[]`, `purchasedOn`, `storeName`, `storeCode`, `duplicateSuspected` |
| POST | `/api/v1/user/receipts` | multipart `image` + `payload` JSON. `items[].register`가 true인 것만 냉장고에 등록, 수량 소수 셋째 자리까지. 응답 `{ receiptId, registeredCount }` |
| GET | `/api/v1/user/receipts` | `?month=YYYY-MM`(최근 6개월). 항목 `id, storeName, storeCode, purchasedOn, foodItemCount, foodAmount` |
| GET | `/api/v1/user/receipts/{id}` | 품목, 총 금액(식재료 합계), 이미지 서명 URL |
| DELETE | `/api/v1/user/receipts/{id}` | 이미지·품목 삭제, 등록된 재료는 유지 |
| GET | `/api/v1/user/expenses/monthly` | 월 총액 + 최근 6개월 평균 대비(`comparison.diffAmount`) |
| GET | `/api/v1/user/expenses/monthly/analysis` | 카테고리 비중 + 버린 재료 금액(`ingredient_logs.cost_amount` 합계. 보여 줄 피그마 화면은 추가 예정) |
| GET | `/api/v1/user/expenses/patterns` | `?month=`: 6개월 월 탭, 총액·평균 대비, 카테고리 비중, 날짜별 합계 |
| GET | `/api/v1/user/expenses/daily-items` | `?date=`: 그날 산 식재료 목록(`imageUrl`은 카테고리 이미지) |

## 테이블

`receipts`(확정 영수증만, `store_code`, 인덱스 `(user_id, purchased_on)`), `receipt_items`(식재료만). 저장 이미지는 영수증 삭제·계정 삭제 때 함께 지웁니다(저장소는 백엔드가 정함).

## 구현 규칙

- **보관 6개월**: 영수증에 찍힌 구매일 기준(없으면 저장일). 매일 새벽 배치가 6개월이 지난 영수증을 이미지·품목과 함께 삭제. 6개월보다 오래된 구매일은 등록 불가(`INVALID_INPUT`). 화면 문구도 6개월입니다(피그마의 "1년 동안 보관"은 고칠 예정).
- 금액 집계는 확정 영수증의 **식재료 품목만**, 구매일 기준. 영수증을 삭제하면 식비·소비 패턴에서도 빠집니다.
- 월을 고르면 월 합계와 영수증 목록이 같은 달로 바뀝니다(`month` 파라미터 공유). 최근 6개월 밖·미래 달은 `INVALID_INPUT`, 영수증이 없는 달은 0원·빈 배열 200.
- 평균 비교는 내 최근 6개월 월평균. 과거 데이터가 없으면 `comparison: null`(문구 숨김). 화면 "내 평균보다 N원 더 썼어요"는 `diffAmount`.
- 냉장고에서 버린 재료도 영수증 금액에서 빼지 않고 소비 패턴에 구매 소비로 그대로 포함합니다.
- `storeCode`는 15개 마트 아이콘 코드(`CU, GS25, SEVEN_ELEVEN, EMART24, EMART, HOMEPLUS, LOTTE_MART, COSTCO, TRADERS, HANARO, GS_FRESH, LOTTE_SUPER, EMART_EVERYDAY, HOMEPLUS_EXPRESS, ETC`). 못 맞추면 `ETC`. 아이콘 이미지는 디자인 자료(피그마, 노션/구글 시트 참고)에 있습니다.
- 중복 의심: 같은 판매처·구매일·식재료 합계의 영수증이 이미 있으면 `duplicateSuspected: true`(막지는 않고 안내).
- `POST /receipts`는 `Idempotency-Key`로 두 번 눌러도 한 장만 저장.
- OCR 호출은 타임아웃 30초 → `OCR_TIMEOUT`. 공급자 오류는 `OCR_UPSTREAM_ERROR`. OCR에 AI를 붙이면 그때만 한도를 정합니다(미결).
- OCR 정확도는 DB가 아니라 이벤트로만 잽니다: `receipt_saved`(OCR 품목 수·수정·삭제·추가 수), 저장 없이 나가면 프론트가 `receipt_result_abandoned`.

## 에러 코드

`FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, `RECEIPT_NOT_RECOGNIZED`, `OCR_UPSTREAM_ERROR`, `OCR_TIMEOUT`, `RECEIPT_NOT_FOUND`, `INVALID_INPUT`, `INVALID_UNIT`. 카메라를 못 쓰면 `camera_unavailable` 화면(프론트).

## 발행 이벤트

서버(3개): `receipt_scan_succeeded`, `receipt_scan_failed`, `receipt_saved`(확정 때 재료마다 `ingredient_added`도). 프론트: `receipt_scan_started`, `camera_unavailable`, `receipt_item_edited`, `receipt_item_added_manually`, `receipt_result_abandoned`, `receipt_list_viewed`, `receipt_detail_viewed`, `receipt_deleted`, `consumption_pattern_viewed`.

## 체크리스트

- [ ] 스캔 → 결과 → 확정 → 냉장고 등록 전체 흐름(한 트랜잭션)
- [ ] 비식재료 걸러내기, 카테고리·소비기한 추정, 수량·단위 정규화
- [ ] 확정 전에는 DB에 아무것도 안 남는지 테스트
- [ ] 월 선택에 따라 합계·목록·소비 패턴 일치, 영수증 삭제 반영
- [ ] 6개월 정리 배치, 6개월 초과 구매일 거절
