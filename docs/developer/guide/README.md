# Remake 개발자 가이드 (한글)

2026-11-22 · 기준: 피그마 + PRD v4 + 정책정의서 v4 + ERD v4(26개) + API 정의서 v4(60개) + 에러 정의서 v2(59개) + 이벤트 정의서 v2(79개). 모두 피그마 기준이에요.

개발자가 구현할 때 펼쳐 보는 문서입니다. 기획 문서(노션)를 대체하지 않고, "내 기능을 만들려면 뭘 알아야 하나"만 뽑아 놓았습니다. 정책·API·이벤트 표는 원본에서 그대로 옮겼고 피그마 노드 근거 칸만 뺐습니다. 기획 문서와 다르면 **피그마 > PRD > 정책정의서 > API·ERD·에러·이벤트 정의서 > 이 문서** 순서로 따르고, 모르겠으면 준호님께 물어보세요.

정책·데이터가 아닌 **코드 규칙**(폴더 구조, 스타일, 인증 방식, 배포)은 레포 문서를 따릅니다: [`../backend-convention.md`](../backend-convention.md), [`../frontend-blueprint.md`](../frontend-blueprint.md), [`../agent-guide.md`](../agent-guide.md). 테이블은 [`../erd.md`](../erd.md)를 봅니다.

AI 코딩 도구(Claude Code 등)는 루트 `AGENTS.md`와 영문 요약 [`docs/agent/spec/`](../../agent/spec/)을 읽습니다. 같은 정책을 영어로 정리한 것입니다.

## 읽는 순서

| 문서 | 대상 |
|---|---|
| [00_common-guide.md](00_common-guide.md) | 전원 (스택, API·에러·이벤트 공통 규칙, 폴더 구조) |
| [01_auth-onboarding.md](01_auth-onboarding.md) | 영역 1 이준호 (로그인·온보딩·설정·구독) |
| [02_fridge.md](02_fridge.md) | 영역 2 김재연 |
| [03_receipt-ocr-spending.md](03_receipt-ocr-spending.md) | 영역 3 이동준 |
| [04_ai-recipe.md](04_ai-recipe.md) | 영역 4 김종수 |
| [05_meal-plan.md](05_meal-plan.md) | 영역 5 한윤수 |
| [06_notification-admin.md](06_notification-admin.md) | 영역 6 이상호 (알림·어드민 운영). 어드민 모니터링 백엔드는 이준호 |
| [07_shared-cart.md](07_shared-cart.md) | 영역 7 최유리 |
| [08_event-tracking.md](08_event-tracking.md) | 전원 (포트폴리오 핵심 기능) |

프론트(화면) 작업은 준호님이 화면을 보고 직접 배분합니다. 화면 코드는 각 영역 문서의 "화면" 표를 보세요.

## 같이 보는 원본

원본은 레포에 없습니다. 노션 "remake › 문서"에 문서별 버전 페이지로 있어요.

- PRD v4, 정책정의서 v4: 무엇을·왜
- API 정의서 v4: API 60개(영문 요약 [`api-summary.md`](../../agent/spec/api-summary.md))
- 에러·예외 정의서 v2: 에러 코드 59개(영문 요약 [`errors.md`](../../agent/spec/errors.md))
- ERD v4: 테이블 26개(레포에는 [`../erd.md`](../erd.md))
- 이벤트 정의서 v2: 이벤트 79개, 속성 102개, 지표 21개, 화면 코드 41개(영문 요약 [`events.md`](../../agent/spec/events.md))
- WBS v2: 일정·담당
