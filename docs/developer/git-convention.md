# Git 컨벤션

> 에이전트용 영문판: [`docs/agent/git-convention.md`](../agent/git-convention.md)

## 1. 브랜치 구성

- `main`: 최종 배포 가능한 안정 버전을 관리하는 브랜치
- `dev`: 팀 개발 내용을 통합하고 테스트하는 브랜치
- 작업 브랜치: 기능 개발, 버그 수정, 환경 설정 및 문서 작업을 진행하는 브랜치

작업 브랜치는 작업 유형에 따라 다음과 같이 생성한다. 이름은 `<유형>/<작업-내용>` 형식이며, 작업 내용은 영어 소문자 kebab-case로 짧게 쓴다.

| 유형 | 사용 목적 | 예시 |
| --- | --- | --- |
| `feat` | 신규 기능 개발 | `feat/login-implementation` |
| `fix` | 버그 수정 | `fix/login-error` |
| `chore` | 환경 설정 및 기타 유지보수 | `chore/eslint-config` |
| `docs` | 문서 작성 및 수정 | `docs/git-convention` |

## 2. 작업 흐름

1. 최신 `dev` 브랜치로 이동하고 변경 사항을 가져온다.
2. 작업 내용에 맞는 유형의 작업 브랜치를 생성한다.
3. 작업을 진행하고 커밋 규칙에 맞게 커밋한다.
4. 작업 브랜치를 원격 저장소에 Push한다.
5. 작업 브랜치에서 `dev`를 대상으로 PR을 생성한다.
6. 변경 사항을 확인하고 테스트한 뒤 `dev`에 Merge한다.
7. 개발이 완료되면 `dev`를 `main`에 Merge한다.

## 3. 작업 규칙

- `main`, `dev` 브랜치에 직접 커밋하지 않는다.
- 작업 내용에 맞는 브랜치 유형을 선택한다.
- 하나의 브랜치에서는 하나의 주요 작업을 진행한다.
- 브랜치 이름은 브랜치 컨벤션을 따른다.
- 커밋 메시지는 커밋 컨벤션을 따른다.
- 가능하면 브랜치 유형과 커밋 유형을 일치시킨다.
- 모든 작업 브랜치는 `dev`를 대상으로 PR을 생성한다.
- Merge 전에 변경 사항을 확인하고 테스트한다.
- Merge가 완료된 작업 브랜치는 삭제한다.
- 2026년 12월 10일에 끝나는 짧은 프로젝트이므로 별도의 `release`, `hotfix` 브랜치는 사용하지 않는다.
- 기능 개발이나 버그 수정은 시작하기 전에 이슈로 등록한다(7장 참고). 이슈 하나 = 브랜치 하나 = PR 하나.

## 4. 작업 예시

### 4-1. 신규 기능 개발 (feat)

```bash
git switch dev
git pull origin dev
git switch -c feat/login-implementation

# 작업 후 커밋
git add .
git commit -m "feat: 로그인 구현"

# 원격 저장소에 Push
git push -u origin feat/login-implementation
```

### 4-2. 버그 수정 (fix)

```bash
git switch dev
git pull origin dev
git switch -c fix/login-error

# 작업 후 커밋
git add .
git commit -m "fix: 로그인 오류 수정"

# 원격 저장소에 Push
git push -u origin fix/login-error
```

### 4-3. 환경 설정 및 유지보수 (chore)

```bash
git switch dev
git pull origin dev
git switch -c chore/eslint-config

# 작업 후 커밋
git add .
git commit -m "chore: ESLint 설정 변경"

# 원격 저장소에 Push
git push -u origin chore/eslint-config
```

### 4-4. 문서 작성 및 수정 (docs)

```bash
git switch dev
git pull origin dev
git switch -c docs/git-convention

# 작업 후 커밋
git add .
git commit -m "docs: Git 컨벤션 작성"

# 원격 저장소에 Push
git push -u origin docs/git-convention
```

위 예시는 짧게 쓰려고 `git add .`를 썼다. 실제로는 이번 변경과 관련된 파일만 골라서 stage한다(루트 `AGENTS.md` 2장).

## 5. PR 및 Merge 규칙

- 모든 작업 브랜치는 `dev`를 대상으로 PR을 생성한다.
- PR 제목은 커밋 컨벤션과 동일한 유형을 사용한다.
- PR 내용에는 작업 목적, 주요 변경 사항 및 테스트 결과를 작성한다. 본문은 [PR 템플릿](../../.github/pull_request_template.md)의 항목(변경 내용, 관련 이슈, 테스트, 리뷰 요청)을 모두 채운다.
- `dev` 또는 `main` 대상 PR은 병합 전에 `Backend CI required`, `Frontend CI required`, `Deploy CI required` 검사를 통과해야 한다.
- 세 검사는 모든 PR에 결과를 보고한다. 해당 영역에 변경이 없으면 백엔드, 프론트엔드, 배포 설정 또는 워크플로 검사 잡은 건너뛰므로, 브랜치 룰셋에는 개별 잡을 필수 검사로 등록하지 않는다. 각 검사가 하는 일은 [`ci.md`](ci.md)를 본다.
- 팀원 리뷰 및 확인이 완료되면 Merge한다.
- Merge가 완료되면 원격 및 로컬 작업 브랜치를 삭제한다.

| 작업 브랜치 | PR 대상 |
| --- | --- |
| `feat/*` | `dev` |
| `fix/*` | `dev` |
| `chore/*` | `dev` |
| `docs/*` | `dev` |

- 이슈가 있는 작업이면 PR 본문의 관련 이슈에 `Closes #번호`를 적는다. PR이 `dev`에 머지되면 이슈가 자동으로 닫힌다.

## 6. 커밋 컨벤션

### 작성 형식

```
<유형>: <작업내용>
```

유형과 작업 내용은 콜론(`:`)으로 구분하고, 콜론 뒤에는 공백을 한 칸 둔다.

제목 아래에는 빈 줄을 하나 두고 본문을 쓴다. 모든 커밋에 본문을 작성한다.

```
<유형>: <작업내용>

- 무엇을 왜 바꿨는지
- 눈에 띄는 변경마다 한 줄씩
```

### 커밋 유형

| 유형 | 사용 목적 | 예시 |
| --- | --- | --- |
| `feat` | 신규 기능 개발 | `feat: 로그인 구현` |
| `fix` | 버그 수정 | `fix: 로그인 오류 수정` |
| `chore` | 환경 설정 및 기타 유지보수 | `chore: ESLint 설정` |
| `docs` | 문서 작성 및 수정 | `docs: Git 컨벤션 작성` |

### 작성 규칙

- `feat`, `fix`, `chore`, `docs` 중 하나를 사용한다.
- 작업 내용을 짧고 명확하게 작성한다.
- 하나의 커밋에는 하나의 주요 변경 사항을 담는다.
- `수정`, `작업`, `update`처럼 의미가 모호한 메시지는 사용하지 않는다.
- 본문은 한국어 목록(`- `)으로 변경 내용과 이유를 적는다. 제목을 반복하지 않는다.
- 커밋 메시지와 PR 본문에 AI 도구의 공동 작성자 표기(`Co-Authored-By: Claude ...`)나 `Generated with ...` 문구를 넣지 않는다. `PR Attribution Check` CI에서 실패한다.

### 예시

```
feat: 프로젝트 등록 기능 구현
fix: 로그인 오류 수정
chore: ESLint 설정 변경
docs: Git Flow 문서 작성
```

본문까지 쓴 예:

```
fix: 소비기한 D-day 계산 오류 수정

- 자정 기준이 아니라 현재 시각 기준으로 계산해 하루씩 밀리던 문제 수정
- dayjs startOf('day')로 날짜만 비교하도록 변경
```

## 7. 이슈와 라벨

### 이슈를 만드는 때

- 기능 개발, 버그 수정, 팀이 정해야 하는 결정은 시작하기 전에 이슈를 만든다. 작은 `chore`·`docs` 변경은 바로 PR을 올려도 된다.
- 이슈 하나 = 브랜치 하나 = PR 하나. PR이 여러 개 필요한 이슈는 이슈를 나눈다.
- 브랜치 이름은 그대로 `<유형>/<작업-내용>`(1장)이다. 이슈는 PR에서 `Closes #번호`로 연결한다.
- 담당자(Assignee)는 실제로 작업하는 사람으로 지정한다.

### 템플릿

이슈는 `.github/ISSUE_TEMPLATE/`의 템플릿으로 만든다. 빈 이슈는 만들 수 없게 막아 두었다.

| 템플릿 | 제목 | 쓰는 때 |
| --- | --- | --- |
| 기능 개발 | `[feat] <한글 설명>` | 화면이나 API 기능. 영역, 파트, 피그마 프레임, API, 테이블, 에러 코드, 이벤트를 채운다 |
| 버그 | `[fix] <한글 설명>` | 정의서와 다르게 동작하는 것. 재현 순서, 에러 `code`, `traceId`를 적는다 |
| 결정 요청 | `[decision] <한글 설명>` | 정의서에 "제안"·"미결"로 표시된 정책, PR 리뷰에서 나온 질문. 선택지와 결정할 사람을 적는다 |
| 설정·문서 작업 | `[chore] <한글 설명>` 또는 `[docs] <한글 설명>` | 설정, CI, 문서 작업 |

- 제목에는 커밋 컨벤션(6장)의 유형 단어를 쓴다. 설명은 한글로 짧고 구체적으로 쓴다.
- 피그마 프레임은 피그마에 보이는 이름 그대로 적는다(예: `p2.1-재료 수정`). 프레임 번호를 정리하는 중이라, 프레임 이름이 바뀌면 그 프레임을 적은 열린 이슈도 고친다.
- 에러 로그를 붙일 때도 개인정보, 토큰, 비밀키는 이슈에 넣지 않는다.

### 후속 작업

머지된 PR은 닫혀서 묻히기 쉽다. PR 뒤에 이어서 해야 하는 작업은 이슈로 남긴다.

1. PR을 올린 사람이 머지 전에 후속 작업마다 이슈를 만든다. 기본은 버그 템플릿(`fix`)이고, 팀이 먼저 정해야 하는 것은 결정 요청 템플릿(`decision`)을 쓴다.
2. 다음 작업을 할 사람(보통 그 영역 담당자)을 담당자(Assignee)로 지정하고, 이슈 본문에서 `@아이디`로 태그한다.
3. 이슈에 PR 번호를 적는다(예: `#38`). 버그 템플릿은 "이어지는 PR", 결정 요청 템플릿은 "관련 이슈·PR"에 적는다.
4. PR 본문의 관련 이슈에 `후속: #번호`를 적는다. `Closes`를 쓰면 이 PR이 머지될 때 후속 이슈까지 닫히므로 쓰지 않는다.
5. 후속 작업을 하는 PR이 `Closes #번호`로 그 이슈를 닫는다.

### 라벨

| 묶음 | 라벨 | 규칙 |
| --- | --- | --- |
| 유형 | `feat`, `fix`, `chore`, `docs`, `decision` | 꼭 하나. 제목 머리말과 같게 |
| 영역 | `area:1-auth`, `area:2-fridge`, `area:3-receipt`, `area:4-recipe`, `area:5-meal-plan`, `area:6-notification-admin`, `area:7-share-cart`, `area:common` | 하나 이상. 영역 번호는 WBS 영역과 같다 |
| 파트 | `BE`, `FE`, `infra` | 하나 이상 |
| 우선순위 | `P0`, `P1`, `P2` | 하나. P0 = 1차 시연에 꼭 필요, P1 = 있어야 함, P2 = 시간이 되면 |
| 상태 | `needs-decision`, `blocked` | 해당할 때만 붙이고 해결되면 뗀다 |

- PR에도 이슈와 같은 유형·영역·파트 라벨을 붙인다.
- 문서 변경 때문에 생긴 작지만 급한 수정은 `docs`가 아니라 `fix`로 처리한다. 예: 바뀐 규칙과 어긋난 코드나 다른 문서, 잘못된 경로·코드·숫자, 깨진 링크. 버그 템플릿, `fix` 라벨, `fix/` 브랜치, `fix:` 커밋을 쓴다. `docs`는 계획된 문서 작업에 쓴다.
