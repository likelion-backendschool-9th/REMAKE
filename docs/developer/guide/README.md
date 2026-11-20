# Remake 개발자 가이드 (한글)

2026-11-20 · 기준: 피그마(etTm84sh...) + PRD v3.1 + 정책정의서 v3.1 + ERD 32개 + API 정의서 v3.1 + 에러 정의서 + 이벤트 정의서

개발자가 구현할 때 펼쳐 보는 문서입니다. 기획 문서(노션/구글 시트 참고)를 대체하지 않고, "내 기능을 만들려면 뭘 알아야 하나"만 뽑아 놓았습니다. 기획 문서와 다르면 **피그마 > PRD > 정책정의서 > API·ERD·에러·이벤트 정의서 > 이 문서** 순서로 따르고, 모르겠으면 준호님께 물어보세요.

정책·데이터가 아닌 **코드 규칙**(폴더 구조, 스타일, 인증 방식, 배포)은 레포 문서를 따릅니다: [`../backend-convention.md`](../backend-convention.md), [`../frontend-blueprint.md`](../frontend-blueprint.md), [`../agent-guide.md`](../agent-guide.md). 테이블은 [`../erd.md`](../erd.md)를 봅니다.

AI 코딩 도구(Claude Code 등)는 루트 `AGENTS.md`와 영문 요약 [`docs/agent/spec/`](../../agent/spec/)을 읽습니다. 같은 정책을 짧게 정리한 것입니다.

## 읽는 순서

| 문서 | 대상 |
|---|---|
| [00_common-guide.md](00_common-guide.md) | 전원 (스택, API·에러·이벤트 공통 규칙, 폴더 구조) |
| [01_auth-onboarding.md](01_auth-onboarding.md) | 영역 1 이준호 |
| [02_fridge.md](02_fridge.md) | 영역 2 김재연 |
| [03_receipt-ocr-spending.md](03_receipt-ocr-spending.md) | 영역 3 이동준 |
| [04_ai-recipe.md](04_ai-recipe.md) | 영역 4 김종수 |
| [05_meal-plan.md](05_meal-plan.md) | 영역 5 한윤수 |
| [06_notification-admin.md](06_notification-admin.md) | 영역 6 이상호 |
| [07_shared-cart.md](07_shared-cart.md) | 영역 7 최유리 |
| [08_event-tracking.md](08_event-tracking.md) | 전원 (자소서에 쓰는 핵심 기능) |

프론트(화면) 작업은 준호님이 화면을 보고 직접 배분합니다. 화면 폴더 이름은 각 영역 문서의 "화면" 표를 보세요.

## 같이 보는 원본

원본은 레포에 없습니다. 노션/구글 시트 참고.

- PRD, 정책정의서: 무엇을·왜 (노션/구글 시트 참고)
- API 정의서 v3, API 예외·에러 정의서: 경로, 에러 코드 59개 (노션/구글 시트 참고. 코드에만 있는 공통 4개는 `docs/agent/spec/errors.md`)
- 테이블 정의서, ERD: 테이블 32개 (노션/구글 시트 참고, 레포에는 [`../erd.md`](../erd.md))
- 이벤트 데이터 정의서: 이벤트 76개, 속성, 지표 (노션/구글 시트 참고)
- WBS: 일정·담당 (노션/구글 시트 참고)
