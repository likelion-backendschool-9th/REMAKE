# 식약처 레시피 조합 검사 기준 만들기 (#288)

식품의약품안전처 조리식품의 레시피 DB(`COOKRCP01`) 1,156개로 식단표 조합 검사 기준 JSON(`backend/src/main/resources/mfds/combination-check.json`)을 만든다.
식단표 AI가 만든 레시피에 "토마토초코밥"처럼 식약처 레시피에서 한 번도 같이 쓰인 적 없는 짝이 주의 재료(과일·음료·단것·요거트·견과류)와 함께 나오면 서버가 형식 오류로 보고 한 번 다시 부른다.

## 원본 받기

원본 CSV는 저장소에 올리지 않는다. [식품안전나라 공공데이터](https://www.foodsafetykorea.go.kr/api/openApiInfo.do?svc_no=COOKRCP01)에서 조리식품의 레시피 DB를 CSV로 내려받는다(1,156개, UTF-8).

## 다시 만들기

저장소 루트에서:

```bash
python3 scripts/mfds/build_combination_check.py <내려받은 CSV(.csv 또는 .csv.gz)>
```

- 결과 JSON은 `backend/src/main/resources/mfds/combination-check.json`에 덮어쓴다. 백엔드는 앱이 켜질 때 이 파일을 읽는다(마이그레이션 없음).
- 재료 사전은 `backend/src/main/resources/db/migration/V5__create_ingredient_catalog.sql`에서 읽는다. 사전 마이그레이션이 바뀌면 다시 돌린다(`MealPlanCombinationCatalogSyncTest`가 DB 사전과 다르면 실패한다).
- 판정 비교용 기대값 `backend/src/test/resources/mfds/combination-check-expected.json`도 함께 쓴다: 식약처 재료 이름 전부와 예시 이름의 검사 키, 예시(`check_examples.json`)와 무작위 레시피 300개의 걸린 짝 수. Java 테스트가 같은 판정인지 비교하므로, 이름 맞추기를 고치면 파이썬과 Java를 함께 고친다.

## 표 파일

| 파일 | 내용 |
| --- | --- |
| `check_rules.json` | 무리 묶음(김치류·돼지고기 부위·면류·빵류·견과류 등, 한 재료로 셈), 검사에서 뺄 이름(물·생수·육수), 주의 재료(과일·음료 카테고리 + 단것·요거트·견과류), 아침 재료(서로 짝이 처음이어도 허용), AI에 보내지 않을 냉장고 재료 18개. 주의·아침 목록에 무리 안 재료를 적으면 무리 전체가 그렇다 |
| `name_fixes.tsv` | 재료 이름 → 재료 사전 이름과 카테고리. 사전 이름이 들어 있어 잘못 맞춰지는 이름(콜라비 → 콜라 등)은 `-`로 연결하지 않는다 |
| `synonyms.tsv` | 사전 안의 같은 재료 다른 표기(계란 → 달걀) |
| `check_examples.json` | 판정 비교용 예시(엉터리 16개·멀쩡한 14개·이름·재료 검사와 고친 이름 20개) |

## 재료 이름 맞추기

식약처 재료 줄과 AI 레시피 재료 줄에 같은 규칙을 쓴다(서버 `MealPlanCombinationRules`와 같다).

1. 4글자 이하 "○○밥"(볶음밥 아님)은 쌀로 본다.
2. 고침표(`name_fixes.tsv`, 공백 뺀 이름·괄호 뺀 이름) → 사전 이름과 같음 → 사전 이름(두 글자 이상)이 들어 있으면 가장 긴 것. 길이가 같으면 이름에서 더 뒤에 있는 것(한국어 합성어는 뒤가 중심: 당근주스 → 주스, 멸치간장 → 간장).
3. 같은 재료 다른 표기는 기준 이름으로, 무리에 든 재료는 무리 이름으로 바꾼다.
4. 사전과 못 맞춘 이름, 양념(SEASONING), 물·생수·육수는 검사하지 않는다.

## JSON 모양

| 칸 | 뜻 |
| --- | --- |
| `catalog` | 재료 사전 이름 → 카테고리(V5에서 읽은 사본) |
| `synonyms`·`aliases`·`groups`·`skippedNames` | 위 이름 맞추기 규칙의 표 |
| `dishNames` | 요리 이름인 사전 항목(두부조림·샐러드·주스 등). 요리 이름·재료 검사에서 재료로 보지 않는다 |
| `titleOnlyTerms` | 사전에 없지만 요리 이름에서 재료 낱말로 읽는 것(새우젓·액젓) |
| `stapleGroups` | 이름을 고칠 때 앞에 붙일 주재료로 고르지 않는 무리(쌀·빵·면) |
| `breakfast` | 아침 재료. 두 재료가 모두 여기 있으면 처음 보는 짝이어도 허용한다(바나나토스트, 우유 시리얼, 초코 바나나 토스트) |
| `excludedFromAi` | 식약처 요리(후식 제외)에 한 번도 안 나오는 재료. 냉장고에 있어도 식단표 AI에 보내지 않는다 |
| `partners` | 주의 재료(검사 키) → 식약처 레시피에서 함께 쓰인 검사 키. 주의 재료 목록이기도 하다 |
