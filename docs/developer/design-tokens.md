# REMAKE 디자인 토큰

> v3.1.0 · 2026-11-23 · 피그마 REMAKE 파일의 `디자인 시스템` 페이지 > `0. 기초` 프레임([node 5761-2324](https://www.figma.com/design/etTm84sh4aA60z6BmOwM3i/REMAKE?node-id=5761-2324), "Remake 디자인 시스템 v1", 2026-11-22)에서 옮겼습니다. 색 변수 40개, 간격 11개·모서리 6개 변수, 텍스트 스타일 10개, 그림자 스타일 5개입니다.
>
> 영문 버전(에이전트용): [`docs/agent/design-tokens.md`](../agent/design-tokens.md)

## 원칙: 피그마 디자인이 최우선

- 토큰은 피그마 디자인 시스템(`0. 기초`)에 정의된 변수·스타일만 이름 그대로 옮긴 것입니다. CSS 변수 이름은 피그마 Dev Mode가 보여 주는 코드 이름과 같습니다(예: `var(--color-text-strong)`).
- 개발할 때는 피그마 프레임(Dev Mode)을 읽고, 변수·스타일이 걸린 값은 같은 이름의 토큰을, 걸리지 않은 값은 프레임 값을 그대로 씁니다.
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

## 이름 규칙

| 피그마                                   | CSS 변수                   | 예                                              |
| ---------------------------------------- | -------------------------- | ----------------------------------------------- |
| 색 변수 `<그룹>/<이름>`                  | `--color-<그룹>-<이름>`    | `text/strong` → `var(--color-text-strong)`      |
| 크기 변수 `space/<px>`                   | `--space-<px>`             | `space/16` → `var(--space-16)` (1rem)           |
| 크기 변수 `radius/<단계>`                | `--radius-<단계>`          | `radius/md` → `var(--radius-md)` (0.625rem)     |
| 텍스트 스타일 `Remake/<이름>`            | `--font-<이름>`            | `font: var(--font-content)`                     |
| 이펙트 스타일 `Remake/shadow-<이름>`     | `--shadow-<이름>`          | `box-shadow: var(--shadow-card)`                |

## 색 (`Remake Color`)

### 글자 (`text/`)

| CSS 변수                         | 값        | 피그마             |
| -------------------------------- | --------- | ------------------ |
| `var(--color-text-strong)`       | `#111827` | `text/strong`      |
| `var(--color-text-title)`        | `#374151` | `text/title`       |
| `var(--color-text-body)`         | `#4B5563` | `text/body`        |
| `var(--color-text-sub)`          | `#9CA3AF` | `text/sub`         |
| `var(--color-text-assist)`       | `#98A3B4` | `text/assist`      |
| `var(--color-text-placeholder)`  | `#ABB7CA` | `text/placeholder` |
| `var(--color-text-disabled)`     | `#B0B8C1` | `text/disabled`    |
| `var(--color-text-on-color)`     | `#F8F9F6` | `text/on-color`    |
| `var(--color-text-primary)`      | `#3182F6` | `text/primary`     |
| `var(--color-text-danger)`       | `#DC2626` | `text/danger`      |
| `var(--color-text-warning)`      | `#FF7119` | `text/warning`     |
| `var(--color-text-safe)`         | `#179C00` | `text/safe`        |

### 배경 (`bg/`)

| CSS 변수                              | 값                       | 피그마                  |
| ------------------------------------- | ------------------------ | ----------------------- |
| `var(--color-bg-surface)`             | `#FFFFFF`                | `bg/surface`            |
| `var(--color-bg-subtle)`              | `#F2F4F7`                | `bg/subtle`             |
| `var(--color-bg-section)`             | `#F3F4F6`                | `bg/section`            |
| `var(--color-bg-input)`               | `#F2F4F6`                | `bg/input`              |
| `var(--color-bg-primary-subtle)`      | `#EBF3FF`                | `bg/primary-subtle`     |
| `var(--color-bg-primary-tint)`        | `#EFF5FF`                | `bg/primary-tint`       |
| `var(--color-bg-skeleton)`            | `#E9ECEF`                | `bg/skeleton`           |
| `var(--color-bg-skeleton-highlight)`  | `#F1F5F9`                | `bg/skeleton-highlight` |
| `var(--color-bg-primary)`             | `#3182F6`                | `bg/primary`            |
| `var(--color-bg-danger)`              | `#DC2626`                | `bg/danger`             |
| `var(--color-bg-badge-danger)`        | `#DC262626` (#DC2626 15%) | `bg/badge-danger`      |
| `var(--color-bg-badge-warning)`       | `#FF71191F` (#FF7119 12%) | `bg/badge-warning`     |
| `var(--color-bg-badge-safe)`          | `#179C001F` (#179C00 12%) | `bg/badge-safe`        |
| `var(--color-bg-dim)`                 | `#191F2899` (#191F28 60%) | `bg/dim`               |
| `var(--color-bg-toast)`               | `#111827EB` (#111827 92%) | `bg/toast` (1)         |

(1) `bg/toast`는 토스트(기본·성공·오류)에 쓰는 피그마 변수입니다. Dev Mode 코드 이름이 없고 `0. 기초`에는 보이지 않지만, CSS 이름은 같은 규칙으로 정했습니다.

### 선 (`border/`)

| CSS 변수                       | 값        | 피그마           |
| ------------------------------ | --------- | ---------------- |
| `var(--color-border-default)`  | `#E2E8F0` | `border/default` |
| `var(--color-border-subtle)`   | `#F5F5F5` | `border/subtle`  |
| `var(--color-border-control)`  | `#CFCFCF` | `border/control` |
| `var(--color-border-chip)`     | `#EAEAEA` | `border/chip`    |

### 아이콘 (`icon/`)

| CSS 변수                       | 값        | 피그마          |
| ------------------------------ | --------- | --------------- |
| `var(--color-icon-default)`    | `#374151` | `icon/default`  |
| `var(--color-icon-sub)`        | `#9CA3AF` | `icon/sub`      |
| `var(--color-icon-disabled)`   | `#CFCFCF` | `icon/disabled` |
| `var(--color-icon-favorite)`   | `#FDD43E` | `icon/favorite` |

### 브랜드·소셜 (`brand/`, `social/`)

| CSS 변수                            | 값        | 피그마                |
| ----------------------------------- | --------- | --------------------- |
| `var(--color-brand-primary)`        | `#3182F6` | `brand/primary`       |
| `var(--color-brand-complementary)`  | `#F56214` | `brand/complementary` |
| `var(--color-social-kakao)`         | `#FEE500` | `social/kakao`        |
| `var(--color-social-naver)`         | `#03C75A` | `social/naver`        |
| `var(--color-social-google)`        | `#F2F2F2` | `social/google`       |

## 간격·모서리 (`Remake Size`)

값은 rem(px ÷ 16)입니다.

| CSS 변수           | px  |     | CSS 변수              | px  |
| ------------------ | --- | --- | --------------------- | --- |
| `var(--space-2)`   | 2   |     | `var(--radius-xs)`    | 4   |
| `var(--space-4)`   | 4   |     | `var(--radius-sm)`    | 8   |
| `var(--space-6)`   | 6   |     | `var(--radius-md)`    | 10  |
| `var(--space-8)`   | 8   |     | `var(--radius-lg)`    | 14  |
| `var(--space-10)`  | 10  |     | `var(--radius-xl)`    | 20  |
| `var(--space-12)`  | 12  |     | `var(--radius-full)`  | 100 |
| `var(--space-14)`  | 14  |     |                       |     |
| `var(--space-16)`  | 16  |     |                       |     |
| `var(--space-20)`  | 20  |     |                       |     |
| `var(--space-24)`  | 24  |     |                       |     |
| `var(--space-30)`  | 30  |     |                       |     |

## 텍스트 스타일 (`Remake/*`)

모든 스타일의 행간은 140%, 자간은 0입니다. 글꼴은 Pretendard입니다. 모바일만 둡니다(앱 폭 360~480px).

| 사용법                             | 피그마 스타일            | 크기 | 굵기          |
| ---------------------------------- | ------------------------ | ---- | ------------- |
| `font: var(--font-display)`        | `Remake/display`         | 22px | Bold 700      |
| `font: var(--font-header)`         | `Remake/header`          | 18px | Bold 700      |
| `font: var(--font-subheader)`      | `Remake/subheader`       | 16px | Bold 700      |
| `font: var(--font-title)`          | `Remake/title`           | 16px | Medium 500    |
| `font: var(--font-content)`        | `Remake/content`         | 14px | Medium 500    |
| `font: var(--font-content-regular)`| `Remake/content-regular` | 14px | Regular 400   |
| `font: var(--font-content-strong)` | `Remake/content-strong`  | 14px | Bold 700      |
| `font: var(--font-assist)`         | `Remake/assist`          | 12px | Medium 500    |
| `font: var(--font-assist-strong)`  | `Remake/assist-strong`   | 12px | Bold 700      |
| `font: var(--font-error)`          | `Remake/error`           | 12px | Regular 400   |

## 그림자 (`Remake/shadow-*`)

| 사용법                               | 피그마                 | 값 (색, 위치, 흐림)          |
| ------------------------------------ | ---------------------- | ---------------------------- |
| `box-shadow: var(--shadow-control)`  | `Remake/shadow-control`| `#0000001F`, (0, 0), 4       |
| `box-shadow: var(--shadow-card)`     | `Remake/shadow-card`   | `#00000014`, (0, 1), 4       |
| `box-shadow: var(--shadow-sheet)`    | `Remake/shadow-sheet`  | `#0000001A`, (0, -4), 10     |
| `box-shadow: var(--shadow-modal)`    | `Remake/shadow-modal`  | `#00000029`, (0, 4), 16      |
| `box-shadow: var(--shadow-header)`   | `Remake/shadow-header` | `#3182F61A`, (0, 4), 20      |

## 아이콘

디자인 시스템에는 아이콘 24개(`아이콘/*`, 24px, 색은 `icon/default`에 연결, 인스턴스에서 색을 바꿔 씀)가 있습니다. 코드에서는 프론트 규칙대로 Solar 아이콘(`@solar-icons/react`)을 씁니다. 가장 비슷한 Solar 아이콘을 고르고 색은 `--color-icon-*` 토큰으로 지정합니다.

## 재료 카테고리 점 색

피그마 소비 패턴 화면에 쓰인 값입니다. 디자인 시스템 변수는 아니지만 데이터(카테고리 코드)로 색을 골라야 해서 토큰으로 둡니다. 변수 이름은 카테고리 코드를 소문자 케밥으로 바꾼 것입니다(예: `DAIRY_EGG` → `--category-dairy-egg`).

| CSS 변수                    | 값        | 카테고리(코드)                    |
| --------------------------- | --------- | --------------------------------- |
| `var(--category-vegetable)` | `#2E9E5D` | 채소(`VEGETABLE`)                 |
| `var(--category-fruit)`     | `#CE3B78` | 과일(`FRUIT`)                     |
| `var(--category-meat)`      | `#C24747` | 육류(`MEAT`)                      |
| `var(--category-seafood)`   | `#358AD4` | 해산물(`SEAFOOD`)                 |
| `var(--category-dairy-egg)` | `#A58812` | 달걀 및 유제품(`DAIRY_EGG`)       |
| `var(--category-grain)`     | `#B98046` | 곡류 및 면, 빵(`GRAIN`)           |
| `var(--category-bean-tofu)` | `#779933` | 두부 및 콩류(`BEAN_TOFU`)         |
| `var(--category-processed)` | `#9947C2` | 가공 및 냉동식품(`PROCESSED`)     |
| `var(--category-side-dish)` | `#DA5D2F` | 김치 및 반찬(`SIDE_DISH`)         |
| `var(--category-seasoning)` | `#2E9E8B` | 양념(`SEASONING`)                 |
| `var(--category-beverage)`  | `#4641C8` | 음료(`BEVERAGE`)                  |
| `var(--category-etc)`       | `#7B838E` | 기타(`ETC`)                       |

## 기능상 필요한 예외

| 변수                 | 값              | 이유                                                                                     |
| -------------------- | --------------- | ---------------------------------------------------------------------------------------- |
| `--font-family-base` | Pretendard 외   | 글꼴 지정(피그마 Pretendard + 대체 글꼴)             |
| `--font-size-input`  | `1rem`          | 입력창 글자 16px. 피그마는 14·12px지만 iOS에서 16px 미만이면 화면이 자동 확대됩니다      |
| `--z-sticky`         | `100`           | 상단 고정 헤더·탭                                                                        |
| `--z-fixed`          | `200`           | 하단 고정 버튼 영역                                                                      |
| `--z-dropdown`       | `300`           | 케밥 메뉴                                                                                |
| `--z-overlay`        | `400`           | 딤 배경                                                                                  |
| `--z-sheet`          | `500`           | 바텀시트                                                                                 |
| `--z-modal`          | `600`           | 모달                                                                                     |
| `--z-toast`          | `700`           | 토스트                                                                                   |

## v2에서 바뀐 점

v2는 예전 로컬 변수(`text/header`, `bg/white`, `stroke-1`, 페인트 스타일 `primay`/`complementary`, `* - mobile` 텍스트 스타일)를 옮긴 것이었습니다. v3은 이를 모두 디자인 시스템 이름으로 바꿨습니다. 앱에서 쓰던 것의 옛 이름 → 새 이름: `--text-header` → `--color-text-strong`, `--text-content` → `--color-text-body`, `--text-assist` → `--color-text-assist`, `--text-white` → `--color-text-on-color`, `--text-error` → `--color-text-danger`, `--bg-white` → `--color-bg-surface`, `--stroke-1` → `--color-border-default`, `--style-primay` → `--color-brand-primary`, `--font-header-mobile` → `--font-header`(이제 Bold), `--font-content-mobile` → `--font-content`.
