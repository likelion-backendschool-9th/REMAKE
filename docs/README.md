# docs 폴더 안내

REMAKE의 개발 문서를 모아 둔 폴더입니다. 같은 내용을 **AI 에이전트용(영문)** 과 **개발자용(한글)** 두 벌로 관리합니다.

## 폴더 구조

```
docs/
├─ README.md                    이 문서
├─ agent/                       AI 에이전트용 (영문)
│  ├─ git-convention.md         브랜치·커밋·PR 규칙
│  ├─ backend-convention.md     백엔드 코드 규칙 (헥사고날 구조, API 경로, 에러 형식, AI 생성, 이벤트 발행, 예약 작업)
│  ├─ backend-testing.md        백엔드 테스트 작성법 (공통 통합 테스트 설정, Testcontainers)
│  ├─ erd.md                    ERD·테이블 정의서 (테이블 32개, 컬럼, 관계, 구현 규칙)
│  ├─ ci.md                     PR에서 도는 CI와 결과 읽는 법
│  ├─ deployment.md             배포 구조, 서버 준비, 배포·롤백, 앱이 지켜야 할 배포 조건
│  └─ spec/                     기획 문서 영문 요약 (영문만 있음)
│     ├─ domain-rules.md        비즈니스 규칙: 횟수 제한, 단위 14개, 카테고리 12개, 소비기한 상태, 영수증·식단표·공유 규칙
│     ├─ api-summary.md         도메인별 전체 API와 각 API가 발행하는 이벤트
│     ├─ errors.md              에러 응답 형식과 에러 코드 59개 + 공통 4개
│     └─ events.md              이벤트 트래킹 규칙과 이벤트 76개
└─ developer/                   개발자용 (한글)
   ├─ agent-guide.md            루트 AGENTS.md 번역 (레포 공통 규칙, 제품 규칙, 문서 규칙)
   ├─ git-convention.md         agent/git-convention.md 번역
   ├─ backend-convention.md     agent/backend-convention.md 번역
   ├─ backend-testing.md        agent/backend-testing.md 번역
   ├─ erd.md                    agent/erd.md 번역
   ├─ ci.md                     agent/ci.md 번역
   ├─ deployment.md             agent/deployment.md 번역
   ├─ frontend-agent-guide.md   frontend/AGENTS.md 번역 (프론트 필수 규칙)
   ├─ frontend-blueprint.md     frontend/blueprint.md 번역 (프론트 스택, 폴더, 라우트, 데이터 흐름, UI 정책, 이벤트 트래킹)
   └─ guide/                    영역별 구현 가이드 (한글만 있음)
      ├─ README.md              가이드 목록과 읽는 순서, 기획 원본 안내
      ├─ 00_common-guide.md     전원: 스택, API·에러 공통 규칙, 패키지 담당
      ├─ 01_auth-onboarding.md  영역 1: 시작하기·소셜 로그인
      ├─ 02_fridge.md           영역 2: 냉장고
      ├─ 03_receipt-ocr-spending.md  영역 3: 영수증·OCR·소비 패턴
      ├─ 04_ai-recipe.md        영역 4: AI 레시피
      ├─ 05_meal-plan.md        영역 5: 식단표
      ├─ 06_notification-admin.md    영역 6: 알림·어드민
      ├─ 07_shared-cart.md      영역 7: 공유 공간·장바구니
      └─ 08_event-tracking.md   전원: 이벤트 트래킹
```

`docs/` 밖에도 문서가 있습니다. 에이전트가 그 자리에서 읽어야 해서 원래 위치에 둡니다.

| 파일 | 내용 | 한글판 |
| --- | --- | --- |
| `AGENTS.md` (레포 루트) | 레포 공통 규칙: 브랜치·커밋·PR, 검사 명령, 제품 규칙, 문서 규칙 | `developer/agent-guide.md` |
| `frontend/AGENTS.md` | 프론트 필수 규칙 | `developer/frontend-agent-guide.md` |
| `frontend/blueprint.md` | 프론트 설계 문서 | `developer/frontend-blueprint.md` |
| `CLAUDE.md` (루트, frontend) | `@AGENTS.md` 한 줄. Claude Code가 같은 규칙을 읽게 하는 파일 | 없음 |

## agent와 developer의 차이

| | `agent/` | `developer/` |
| --- | --- | --- |
| 읽는 사람 | AI 코딩 에이전트 (Claude Code, Codex 등) | 팀 개발자 |
| 언어 | 영어 | 한국어 |
| 쓰는 방식 | 명령형 규칙, 정확한 명령어와 경로 | 같은 내용을 자연스러운 한국어로 설명 |

- 두 폴더의 같은 이름 파일은 **짝**입니다. 한쪽을 고치면 같은 커밋에서 다른 쪽도 고칩니다(루트 `AGENTS.md` §5).
- 짝이 없는 예외가 두 개 있습니다.
  - `agent/spec/`(영문만): 노션·구글 시트의 한글 기획 문서를 에이전트용으로 요약한 문서입니다. 개발자는 기획 원본을 봅니다.
  - `developer/guide/`(한글만): 개발자가 자기 영역을 구현할 때 펼쳐 보는 가이드입니다. 에이전트는 같은 정책을 `agent/spec/`에서 읽습니다.
- 기획 원본(PRD, 정책정의서, API·에러·이벤트 정의서, WBS)은 레포에 없습니다. 노션/구글 시트를 참고합니다.

## 무엇을 따르나

- **정책·데이터**(테이블, 횟수 제한, 단위, 영수증 보관, 알림 종류, 에러 코드, 이벤트): 기획 원본 → `agent/spec/`·`erd.md` 순서로 따릅니다. 문서끼리 다르면 **피그마 > PRD > 정책정의서 > API·ERD·에러·이벤트 정의서 > 이 폴더의 요약**입니다.
- **코드 규칙**(폴더 구조, 헥사고날, 스타일, 인증 방식, 배포): 이 레포의 문서(`AGENTS.md`, `backend-convention.md`, `frontend/blueprint.md`, `deployment.md`)를 따릅니다.
- 모르겠으면 추측하지 말고 팀에 물어봅니다.

## 처음 온 사람이 읽는 순서

1. [`developer/agent-guide.md`](developer/agent-guide.md): 레포 구조, 브랜치·커밋·PR 규칙, 제품 규칙. 모두 읽습니다.
2. [`developer/git-convention.md`](developer/git-convention.md): 브랜치 만들기부터 PR까지 예시.
3. [`developer/guide/README.md`](developer/guide/README.md) → [`00_common-guide.md`](developer/guide/00_common-guide.md) → 내 영역 가이드(01~07) → [`08_event-tracking.md`](developer/guide/08_event-tracking.md).
4. 맡은 쪽 문서
   - 백엔드: [`backend-convention.md`](developer/backend-convention.md) → [`erd.md`](developer/erd.md) → [`backend-testing.md`](developer/backend-testing.md)
   - 프론트: [`frontend-agent-guide.md`](developer/frontend-agent-guide.md) → [`frontend-blueprint.md`](developer/frontend-blueprint.md)
5. PR을 올릴 때: [`developer/ci.md`](developer/ci.md). 배포나 환경 변수를 건드릴 때: [`developer/deployment.md`](developer/deployment.md).
