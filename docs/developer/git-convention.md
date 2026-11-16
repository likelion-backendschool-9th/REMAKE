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
- 2주 프로젝트이므로 별도의 `release`, `hotfix` 브랜치는 사용하지 않는다.

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

## 5. PR 및 Merge 규칙

- 모든 작업 브랜치는 `dev`를 대상으로 PR을 생성한다.
- PR 제목은 커밋 컨벤션과 동일한 유형을 사용한다.
- PR 내용에는 작업 목적, 주요 변경 사항 및 테스트 결과를 작성한다. 본문은 [PR 템플릿](../../.github/pull_request_template.md)의 항목(변경 내용, 관련 이슈, 테스트, 리뷰 요청)을 모두 채운다.
- 팀원 리뷰 및 확인이 완료되면 Merge한다.
- Merge가 완료되면 원격 및 로컬 작업 브랜치를 삭제한다.

| 작업 브랜치 | PR 대상 |
| --- | --- |
| `feat/*` | `dev` |
| `fix/*` | `dev` |
| `chore/*` | `dev` |
| `docs/*` | `dev` |

## 6. 커밋 컨벤션

### 작성 형식

```
<유형>: <작업내용>
```

유형과 작업 내용은 콜론(`:`)으로 구분하고, 콜론 뒤에는 공백을 한 칸 둔다.

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
- 커밋 메시지와 PR 본문에 AI 도구의 공동 작성자 표기(`Co-Authored-By: Claude ...`)나 `Generated with ...` 문구를 넣지 않는다. `PR Attribution Check` CI에서 실패한다.

### 예시

```
feat: 프로젝트 등록 기능 구현
fix: 로그인 오류 수정
chore: ESLint 설정 변경
docs: Git Flow 문서 작성
```
