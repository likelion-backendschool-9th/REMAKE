# 03. 영수증·OCR·소비 패턴 (이동준)

기준: PRD v4·정책정의서 v4·ERD v4·API 정의서 v4·에러 정의서 v2·이벤트 정의서 v2(모두 피그마 기준, 2026-11-22, 노션). 표는 원본에서 그대로 옮겼고 피그마 노드 근거 칸만 뺐어요.

## 범위

영수증 스캔(OCR) → 확인·수정 → 등록(냉장고 재료까지), 중복 영수증 막기, 영수증 목록·상세·삭제, 월 식비, 나의 소비(소비 패턴).

## 화면

화면 코드는 이벤트의 `screen` 값이에요. "추가될 페이지"는 아직 피그마에 없는 화면이에요.

| 화면 코드 | 피그마 화면 |
| --- | --- |
| `receipt_list` | p3 영수증 |
| `receipt_scan` | p3.1 카메라, 이미지 업로드(추가될 페이지 4번) |
| `receipt_loading` | p3.3 OCR 결과 스켈레톤 |
| `receipt_scan_error` | OCR 실패 화면(추가될 페이지 4번) |
| `receipt_result` | p3.3 OCR 결과 |
| `receipt_item_edit` | p3.3.1 OCR 재료 수정 |
| `receipt_item_add` | p3.3.2 OCR 재료 추가 |
| `receipt_detail` | p3.2 영수증 상세 |
| `receipt_pattern` | p3.4 소비 패턴 |

## 정책

상태: 확정 = 그대로 구현, 제안 = 상수로 분리해 구현하고 PR에 적기, 미결 = 구현 전에 준호님께 질문, 화면 대기 = 규칙은 정해졌고 화면이 추가될 예정.

| 항목 | 규칙 | 상태 |
| --- | --- | --- |
| 스캔 방법 | "영수증 스캔 하기" → 카메라 촬영, 또는 "첨부 하기"·카메라 갤러리 아이콘 → 이미지 업로드(업로드 이후 화면은 추가 예정) | 확정(피그마) / 업로드 이후는 화면 대기 |
| 카메라 방식 | 앱 안 촬영 화면이 아니라 휴대폰 기본 카메라 앱을 엶. 파일 선택 입력으로: 카메라는 `accept="image/*" capture="environment"`, 앨범은 `capture` 없이 `accept="image/*"`. 둘 다 사진 파일 하나를 돌려받아 스캔으로 보냄. 앱 안 미리보기·가이드 틀·플래시·초점 조절 없음, 권한 검사·토글 없음(권한은 기본 카메라 앱이 처리). 카메라가 없는 기기는 파일 선택 창이 열림 | 확정(11/25) |
| 카메라를 못 쓸 때 | 공통 토스트 "카메라를 쓸 수 없어요. 갤러리에서 골라 주세요"(CAMERA_UNAVAILABLE). 기본 카메라 앱 방식에서는 거의 뜨지 않고, 사진을 찍을 수 없을 때를 위한 안전망으로 둠 | 확정 / 문구는 제안 |
| 스캔 한도 | 없음 | 확정 |
| OCR 처리 | 동기 처리(최대 30초), 기다리는 동안 OCR 결과 스켈레톤. 분석 완료 알림 없음 | 확정 |
| OCR 실패 | OCR 실패 화면 하나에 문구만 다르게: 영수증을 읽지 못함(영수증 아님·OCR 오류·시간 초과), 식재료가 하나도 없음, 구매일이 6개월 지남, 이미 등록한 영수증. 다시 찍으면 됨 | 확정 / 화면 대기 |
| 결과에 나오는 것 | 식재료만(수세미·냄비 같은 비식재료는 걸러 냄). 식재료는 카테고리·보관 위치·수량·가격·소비기한을 채워 보여 줌 | 확정 |
| 결과 확인·수정 | "영수증 재료" 화면에서 품목 수정(p3.3.1)·추가(p3.3.2)·삭제. OCR이 잘못 읽은 품목을 뺄 수 있게 삭제를 허용함: p3.3.1 휴지통 → 확인 모달 "재료를 삭제할까요?"(11/22 취소했다가 11/24 다시 넣음). 재료 추가·영수증 재촬영은 앱바 ⋮ 시트에서 들어감. 고친·추가한·뺀 값은 화면 상태로만 두고 등록할 때 함께 보냄(뺀 품목은 보내지 않음) | 확정 |
| 소비기한 추정 안내 | "소비 기한은 카테고리 기본 보관 기간으로 추정 했어요. 소비기한을 확인 하시고 수정해 주세요" | 확정(피그마) |
| 등록 | "식재료 N개 등록 하기"를 누를 때 영수증 이미지·영수증·품목·냉장고 재료를 한 번에 저장. 등록 전 OCR 결과는 서버에 저장하지 않고, 등록하지 않고 나가면 정확도 분석용 이벤트만 남음 | 확정 |
| 구매일·판매처 | OCR이 못 읽으면 비워서 주고, 등록할 때 구매일·판매처 이름은 필수 | 확정 |
| 6개월 지난 영수증 | 구매일이 6개월 지난 영수증은 등록할 수 없음(스캔 단계에서 OCR 실패로) | 확정 |
| 중복 등록 | 등록 버튼은 요청이 끝날 때까지 비활성 | 확정 |
| 중복 영수증 | 판매처·영수증 번호·구매 시각이 모두 같으면 이미 등록한 영수증으로 보고 등록을 막음(번호를 못 읽었으면 판매처·구매일·식재료, 시각을 못 읽었으면 구매일로 비교). 스캔 직후 확인해 OCR 실패 화면에 "이미 등록한 영수증이에요"(RECEIPT_DUPLICATE 409), 등록 때 한 번 더 확인 | 확정 / 화면 대기 |
| 장바구니 선택 차감 | 최종 등록 직전 바텀시트에서 품목당 0~1개 행 선택. 건너뛰면 유지. 실제 등록량 이내·동일 단위로 차감하고 0이 된 행만 삭제 | #57 합의 / 화면 구현은 별도 |
| 영수증 목록 | 월 선택 "2026년 11월 ∨"(최근 6개월), 월 합계, "내 평균보다 N원 더 썼어요", 주차 묶음(주차·기간·합계), 영수증 행(마트 아이콘·판매처·날짜·식재료 N개·금액) | 확정(피그마) |
| 월 합계 | 그 달 영수증의 식재료 품목 금액 합계(구매일 기준) | 확정 |
| 평균 비교 | 내 과거 평균(최근 6개월 월평균)과의 금액 차이. 이번 달 제외·기록 있는 달만·과거가 없으면 숨김은 제안 | 확정 / 세부는 제안 |
| 판매처 아이콘 | 판매처 이름을 14종 코드(CU·GS25·세븐일레븐·이마트24·이마트·홈플러스·롯데마트·코스트코·트레이더스·하나로·GS더프레시·롯데슈퍼·이마트에브리데이·홈플러스익스프레스)에 맞춰 저장, 못 맞추면 비우고 기본 아이콘 | 확정 / 매칭 방식은 제안 |
| 영수증 상세 | 원본 이미지, "2026년 11월 영수증", 마트·지점명, 구매일(날짜만, 구매 시각은 보이지 않음 11/25 결정), 품목(이미지·이름·보관 위치·수량·금액), 총 금액(식재료 품목 합계), 삭제 | 확정(피그마) |
| 수정·삭제 | 등록한 영수증은 수정 없이 삭제만. 냉장고에 들어간 재료는 냉장고에서 따로 고침(구매가를 고쳐도 영수증 금액은 그대로) | 확정 |
| 영수증 삭제 | 삭제 모달 → 영수증·품목·이미지 삭제, 식비·소비 패턴에서도 빠짐, 냉장고 재료는 남음. 모달 제목·본문은 피그마 수정 대상 | 확정 |
| 보관 기간 | 구매일부터 6개월, 지나면 매일 새벽 정리 작업이 이미지와 함께 삭제 | 확정 |
| 이미지 저장 | 저장소 종류는 백엔드가 정함. 응답 이미지 주소는 서버가 만듦 | 확정 |
| 나의 소비 | "소비 패턴 확인 하기" → 나의 소비 화면: 최근 6개월 월 탭(프론트가 만듦), 그 달 지출(양수), "평균 보다 N원 더 지출", 카테고리 비율 막대·범례, 월~일 날짜 줄(선택일 아래 금액), 선택일 "총 N원"과 식재료 목록 | 확정(피그마) |
| 소비 패턴 범위 | 영수증 품목 + 영수증 없이 추가했고 가격이 있는 재료(등록일 기준). 등록 후 가격을 넣거나 고쳐도 반영하고, 지우면 합계에서 빼며 날짜는 등록일 그대로. 가격이 없으면 냉장고에만 넣은 것으로 보고 뺌. 버린 재료도 이미 산 것이라 그대로 포함 | 확정 |
| 카테고리 비율 | 12개 카테고리별 지출 비율을 막대 하나와 범례로, 지출 0인 카테고리는 빠짐 | 확정(피그마) / 0 제외는 제안 |
| 날짜 선택 | 기본은 오늘(이번 달이 아니면 그 달 마지막 구매일). 소비가 없으면 "해당 날짜에는 소비가 없습니다" | 확정(피그마) / 기본 날짜는 제안 |
| 주 넘기기 | 월~일 날짜 줄을 좌우로 밀어(캐러셀) 고른 달의 앞·뒤 주로 넘김. 고른 달 밖의 날과 오늘 뒤의 날은 고를 수 없음 | 확정(11/25) / 고를 수 없는 날은 제안 |
| 평균 차이 표기 | "평균보다 N만원 더(덜) 지출": 1만원 이상은 만원 단위로 반올림, 그 밑은 원 단위. 차이가 0이면 "평균과 같게 지출" | 확정(11/25) / 0일 때 문구는 제안 |
| 버린 식비 | 나의 소비 위쪽 "N월 버린 식비 N원"을 누르면 버린 식재료(p3.4.1): "N월에 버린 식재료" 금액(가격 있는 재료만), "가격을 넣은 재료만 금액에 더해져요", 버린 재료 행(카테고리 이미지·이름·"M월 D일 버림"·금액 또는 "가격 없음"), 최근에 버린 것부터. API 25 `discardedAmount`와 API 72 `/user/expenses/discarded-items`(#248) | 확정 |
| 자주 산 품목·구매 주기 | 없음 | 확정 |

## API

경로의 `/api/v1`은 줄였어요. 요청의 `?`는 선택 값, `|`는 그중 하나예요. 번호는 API 정의서 번호예요(다른 문서도 이 번호로 가리켜요).

| 번호 | 메서드 | 경로 | 설명 | 요청 | 응답 | 관련 테이블 |
| --- | --- | --- | --- | --- | --- | --- |
| 20 | POST | `/user/receipts/scan` | OCR 분석만 하고 저장하지 않음(11/20 결정). 동기 처리. 식재료만 돌려줌. 식재료가 하나도 없거나 구매일이 6개월 지난 영수증은 실패로 돌려주고 OCR 실패 화면(추가될 페이지 4번)에서 보여줌(에러 정의서 답 4). 이미 등록한 영수증(판매처(지점명)·영수증 번호·구매 시각이 모두 같은 영수증(번호를 못 읽었으면 판매처·구매일·식재료 이름 목록, 시각을 못 읽었으면 구매일로 비교))도 같은 화면에서 실패로 보여줌(정책 답 8·10). `dDay`·`status`는 주지 않음. 저장 전이고 사용자가 소비기한을 고치는 값이라 프론트가 `expirationDate`와 KST 오늘 날짜로 계산함(11/25) | `multipart image(jpg·png·heic, 10MB)` | `200 { storeName, storeCode, purchasedOn, purchasedTime, receiptNumber, items: [{ name, categoryCode, storage, amount, unit, price, expirationDate, purchaseQuantity?, volume?: { amount, unit, scope } }] }` | receipts(중복 확인만) |
| 21 | POST | `/user/receipts` | 영수증·품목·냉장고 재료를 한 번에 저장. 단위는 서버가 재료명으로 정함(ERD 답 5). 선택한 장바구니 행만 실제 등록량 이내·동일 단위로 차감(#57). 건너뛰면 유지하며 0이 된 행만 삭제. 6개월 지난 영수증은 등록 불가(11/20 결정). 이미 등록한 영수증이면 한 번 더 막음(정책 답 8·10) | `multipart image + payload { storeName, storeCode, purchasedOn, purchasedTime, receiptNumber, items: [{ name, categoryCode, storage, amount?, purchaseQuantity?, volume?: { amount, unit, scope }, price, expirationDate, cartDeductions?: [{ cartItemId, amount }] }] }` | `201 { receiptId, ingredientCount, cartRemovedCount }` | receipts, receipt_items, ingredients, cart_items |
| 22 | GET | `/user/receipts` | 그 달 영수증. 월 합계는 영수증 품목만, 평균은 최근 6개월 월 합계 평균 | `?month=2026-11(최근 6개월 안)` | `200 { month, totalAmount, averageDiffAmount, weeks: [{ label, startDate, endDate, totalAmount, receipts: [{ id, storeCode, storeName, purchasedOn, itemCount, totalAmount }] }] }` | receipts, receipt_items |
| 23 | GET | `/user/receipts/{id}` | 영수증 하나 | `-` | `200 { id, storeCode, storeName, purchasedOn, imageUrl, items: [{ name, categoryCode, imageUrl, storage, amount, unit, price }], totalAmount }` | receipts, receipt_items |
| 24 | DELETE | `/user/receipts/{id}` | 영수증·품목·이미지 삭제. 식비·소비 패턴에서도 빠지고 냉장고 재료는 남음(ERD 답 1) | `-` | `204` | receipts, receipt_items, ingredients |
| 25 | GET | `/user/expenses/patterns` | 그 달 소비. 영수증 품목 + 영수증 없이 가격을 넣은 재료(등록일 기준, ERD 답 3). `discardedAmount`는 그 달 버린 금액(가격 있는 `cost_amount` 합계, #248) | `?month=2026-11(최근 6개월 안)` | `200 { month, totalAmount, averageDiffAmount, discardedAmount, categories: [{ categoryCode, amount, ratio }], dailyTotals: [{ date, amount }] }` | receipt_items, manual_ingredient_expenses |
| 26 | GET | `/user/expenses/daily-items` | 고른 날의 품목 | `?date=2026-11-20` | `200 { date, totalAmount, items: [{ name, categoryCode, imageUrl, storage, amount, unit, price }] }` | receipt_items, manual_ingredient_expenses |
| 72 | GET | `/user/expenses/discarded-items` | p3.4.1 버린 식재료(#248). 그 달 `DISCARDED` 기록 하나당 한 줄(냉장고 UseCase), 최근에 버린 것부터. `discardedOn`은 버린 날(KST), `amount`는 `cost_amount`(가격이 없으면 null), `totalAmount`는 null이 아닌 금액의 합으로 API 25 `discardedAmount`와 같음. 기록이 없는 달은 200 + 빈 목록. 월 생략 시 이번 달 | `?month=2026-11(최근 6개월 안)` | `200 { month, totalAmount, items: [{ name, categoryCode, discardedOn, amount }] }` | ingredient_logs |

API 25의 월을 생략하면 한국 시간 기준 이번 달을 조회해요. API 25·26·61은 이번 달을 포함한 최근 6개 달을 조회하고, 기록 없는 날은 합계 0과 빈 품목 목록을 돌려줘요. 카테고리 비율은 0~1이에요. API 22와 같이 평균에서는 이번 달과 기록이 없는 달을 제외해요.

API 20 응답의 `purchaseQuantity`와 `volume`은 #72에서 반영 예정이며, #101에서 프론트 연동을 확인해요.

### 영수증 수량 계약 (#173)

- API 20의 `amount`와 `unit`은 미리보기예요. 확인된 구매 수량은 `purchaseQuantity`, 포장 용량은 `volume { amount, unit, scope }`로 받아요. 용량 단위는 `G`, `KG`, `ML`, `L`이에요. 수량을 정할 수 없으면 확인 화면에서 값을 입력해요.
- API 21은 같은 구매 수량과 용량 필드를 받아요. `PER_PACK`이면 팩당 용량에 구매 수량을 곱하고, `TOTAL`이면 전체 용량을 한 번만 반영해요.
- 사용자가 최종 수량을 직접 입력하면 `amount`를 보내고, 서버는 API 14와 같은 사전 단위로 저장해요. 자동 계산 결과나 `PACK` 미리보기대로 등록할 때는 `amount`를 생략해요.
- 사전 단위가 무게·부피일 때 같은 종류의 용량을 환산해요. 용량이 없거나 종류가 달라 환산할 수 없고 구매 수량을 알 수 있으면 `PACK`에 구매 수량을 저장해요. 사전 단위가 개수 계열이면 구매 수량을 사용해요.
- 저장 수량을 정할 수 없으면 400 `INVALID_INPUT`의 `fieldErrors`에 `items[i].purchaseQuantity`를 내려줘요. 예를 들어 사전 단위가 `G`일 때 팩당 500g × 2는 `1000 G`, 전체 500g은 `500 G`, 팩당 500mL × 1은 `1 PACK`, 직접 입력한 `amount: 600`은 `600 G`예요.

등록한 영수증을 고치는 API는 없어요. 이전의 `GET /user/expenses/monthly`·`/monthly/analysis`는 API 22·25에 합쳤어요.

## 테이블

`receipts`, `receipt_items`. 등록할 때 `ingredients`(02)와 `cart_items`(07)도 바꿔요. 컬럼은 [`../erd.md`](../erd.md)를 보세요.

## 구현 메모

- 흐름: 스캔 → `POST /user/receipts/scan`(동기, 최대 30초, 저장 안 함) → OCR 결과 화면에서 수정·추가(화면 상태로만, 빼기 없음) → `POST /user/receipts`로 영수증·품목·냉장고 재료를 한 트랜잭션에 저장.
- 등록 전 OCR 결과는 서버에 저장하지 않아요. 등록하지 않고 나가면 `receipt_result_abandoned` 이벤트만 남아요.
- 결과에는 식재료만 남겨요. 비식재료(수세미·냄비 등)는 응답에 나오지 않아요. 소비기한이 없으면 카테고리 기본 보관 일수로 채워요(별도 추정 표시 없음, 안내 문구만).
- 단위는 서버가 재료명으로 정해요(02의 재료 사전).
- OCR 실패 화면 하나에 문구만 달라요: `RECEIPT_NOT_RECOGNIZED`·`OCR_UPSTREAM_ERROR`·`OCR_TIMEOUT`, `RECEIPT_NO_FOOD_ITEMS`, `RECEIPT_TOO_OLD`, `RECEIPT_DUPLICATE`. OCR은 재시도하지 않아요.
- 중복 영수증: 판매처(지점명)·영수증 번호·구매 시각이 모두 같으면 중복. 번호를 못 읽었으면 판매처·구매일·식재료 이름 목록, 시각을 못 읽었으면 구매일로 비교해요. 스캔 때 확인하고 등록 때 한 번 더 확인해요(409).
- 구매일·판매처를 못 읽으면 비워서 주고, 등록 때 둘 다 필수예요. 구매일이 6개월 지났으면 스캔 단계에서 실패로 돌려줘요.
- 영수증 품목별 `cartDeductions`는 생략·빈 배열 또는 1개만 허용해요. 영수증 쪽은 양수 수량과 품목별 실제 냉장고 등록량 이내인지를 검증한 뒤, 최종 재료명·서버 결정 단위로 07의 `DeductCartItemsUseCase`를 같은 트랜잭션에서 호출해요. 반환값 `cartRemovedCount`는 0이 되어 실제 삭제된 행 수예요. 상세 계약은 [공유·장바구니 가이드](07_shared-cart.md#영수증-연동-계약57)를 보세요.
- 월 합계는 그 달 영수증의 식재료 품목 금액(구매일 기준). 평균은 최근 6개월 월 합계 평균과의 차이예요(이번 달 제외 등 세부는 제안).
- 소비 패턴은 영수증 품목 + 영수증 없이 가격을 넣은 재료(등록일 기준)예요. 등록 후 가격 수정도 반영하고 날짜는 등록일이에요. 가격 없는 재료는 빼고, 버린 재료는 이미 산 것이라 포함해요.
- 판매처 이름을 14종 코드에 맞춰 `store_code`로 저장하고, 못 맞추면 비워요(기본 아이콘). 마트 배지는 영수증 라우트 안 코드 컴포넌트예요.
- 영수증 이미지 저장소 종류는 백엔드가 정하고, 응답 이미지 주소는 서버가 만들어요. 매일 새벽 정리 작업이 구매일 6개월 지난 영수증을 이미지와 함께 지워요.
- 영수증 스캔은 횟수 제한이 없어요. 비용·성공률은 이벤트로 봐요.

## 지금 코드와 다른 점

- 아직 이 영역 코드는 없어요. 이전 가이드의 `duplicateSuspected`(의심 표시)는 없어지고 중복은 막아요.
- `receipts`에 `receipt_number`·`purchased_time`이 새로 있어요(중복 비교용).

## 에러 코드

`INVALID_INPUT`, `RECEIPT_NOT_RECOGNIZED`, `OCR_UPSTREAM_ERROR`, `OCR_TIMEOUT`, `RECEIPT_NO_FOOD_ITEMS`, `RECEIPT_TOO_OLD`, `RECEIPT_DUPLICATE`, `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, `INGREDIENT_CATEGORY_NOT_FOUND`, `RECEIPT_NOT_FOUND`. 공통 코드(`UNAUTHORIZED`, `TOKEN_EXPIRED`, `RATE_LIMITED`, `INTERNAL_ERROR`, `SERVICE_UNAVAILABLE`)는 모든 API에 나올 수 있어요. API별 코드와 화면 표시는 [`docs/agent/spec/errors.md`](../../agent/spec/errors.md)에 있어요.

## 발행 이벤트

발송 FE는 화면 담당 프론트가 API 56으로 보내고, BE는 이 영역 담당이 발행 API가 끝나는 지점에서 `EventTracker`로 기록해요([08](08_event-tracking.md)).

| ID | 이벤트 | 발생 시점 | 속성 | 발송 | 우선순위 | 발행 API |
| --- | --- | --- | --- | --- | --- | --- |
| E28 | `receipt_scan_started` | 촬영하거나 갤러리 이미지를 골랐을 때 | scan_method | FE | P0 | 56 POST /user/events(프론트 전송) |
| E30 | `receipt_scan_succeeded` | OCR이 끝나 식재료 결과가 나왔을 때 | item_count, food_item_count, duration_ms | BE | P0 | 20 영수증 스캔 |
| E31 | `receipt_scan_failed` | OCR이 실패했을 때(영수증 아님, OCR 오류·시간 초과, 식재료 없음, 6개월 지남, 이미 등록한 영수증) | error_code, duration_ms | BE | P0 | 20 영수증 스캔 |
| E32 | `receipt_item_edited` | OCR 재료 수정 화면에서 품목을 고쳤을 때(항목마다 1건) | item_index, edit_field | FE | P0 | 56 POST /user/events(프론트 전송) |
| E33 | `receipt_item_added_manually` | OCR 결과에서 빠진 품목을 직접 추가했을 때 | 공통 속성만 | FE | P1 | 56 POST /user/events(프론트 전송) |
| E34 | `receipt_saved` | "식재료 N개 등록 하기"로 영수증이 저장됐을 때 | receipt_id, registered_count, ocr_item_count, edited_item_count, deleted_item_count, added_item_count, duration_sec, food_total_amount, cart_removed_count | BE | P0 | 21 영수증 등록 |
| E76 | `receipt_result_abandoned` | OCR 결과 화면에서 등록하지 않고 나갔을 때(뒤로·닫기·앱 종료 직전). 등록 전 결과는 서버에 저장하지 않아 OCR 정확도를 이것으로 잼 | ocr_item_count, edited_item_count, deleted_item_count, added_item_count | FE | P0 | 56 POST /user/events(프론트 전송) |
| E35 | `receipt_list_viewed` | 영수증 화면이 열리거나 달을 바꿨을 때 | month_offset, has_comparison | FE | P1 | 56 POST /user/events(프론트 전송) |
| E36 | `receipt_detail_viewed` | 영수증 상세가 열릴 때 | 공통 속성만 | FE | P2 | 56 POST /user/events(프론트 전송) |
| E37 | `receipt_deleted` | 영수증 삭제 모달에서 삭제했을 때 | 공통 속성만 | FE | P1 | 56 POST /user/events(프론트 전송) |
| E38 | `consumption_pattern_viewed` | 나의 소비 화면이 열리거나 달을 바꿨을 때 | month_offset | FE | P0 | 56 POST /user/events(프론트 전송) |

## 체크리스트

- [ ] 스캔 → 수정 → 등록 한 트랜잭션, 등록 전 미저장
- [ ] OCR 실패 4가지 문구, 6개월 지난 영수증 거부
- [ ] 중복 영수증(번호·시각 없을 때 포함) 스캔·등록 둘 다 막기
- [ ] 장바구니 선택 차감·생략·반환 삭제 행 수·전체 롤백
- [ ] 월 합계·평균 차이·주차 묶음, 소비 패턴 범위(가격 있는 직접 추가 포함)
- [ ] 영수증 삭제 뒤 냉장고 재료 유지, 6개월 정리 작업
