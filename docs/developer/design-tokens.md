# REMAKE 디자인 토큰

> v2.0.0 · 2026-11-20 · 피그마 REMAKE 파일의 로컬 변수 24개, 페인트 스타일 2개, 모바일 텍스트 스타일 6개를 이름 그대로 옮겼습니다.
>
> 영문 버전(에이전트용): [`docs/agent/design-tokens.md`](../agent/design-tokens.md)

## 원칙: 피그마 디자인이 최우선

- 토큰은 피그마에 정의된 변수·스타일만 이름 그대로 옮긴 것입니다. 값을 합치거나 4pt 등에 억지로 맞추지 않습니다.
- 개발할 때는 피그마 프레임(Dev Mode)을 읽고, 변수가 걸린 값은 같은 이름의 토큰을, 변수가 없는 값(간격·반경·그림자·글래스 등)은 프레임 값을 그대로 씁니다.
- 토큰과 피그마가 다르면 피그마를 따르고, 차이는 토큰에 반영합니다.
- 예외는 기능상 꼭 필요한 것만 둡니다([기능상 필요한 예외](#기능상-필요한-예외) 참고).

## 파일과 다시 만들기

| 파일                            | 역할                                                    |
| ------------------------------- | ------------------------------------------------------- |
| `design/tokens/build_tokens.py` | 원본. 토큰 값이 이 스크립트 안에 있습니다.              |
| `design/tokens/tokens.json`     | 생성 결과(데이터).                                      |
| `design/tokens/tokens.css`      | 생성 결과(CSS 변수).                                    |
| `design/tokens/index.html`      | 생성 결과(미리보기 페이지). 브라우저로 열어 확인합니다. |
| `frontend/app/tokens.css`       | 앱이 쓰는 복사본. `globals.css`가 불러옵니다.           |

값을 바꿀 때는 `build_tokens.py`만 고칩니다. 생성된 파일은 손으로 고치지 않습니다.

```bash
python3 design/tokens/build_tokens.py
cp design/tokens/tokens.css frontend/app/tokens.css
```

## 피그마 변수 → CSS 변수

피그마 이름의 `/`는 `-`로 바뀝니다. 오타(`warring`, `skeletn`)도 피그마와 같게 두었습니다.

### 글자 색 (`text/`)

| CSS 변수                  | 값        | 피그마 변수        |
| ------------------------- | --------- | ------------------ |
| `var(--text-header)`      | `#111827` | `text/header`      |
| `var(--text-title)`       | `#374151` | `text/title`       |
| `var(--text-content)`     | `#4B5563` | `text/content`     |
| `var(--text-assist)`      | `#98A3B4` | `text/assist`      |
| `var(--text-placeholder)` | `#ABB7CA` | `text/placeholder` |
| `var(--text-disabled)`    | `#B0B8C1` | `text/disabled`    |
| `var(--text-primary)`     | `#3182F6` | `text/primary`     |
| `var(--text-error)`       | `#EF4444` | `text/error`       |
| `var(--text-warring)`     | `#FF7119` | `text/warring`     |
| `var(--text-safe)`        | `#179C00` | `text/safe`        |
| `var(--text-white)`       | `#F8F9F6` | `text/white`       |

### 배경 (`bg/`)

| CSS 변수                        | 값                       | 피그마 변수              |
| ------------------------------- | ------------------------ | ------------------------ |
| `var(--bg-white)`               | `#FFFFFF`                | `bg/white`               |
| `var(--bg-1depth)`              | `#F2F4F7`                | `bg/1depth`              |
| `var(--bg-1depth-bg)`           | `#EFF5FF`                | `bg/1depth-bg`           |
| `var(--bg-blue-btn-bg)`         | `#EBF3FF`                | `bg/blue-btn-bg`         |
| `var(--bg-input-bg)`            | `#F2F4F6`                | `bg/input-bg`            |
| `var(--bg-gray-1depth)`         | `#F5F5F5`                | `bg/gray-1depth`         |
| `var(--bg-gray-2depth)`         | `#EAEAEA`                | `bg/gray-2depth`         |
| `var(--bg-gray-3depth)`         | `#CFCFCF`                | `bg/gray-3depth`         |
| `var(--bg-gray-4depth)`         | `#B9B9B9`                | `bg/gray-4depth`         |
| `var(--bg-skeletn-bg)`          | `#E9ECEF`                | `bg/skeletn-bg`          |
| `var(--bg-skeleton-highlights)` | `#F1F5F9`                | `bg/skeleton-highlights` |
| `var(--bg-shadow)`              | `rgba(25, 31, 40, 0.32)` | `bg/shadow`              |

### 선

| CSS 변수          | 값        | 피그마 변수 |
| ----------------- | --------- | ----------- |
| `var(--stroke-1)` | `#E2E8F0` | `stroke-1`  |

## 피그마 페인트 스타일

| CSS 변수                     | 값        | 피그마 스타일   |
| ---------------------------- | --------- | --------------- |
| `var(--style-primay)`        | `#3182F6` | `primay`        |
| `var(--style-complementary)` | `#F56214` | `complementary` |

## 피그마 텍스트 스타일 (모바일)

행간은 피그마가 모두 auto라 CSS `normal` 그대로 둡니다. 태블릿 스타일 6개는 앱 폭(360~480px)에서 쓰지 않아 뺐습니다.

| 사용법                                 | 피그마 스타일          | 크기 | 굵기     |
| -------------------------------------- | ---------------------- | ---- | -------- |
| `font: var(--font-header-mobile)`      | `header - mobile`      | 18px | SemiBold |
| `font: var(--font-title-mobile)`       | `title - mobile`       | 16px | Medium   |
| `font: var(--font-content-mobile)`     | `content - mobile`     | 14px | Medium   |
| `font: var(--font-assist-mobile)`      | `assist - mobile`      | 12px | Medium   |
| `font: var(--font-placeholder-mobile)` | `placeholder - mobile` | 12px | Medium   |
| `font: var(--font-error-mobile)`       | `error - mobile`       | 12px | Regular  |

## 재료 카테고리 점 색

피그마 소비 패턴 화면에 쓰인 값입니다. 데이터(카테고리 코드)로 색을 골라야 해서 토큰으로 둡니다. 변수 이름은 카테고리 코드를 소문자 kebab-case로 바꾼 것입니다(예: `DAIRY_EGG` → `--category-dairy-egg`).

| CSS 변수                    | 값        | 카테고리         |
| --------------------------- | --------- | ---------------- |
| `var(--category-vegetable)` | `#2E9E5D` | 채소             |
| `var(--category-fruit)`     | `#CE3B78` | 과일             |
| `var(--category-meat)`      | `#C24747` | 육류             |
| `var(--category-seafood)`   | `#358AD4` | 해산물           |
| `var(--category-dairy-egg)` | `#A58812` | 달걀 및 유제품   |
| `var(--category-grain)`     | `#B98046` | 곡류 및 면, 빵   |
| `var(--category-bean-tofu)` | `#779933` | 두부 및 콩류     |
| `var(--category-processed)` | `#9947C2` | 가공 및 냉동식품 |
| `var(--category-side-dish)` | `#DA5D2F` | 김치 및 반찬     |
| `var(--category-seasoning)` | `#2E9E8B` | 양념             |
| `var(--category-beverage)`  | `#4641C8` | 음료             |
| `var(--category-etc)`       | `#7B838E` | 기타             |

## 기능상 필요한 예외

| 변수                 | 값            | 이유                                                                            |
| -------------------- | ------------- | ------------------------------------------------------------------------------- |
| `--font-family-base` | Pretendard 외 | 글꼴 지정(피그마 Pretendard + 대체 글꼴)                                        |
| `--font-size-input`  | `1rem`        | 입력창 글자 16px. 피그마는 14·12px지만 iOS에서 16px 미만이면 화면이 자동 확대됨 |
| `--z-sticky`         | `100`         | 상단 고정 헤더·탭                                                               |
| `--z-fixed`          | `200`         | 하단 고정 버튼 영역                                                             |
| `--z-dropdown`       | `300`         | 케밥 메뉴                                                                       |
| `--z-overlay`        | `400`         | 딤 배경                                                                         |
| `--z-sheet`          | `500`         | 바텀시트                                                                        |
| `--z-modal`          | `600`         | 모달                                                                            |
| `--z-toast`          | `700`         | 토스트                                                                          |
