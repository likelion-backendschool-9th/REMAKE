"""Remake 디자인 토큰 v2 생성기.
원칙: 피그마 디자인이 최우선. 토큰 = 피그마에 정의된 변수·스타일만(이름 그대로).
그 밖의 값(간격·반경·그림자·글래스 등)은 개발할 때 피그마 프레임 값을 그대로 읽어 쓴다.
사용법: python3 build_tokens.py  (같은 폴더에 tokens.json·tokens.css·index.html 생성)
"""
import json, os, html

OUT = os.path.dirname(os.path.abspath(__file__))
e = html.escape

def rem(px):
    s = ("%.4f" % (px / 16)).rstrip("0").rstrip(".")
    return s + "rem"

PRINCIPLE = ("피그마 디자인이 최우선. 토큰은 피그마에 정의된 변수·스타일만 이름 그대로 옮긴 것이다. "
             "간격·반경·그림자·글래스 등 나머지 값은 억지로 정리하거나 맞추지 말고 피그마 프레임(Dev Mode)의 값을 그대로 읽어 쓴다. "
             "프레임에 변수가 걸려 있으면 그 변수에 해당하는 토큰을 쓰고, 안 걸려 있으면 프레임 값을 그대로 쓴다.")

# 피그마 로컬 변수 (Collection 1 / Mode 1) — 이름 그대로
variables = [
  ("text/header", "#111827"), ("text/title", "#374151"), ("text/content", "#4B5563"),
  ("text/assist", "#98A3B4"), ("text/placeholder", "#ABB7CA"), ("text/disabled", "#B0B8C1"),
  ("text/primary", "#3182F6"), ("text/error", "#EF4444"), ("text/warring", "#FF7119"),
  ("text/safe", "#179C00"), ("text/white", "#F8F9F6"),
  ("bg/white", "#FFFFFF"), ("bg/1depth", "#F2F4F7"), ("bg/1depth-bg", "#EFF5FF"),
  ("bg/blue-btn-bg", "#EBF3FF"), ("bg/input-bg", "#F2F4F6"),
  ("bg/gray-1depth", "#F5F5F5"), ("bg/gray-2depth", "#EAEAEA"), ("bg/gray-3depth", "#CFCFCF"),
  ("bg/gray-4depth", "#B9B9B9"), ("bg/skeletn-bg", "#E9ECEF"), ("bg/skeleton-highlights", "#F1F5F9"),
  ("bg/shadow", "rgba(25, 31, 40, 0.32)"), ("stroke-1", "#E2E8F0"),
]
# 피그마 페인트 스타일 — 이름 그대로
paint_styles = [("primay", "#3182F6"), ("complementary", "#F56214")]
# 피그마 텍스트 스타일 (모바일만. 앱 폭 360~480px라 태블릿 스타일은 제외) — 행간 auto = CSS normal
text_styles = [
  ("header - mobile", 18, 600, "SemiBold"),
  ("title - mobile", 16, 500, "Medium"),
  ("content - mobile", 14, 500, "Medium"),
  ("assist - mobile", 12, 500, "Medium"),
  ("placeholder - mobile", 12, 500, "Medium"),
  ("error - mobile", 12, 400, "Regular"),
]
# 재료 카테고리 점 색: 피그마 화면(소비 패턴 막대)에 쓰인 값. 피그마 변수는 아니지만 데이터(코드)로 색을 고르므로 토큰으로 둔다
categories = [("VEGETABLE","채소","#2E9E5D"),("FRUIT","과일","#CE3B78"),("MEAT","육류","#C24747"),("SEAFOOD","해산물","#358AD4"),
  ("DAIRY_EGG","달걀 및 유제품","#A58812"),("GRAIN","곡류 및 면, 빵","#B98046"),("BEAN_TOFU","두부 및 콩류","#779933"),
  ("PROCESSED","가공 및 냉동식품","#9947C2"),("SIDE_DISH","김치 및 반찬","#DA5D2F"),("SEASONING","양념","#2E9E8B"),
  ("BEVERAGE","음료","#4641C8"),("ETC","기타","#7B838E")]
# 기능상 꼭 필요한 예외 (피그마에 없음)
FONT_FAMILY = "var(--font-pretendard, 'Pretendard Variable'), Pretendard, -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', sans-serif"
exceptions = [
  ("--font-family-base", FONT_FAMILY, "글꼴 지정(피그마 Pretendard + 대체 글꼴)"),
  ("--font-size-input", "1rem", "입력창 글자 16px. 피그마는 14·12px지만 iOS에서 16px 미만이면 화면이 자동 확대됨"),
  ("--z-sticky", "100", "상단 고정 헤더·탭"),
  ("--z-fixed", "200", "하단 고정 버튼 영역"),
  ("--z-dropdown", "300", "케밥 메뉴"),
  ("--z-overlay", "400", "딤 배경"),
  ("--z-sheet", "500", "바텀시트"),
  ("--z-modal", "600", "모달"),
  ("--z-toast", "700", "토스트"),
]

def cssname(n):
    return "--" + n.replace(" - ", "-").replace("/", "-").replace(" ", "-").lower()

# ---------- tokens.json ----------
tj = {
  "meta": {"name": "Remake 디자인 토큰", "version": "2.0.0", "date": "2026-11-20",
           "principle": PRINCIPLE,
           "source": "Figma etTm84sh4aA60z6BmOwM3i 로컬 변수(Collection 1) 24개, 페인트 스타일 2개, 텍스트 스타일 모바일 6개",
           "naming": "피그마 이름 그대로(오타 text/warring·bg/skeletn-bg·primay도 그대로 둠. 피그마에서 이름을 고치면 여기도 같이 고침). '/'와 ' - '는 '-'로 바꿔 CSS 변수가 됨",
           "units": "rem(1rem=16px), 선만 px, 라이트 모드만"},
  "variables": {n: {"$type": "color", "$value": v, "css": cssname(n)} for n, v in variables},
  "paintStyles": {n: {"$type": "color", "$value": v, "css": "--style-" + n} for n, v in paint_styles},
  "textStyles": {n: {"$type": "typography", "$value": {"fontFamily": "Pretendard", "fontSize": rem(s), "fontWeight": w, "lineHeight": "normal", "letterSpacing": 0},
                     "px": s, "figmaWeight": fw, "css": cssname("font/" + n)} for n, s, w, fw in text_styles},
  "categoryDot": {c: {"$type": "color", "$value": v, "label": k, "css": "--category-" + c.lower().replace("_", "-"),
                      "note": "피그마 화면에 쓰인 값(변수 아님)"} for c, k, v in categories},
  "exceptions": {n: {"$value": v, "$description": d} for n, v, d in exceptions},
}
json.dump(tj, open(f"{OUT}/tokens.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)

# ---------- tokens.css ----------
L = ["/* Remake 디자인 토큰 v2.0.0 (2026-11-20)",
     " * 원칙: " + PRINCIPLE,
     " * 원천: design/tokens/build_tokens.py (생성 파일은 손으로 고치지 말고 이 스크립트로 다시 만듭니다)",
     " */", ":root {", "  color-scheme: light;", "",
     "  /* ---------- 피그마 변수 (Collection 1) ---------- */"]
for n, v in variables: L.append(f"  {cssname(n)}: {v}; /* figma: {n} */")
L += ["", "  /* ---------- 피그마 페인트 스타일 ---------- */"]
for n, v in paint_styles: L.append(f"  --style-{n}: {v}; /* figma style: {n} */")
L += ["", "  /* ---------- 피그마 텍스트 스타일 (모바일). 사용: font: var(--font-content-mobile); ---------- */"]
for n, s, w, fw in text_styles:
    L.append(f"  {cssname('font/' + n)}: {w} {rem(s)}/normal var(--font-family-base); /* figma: {n} = Pretendard {fw} {s}px, 행간 auto */")
L += ["", "  /* ---------- 재료 카테고리 점 색 (피그마 화면 값) ---------- */"]
for c, k, v in categories: L.append(f"  --category-{c.lower().replace('_','-')}: {v}; /* {k} */")
L += ["", "  /* ---------- 기능상 필요한 예외 (피그마에 없음) ---------- */"]
for n, v, d in exceptions: L.append(f"  {n}: {v}; /* {d} */")
L += ["}", ""]
open(f"{OUT}/tokens.css", "w", encoding="utf-8").write("\n".join(L))

# ---------- index.html ----------
def sw(css, val, sub):
    return (f'<div class="sw"><div class="chip" style="background:var({css})"></div><div class="meta">'
            f'<code>var({css})</code><span class="val">{e(val)}</span><span class="desc">{e(sub)}</span></div></div>')
var_html = "".join(sw(cssname(n), v, "figma: " + n) for n, v in variables)
ps_html = "".join(sw("--style-" + n, v, "figma style: " + n) for n, v in paint_styles)
cat_html = "".join(sw("--category-" + c.lower().replace("_", "-"), v, k) for c, k, v in categories)
ty_html = "".join(f'<div class="ty"><div style="font:var({cssname("font/" + n)});color:var(--text-header)">냉장고에 재료가 4개 있어요</div>'
                  f'<div class="meta row"><code>font: var({cssname("font/" + n)})</code><span class="val">figma: {e(n)} · {s}px · {fw}</span></div></div>'
                  for n, s, w, fw in text_styles)
ex_html = "".join(f"<tr><td><code>{e(n)}</code></td><td>{e(v if len(v) < 30 else 'Pretendard 외')}</td><td>{e(d)}</td></tr>" for n, v, d in exceptions)

page = f"""<!doctype html>
<html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Remake 디자인 토큰</title>
<link rel="stylesheet" href="tokens.css">
<style>
@font-face{{font-family:'Pretendard Variable';src:url(PretendardVariable.woff2) format('woff2');font-weight:45 920;font-display:swap}}
*{{box-sizing:border-box}}
body{{margin:0;background:var(--bg-1depth);color:var(--text-content);font:var(--font-content-mobile)}}
main{{max-width:60rem;margin:0 auto;padding:2rem 1rem}}
h1{{font:600 1.375rem/normal var(--font-family-base);color:var(--text-header);margin:0 0 .5rem}}
h2{{font:var(--font-header-mobile);color:var(--text-header);margin:0 0 1rem}}
p{{margin:0;line-height:1.6}}
.lead{{color:var(--text-assist);font:var(--font-assist-mobile);line-height:1.6}}
.principle{{margin:1.25rem 0;background:var(--bg-blue-btn-bg);border-left:4px solid var(--text-primary);border-radius:.75rem;padding:1rem}}
.principle b{{font:var(--font-title-mobile);font-weight:600;color:var(--text-header)}}
.principle ol{{margin:.5rem 0 0;padding-left:1.25rem;line-height:1.7}}
section{{background:var(--bg-white);border-radius:.875rem;box-shadow:0 0 1px rgba(0,0,0,.25);padding:1.5rem 1rem;margin-bottom:1.25rem}}
code{{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:.75rem;color:var(--text-title)}}
.val,.desc{{color:var(--text-assist);font:var(--font-assist-mobile)}}
.grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(13rem,1fr));gap:.75rem}}
.sw{{display:flex;gap:.75rem;align-items:flex-start}}
.chip{{flex:none;width:3rem;height:3rem;border-radius:.75rem;border:1px solid var(--stroke-1)}}
.meta{{display:flex;flex-direction:column;gap:2px;min-width:0}}
.meta.row{{flex-direction:row;flex-wrap:wrap;gap:.5rem}}
.ty{{display:flex;flex-direction:column;gap:.25rem;padding:.75rem 0;border-bottom:1px solid var(--bg-1depth)}}
table{{width:100%;border-collapse:collapse}}
th,td{{text-align:left;padding:.5rem;border-bottom:1px solid var(--bg-1depth);font:var(--font-assist-mobile);line-height:1.6;vertical-align:top}}
th{{color:var(--text-assist)}}
.tablewrap{{overflow-x:auto}}
</style></head>
<body><main>
<h1>Remake 디자인 토큰</h1>
<p class="lead">v2.0.0 · 2026-11-20 · 피그마 REMAKE 파일의 로컬 변수 24개, 페인트 스타일 2개, 모바일 텍스트 스타일 6개를 이름 그대로 옮겼습니다.</p>
<div class="principle"><b>원칙: 피그마 디자인이 최우선</b>
<ol>
<li>토큰은 피그마에 정의된 변수·스타일만 이름 그대로 옮긴 것입니다. 값을 합치거나 4pt 등에 억지로 맞추지 않습니다.</li>
<li>개발할 때는 피그마 프레임(Dev Mode)을 읽고, 변수가 걸린 값은 같은 이름의 토큰을, 변수가 없는 값(간격·반경·그림자·글래스 등)은 프레임 값을 그대로 씁니다.</li>
<li>토큰과 피그마가 다르면 피그마를 따르고, 차이는 토큰에 반영합니다.</li>
<li>예외는 기능상 꼭 필요한 것만 둡니다(아래 표).</li>
</ol></div>
<section><h2>피그마 변수 → CSS 변수</h2><p class="desc" style="margin-bottom:1rem">'/'는 '-'로 바뀝니다. 오타(warring, skeletn)도 피그마와 같게 두었습니다.</p><div class="grid">{var_html}</div></section>
<section><h2>피그마 페인트 스타일</h2><div class="grid">{ps_html}</div></section>
<section><h2>피그마 텍스트 스타일 (모바일)</h2><p class="desc">행간은 피그마가 모두 auto라 CSS normal 그대로 둡니다. 태블릿 스타일 6개는 앱 폭(360~480px)에서 쓰지 않아 뺐습니다.</p>{ty_html}</section>
<section><h2>재료 카테고리 점 색</h2><p class="desc" style="margin-bottom:1rem">피그마 소비 패턴 화면에 쓰인 값입니다. 데이터(카테고리 코드)로 색을 골라야 해서 토큰으로 둡니다.</p><div class="grid">{cat_html}</div></section>
<section><h2>기능상 필요한 예외</h2><div class="tablewrap"><table><tr><th>변수</th><th>값</th><th>이유</th></tr>{ex_html}</table></div></section>
</main></body></html>"""
open(f"{OUT}/index.html", "w", encoding="utf-8").write(page)
print("ok", len(variables), len(paint_styles), len(text_styles))
