"""Remake 디자인 토큰 v3 생성기.
원칙: 피그마 디자인이 최우선. 토큰 = 피그마 '디자인 시스템' 페이지 > '0. 기초'에 정의된 변수·스타일(이름 그대로).
CSS 변수 이름은 피그마 Dev Mode가 보여 주는 코드 이름(예: var(--color-text-strong))과 같다.
사용법: python3 build_tokens.py  (같은 폴더에 tokens.json·tokens.css·index.html 생성)
"""
import json, os, html

OUT = os.path.dirname(os.path.abspath(__file__))
e = html.escape
VERSION, DATE = "3.0.0", "2026-11-23"
SOURCE = "Figma etTm84sh4aA60z6BmOwM3i '디자인 시스템' 페이지 > '0. 기초' (node 5761:2324, Remake 디자인 시스템 v1 2026-11-22)"

def rem(px):
    s = ("%.4f" % (px / 16)).rstrip("0").rstrip(".")
    return "0" if s == "0" else s + "rem"

PRINCIPLE = ("피그마 디자인이 최우선. 토큰은 피그마 디자인 시스템(0. 기초)에 정의된 변수·스타일만 이름 그대로 옮긴 것이다. "
             "프레임에 변수·스타일이 걸려 있으면 같은 이름의 토큰을 쓰고, 안 걸려 있으면 프레임 값을 그대로 쓴다.")

# ---------- 피그마 변수: Remake Color ----------
# (그룹, 이름, 값). CSS 변수 = --color-<그룹>-<이름>
colors = [
  ("text", [
    ("strong", "#111827"), ("title", "#374151"), ("body", "#4B5563"), ("sub", "#9CA3AF"),
    ("assist", "#98A3B4"), ("placeholder", "#ABB7CA"), ("disabled", "#B0B8C1"), ("on-color", "#F8F9F6"),
    ("primary", "#3182F6"), ("danger", "#DC2626"), ("warning", "#FF7119"), ("safe", "#179C00"),
  ]),
  ("bg", [
    ("surface", "#FFFFFF"), ("subtle", "#F2F4F7"), ("section", "#F3F4F6"), ("input", "#F2F4F6"),
    ("primary-subtle", "#EBF3FF"), ("primary-tint", "#EFF5FF"), ("skeleton", "#E9ECEF"),
    ("skeleton-highlight", "#F1F5F9"), ("primary", "#3182F6"), ("danger", "#DC2626"),
    ("badge-danger", "#DC262626"), ("badge-warning", "#FF71191F"), ("badge-safe", "#179C001F"),
    ("dim", "#191F2899"),
  ]),
  ("border", [("default", "#E2E8F0"), ("subtle", "#F5F5F5"), ("control", "#CFCFCF"), ("chip", "#EAEAEA")]),
  ("icon", [("default", "#374151"), ("sub", "#9CA3AF"), ("disabled", "#CFCFCF"), ("favorite", "#FDD43E")]),
  ("brand", [("primary", "#3182F6"), ("complementary", "#F56214")]),
  ("social", [("kakao", "#FEE500"), ("naver", "#03C75A"), ("google", "#F2F2F2")]),
]
ALPHA_NOTE = {"badge-danger": "#DC2626 15%", "badge-warning": "#FF7119 12%", "badge-safe": "#179C00 12%", "dim": "#191F28 60%"}

# ---------- 피그마 변수: Remake Size ----------
spaces = [2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 30]
radii = [("xs", 4), ("sm", 8), ("md", 10), ("lg", 14), ("xl", 20), ("full", 100)]

# ---------- 피그마 텍스트 스타일 Remake/* (행간 140%, 자간 0) ----------
# 피그마는 임시로 Noto Sans KR. 앱은 Pretendard(피그마 메모: "Pretendard로 바꿀 것")
text_styles = [
  ("display", 22, 700), ("header", 18, 700), ("subheader", 16, 700), ("title", 16, 500),
  ("content", 14, 500), ("content-regular", 14, 400), ("content-strong", 14, 700),
  ("assist", 12, 500), ("assist-strong", 12, 700), ("error", 12, 400),
]
WEIGHT_NAME = {400: "Regular", 500: "Medium", 700: "Bold"}
LINE_HEIGHT = 1.4

# ---------- 피그마 이펙트 스타일 Remake/shadow-* ----------
shadows = [
  ("control", "0 0 4px 0 rgba(0, 0, 0, 0.12)", "#0000001F, (0,0), blur 4"),
  ("card", "0 1px 4px 0 rgba(0, 0, 0, 0.08)", "#00000014, (0,1), blur 4"),
  ("sheet", "0 -4px 10px 0 rgba(0, 0, 0, 0.1)", "#0000001A, (0,-4), blur 10"),
  ("modal", "0 4px 16px 0 rgba(0, 0, 0, 0.16)", "#00000029, (0,4), blur 16"),
  ("header", "0 4px 20px 0 rgba(49, 130, 246, 0.1)", "#3182F61A, (0,4), blur 20"),
]

# 재료 카테고리 점 색: 피그마 화면(소비 패턴 막대)에 쓰인 값. 디자인 시스템 변수는 아니지만 데이터(코드)로 색을 고르므로 토큰으로 둔다
categories = [("VEGETABLE","채소","#2E9E5D"),("FRUIT","과일","#CE3B78"),("MEAT","육류","#C24747"),("SEAFOOD","해산물","#358AD4"),
  ("DAIRY_EGG","달걀 및 유제품","#A58812"),("GRAIN","곡류 및 면, 빵","#B98046"),("BEAN_TOFU","두부 및 콩류","#779933"),
  ("PROCESSED","가공 및 냉동식품","#9947C2"),("SIDE_DISH","김치 및 반찬","#DA5D2F"),("SEASONING","양념","#2E9E8B"),
  ("BEVERAGE","음료","#4641C8"),("ETC","기타","#7B838E")]

# 기능상 꼭 필요한 예외 (피그마에 없음)
FONT_FAMILY = "var(--font-pretendard, 'Pretendard Variable'), Pretendard, -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', sans-serif"
exceptions = [
  ("--font-family-base", FONT_FAMILY, "글꼴 지정(Pretendard + 대체 글꼴). 피그마의 임시 글꼴 Noto Sans KR 대신 Pretendard"),
  ("--font-size-input", "1rem", "입력창 글자 16px. 피그마는 14·12px지만 iOS에서 16px 미만이면 화면이 자동 확대됨"),
  ("--z-sticky", "100", "상단 고정 헤더·탭"),
  ("--z-fixed", "200", "하단 고정 버튼 영역"),
  ("--z-dropdown", "300", "케밥 메뉴"),
  ("--z-overlay", "400", "딤 배경"),
  ("--z-sheet", "500", "바텀시트"),
  ("--z-modal", "600", "모달"),
  ("--z-toast", "700", "토스트"),
]

def ccss(g, n): return f"--color-{g}-{n}"
def font_css(n): return f"--font-{n}"
def font_val(s, w): return f"{w} {rem(s)}/{LINE_HEIGHT} var(--font-family-base)"
def cat_css(c): return "--category-" + c.lower().replace("_", "-")

# ---------- tokens.json ----------
tj = {
  "meta": {"name": "Remake 디자인 토큰", "version": VERSION, "date": DATE, "principle": PRINCIPLE, "source": SOURCE,
           "naming": "피그마 Dev Mode 코드 이름 그대로: 색 --color-<그룹>-<이름>, 간격 --space-<px>, 모서리 --radius-<단계>, 텍스트 --font-<스타일>, 그림자 --shadow-<이름>",
           "units": "rem(1rem=16px), 선만 px, 라이트 모드만"},
  "color": {f"{g}/{n}": {"$type": "color", "$value": v, "css": ccss(g, n)} for g, items in colors for n, v in items},
  "space": {f"space/{p}": {"$type": "dimension", "$value": rem(p), "px": p, "css": f"--space-{p}"} for p in spaces},
  "radius": {f"radius/{n}": {"$type": "dimension", "$value": rem(p), "px": p, "css": f"--radius-{n}"} for n, p in radii},
  "textStyles": {f"Remake/{n}": {"$type": "typography", "$value": {"fontFamily": "Pretendard", "fontSize": rem(s), "fontWeight": w,
                 "lineHeight": LINE_HEIGHT, "letterSpacing": 0}, "px": s, "css": font_css(n)} for n, s, w in text_styles},
  "shadows": {f"Remake/shadow-{n}": {"$type": "shadow", "$value": v, "figma": f, "css": f"--shadow-{n}"} for n, v, f in shadows},
  "categoryDot": {c: {"$type": "color", "$value": v, "label": k, "css": cat_css(c), "note": "피그마 화면에 쓰인 값(변수 아님)"}
                  for c, k, v in categories},
  "exceptions": {n: {"$value": v, "$description": d} for n, v, d in exceptions},
}
json.dump(tj, open(f"{OUT}/tokens.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)

# ---------- tokens.css ----------
L = [f"/* Remake 디자인 토큰 v{VERSION} ({DATE})",
     " * 원칙: " + PRINCIPLE,
     " * 원천: " + SOURCE,
     " * 생성: design/tokens/build_tokens.py (생성 파일은 손으로 고치지 말고 이 스크립트로 다시 만듭니다)",
     " */", ":root {", "  color-scheme: light;"]
for g, items in colors:
    L += ["", f"  /* ---------- 색 {g}/ (Remake Color) ---------- */"]
    for n, v in items:
        note = f" = {ALPHA_NOTE[n]}" if g == "bg" and n in ALPHA_NOTE else ""
        L.append(f"  {ccss(g, n)}: {v}; /* figma: {g}/{n}{note} */")
L += ["", "  /* ---------- 간격 space/ (Remake Size) ---------- */"]
for p in spaces: L.append(f"  --space-{p}: {rem(p)}; /* {p}px */")
L += ["", "  /* ---------- 모서리 radius/ (Remake Size) ---------- */"]
for n, p in radii: L.append(f"  --radius-{n}: {rem(p)}; /* {p}px */")
L += ["", "  /* ---------- 텍스트 스타일 Remake/*. 사용: font: var(--font-content); ---------- */"]
for n, s, w in text_styles:
    L.append(f"  {font_css(n)}: {font_val(s, w)}; /* figma: Remake/{n} = {WEIGHT_NAME[w]} {s}px, 행간 140% */")
L += ["", "  /* ---------- 그림자 Remake/shadow-*. 사용: box-shadow: var(--shadow-card); ---------- */"]
for n, v, f in shadows: L.append(f"  --shadow-{n}: {v}; /* figma: {f} */")
L += ["", "  /* ---------- 재료 카테고리 점 색 (피그마 화면 값) ---------- */"]
for c, k, v in categories: L.append(f"  {cat_css(c)}: {v}; /* {k} */")
L += ["", "  /* ---------- 기능상 필요한 예외 (피그마에 없음) ---------- */"]
for n, v, d in exceptions: L.append(f"  {n}: {v}; /* {d} */")
L += ["}", ""]
open(f"{OUT}/tokens.css", "w", encoding="utf-8").write("\n".join(L))

# ---------- index.html (미리보기) ----------
def sw(css, val, sub):
    return (f'<div class="sw"><div class="chip" style="background:var({css})"></div><div class="meta">'
            f'<code>var({css})</code><span class="val">{e(val)}</span><span class="desc">{e(sub)}</span></div></div>')
color_html = "".join(
    f'<h3>{g}/</h3><div class="grid">' + "".join(
        sw(ccss(g, n), v + (f" ({ALPHA_NOTE[n]})" if g == "bg" and n in ALPHA_NOTE else ""), f"figma: {g}/{n}") for n, v in items) + "</div>"
    for g, items in colors)
space_html = "".join(f'<div class="bar"><span style="width:var(--space-{p})"></span><code>var(--space-{p})</code><span class="val">{p}px</span></div>' for p in spaces)
radius_html = "".join(f'<div class="rad"><div style="border-radius:var(--radius-{n})"></div><code>var(--radius-{n})</code><span class="val">{p}px</span></div>' for n, p in radii)
ty_html = "".join(f'<div class="ty"><div style="font:var({font_css(n)});color:var(--color-text-strong)">냉장고 속 재료를 똑똑하게 관리해요 123</div>'
                  f'<div class="meta row"><code>font: var({font_css(n)})</code><span class="val">figma: Remake/{e(n)} · {s}px · {WEIGHT_NAME[w]}</span></div></div>'
                  for n, s, w in text_styles)
shadow_html = "".join(f'<div class="rad"><div style="box-shadow:var(--shadow-{n});border:0"></div><code>var(--shadow-{n})</code><span class="val">{e(f)}</span></div>' for n, v, f in shadows)
cat_html = "".join(sw(cat_css(c), v, k) for c, k, v in categories)
ex_html = "".join(f"<tr><td><code>{e(n)}</code></td><td>{e(v if len(v) < 30 else 'Pretendard 외')}</td><td>{e(d)}</td></tr>" for n, v, d in exceptions)

page = f"""<!doctype html>
<html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Remake 디자인 토큰</title>
<link rel="stylesheet" href="tokens.css">
<style>
@font-face{{font-family:'Pretendard Variable';src:url(PretendardVariable.woff2) format('woff2');font-weight:45 920;font-display:swap}}
*{{box-sizing:border-box}}
body{{margin:0;background:var(--color-bg-subtle);color:var(--color-text-body);font:var(--font-content)}}
main{{max-width:60rem;margin:0 auto;padding:2rem 1rem}}
h1{{font:var(--font-display);color:var(--color-text-strong);margin:0 0 .5rem}}
h2{{font:var(--font-header);color:var(--color-text-strong);margin:0 0 1rem}}
h3{{font:var(--font-assist-strong);color:var(--color-text-sub);margin:1rem 0 .5rem}}
p{{margin:0;line-height:1.6}}
.lead{{color:var(--color-text-assist);font:var(--font-assist);line-height:1.6}}
.principle{{margin:1.25rem 0;background:var(--color-bg-primary-subtle);border-left:4px solid var(--color-text-primary);border-radius:var(--radius-md);padding:1rem}}
.principle b{{font:var(--font-subheader);color:var(--color-text-strong)}}
.principle ol{{margin:.5rem 0 0;padding-left:1.25rem;line-height:1.7}}
section{{background:var(--color-bg-surface);border-radius:var(--radius-lg);box-shadow:var(--shadow-card);padding:1.5rem 1rem;margin-bottom:1.25rem}}
code{{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:.75rem;color:var(--color-text-title)}}
.val,.desc{{color:var(--color-text-assist);font:var(--font-assist)}}
.grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(13rem,1fr));gap:.75rem}}
.sw{{display:flex;gap:.75rem;align-items:flex-start}}
.chip{{flex:none;width:3rem;height:3rem;border-radius:var(--radius-md);border:1px solid var(--color-border-default)}}
.meta{{display:flex;flex-direction:column;gap:2px;min-width:0}}
.meta.row{{flex-direction:row;flex-wrap:wrap;gap:.5rem}}
.ty{{display:flex;flex-direction:column;gap:.25rem;padding:.75rem 0;border-bottom:1px solid var(--color-border-subtle)}}
.bar{{display:flex;align-items:center;gap:.75rem;padding:.25rem 0}}
.bar span:first-child{{display:block;height:.75rem;background:var(--color-bg-primary);border-radius:2px}}
.row2{{display:flex;flex-wrap:wrap;gap:1.25rem}}
.rad{{display:flex;flex-direction:column;align-items:center;gap:.375rem;padding:.5rem}}
.rad div{{width:4rem;height:4rem;background:var(--color-bg-primary-subtle);border:1px solid var(--color-bg-primary)}}
table{{width:100%;border-collapse:collapse}}
th,td{{text-align:left;padding:.5rem;border-bottom:1px solid var(--color-border-subtle);font:var(--font-assist);line-height:1.6;vertical-align:top}}
th{{color:var(--color-text-assist)}}
.tablewrap{{overflow-x:auto}}
</style></head>
<body><main>
<h1>Remake 디자인 토큰</h1>
<p class="lead">v{VERSION} · {DATE} · {e(SOURCE)}</p>
<div class="principle"><b>원칙: 피그마 디자인이 최우선</b>
<ol>
<li>토큰은 피그마 디자인 시스템(0. 기초)에 정의된 변수·스타일만 이름 그대로 옮긴 것입니다. CSS 변수 이름은 피그마 Dev Mode의 코드 이름과 같습니다.</li>
<li>개발할 때는 피그마 프레임(Dev Mode)을 읽고, 변수·스타일이 걸린 값은 같은 이름의 토큰을, 걸리지 않은 값은 프레임 값을 그대로 씁니다.</li>
<li>토큰과 피그마가 다르면 피그마를 따르고, 차이는 토큰에 반영합니다.</li>
<li>예외는 기능상 꼭 필요한 것만 둡니다(아래 표).</li>
</ol></div>
<section><h2>색 (Remake Color)</h2>{color_html}</section>
<section><h2>텍스트 스타일 (Remake/*)</h2><p class="desc">행간 140%, 자간 0. 피그마의 임시 글꼴 Noto Sans KR 대신 Pretendard로 그립니다.</p>{ty_html}</section>
<section><h2>간격 (space)</h2>{space_html}</section>
<section><h2>모서리 (radius)</h2><div class="row2">{radius_html}</div></section>
<section><h2>그림자 (Remake/shadow-*)</h2><div class="row2">{shadow_html}</div></section>
<section><h2>재료 카테고리 점 색</h2><p class="desc" style="margin-bottom:1rem">피그마 소비 패턴 화면에 쓰인 값입니다. 데이터(카테고리 코드)로 색을 골라야 해서 토큰으로 둡니다.</p><div class="grid">{cat_html}</div></section>
<section><h2>기능상 필요한 예외</h2><div class="tablewrap"><table><tr><th>변수</th><th>값</th><th>이유</th></tr>{ex_html}</table></div></section>
</main></body></html>"""
open(f"{OUT}/index.html", "w", encoding="utf-8").write(page)
print("ok", sum(len(i) for _, i in colors), "colors", len(spaces), "spaces", len(radii), "radii", len(text_styles), "text", len(shadows), "shadows")
