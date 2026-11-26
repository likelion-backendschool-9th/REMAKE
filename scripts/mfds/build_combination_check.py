#!/usr/bin/env python3
"""식약처 조리식품 레시피 DB(COOKRCP01) CSV로 식단표 조합 검사 기준 JSON을 만든다(#288 F).

사용법(저장소 루트에서):
    python3 scripts/mfds/build_combination_check.py <COOKRCP01.csv 또는 .csv.gz> \
        [--out backend/src/main/resources/mfds/combination-check.json]

원본 CSV는 저장소에 올리지 않는다(README 참고). 같은 폴더의 표만 고쳐 다시 돌린다.
- check_rules.json : 무리 묶음, 검사에서 뺄 이름, 주의 재료, 아침 재료, AI에 보내지 않을 냉장고 재료
- name_fixes.tsv   : 재료 이름 → 재료 사전 이름(자동 맞추기가 틀리거나 없을 때)과 카테고리
- synonyms.tsv     : 사전 안의 같은 재료 다른 표기(계란 → 달걀)

식약처 레시피 1,156개 전부(후식 포함)의 재료를 사전 이름으로 맞추고(양념·물 뺌), 주의 재료마다 함께 쓰인 재료 목록을 적는다.
서버는 AI가 만든 칸의 재료 짝 가운데 이 목록에 없는 짝에 주의 재료가 끼면(아침 재료끼리는 빼고) 형식 오류로 본다.
"""
import argparse
import csv
import gzip
import io
import itertools
import json
import re
import sys
from collections import OrderedDict, defaultdict
from fractions import Fraction
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
CATALOG_SQL = ROOT / 'backend/src/main/resources/db/migration/V5__create_ingredient_catalog.sql'
DEFAULT_OUT = ROOT / 'backend/src/main/resources/mfds/combination-check.json'
SEASONING = 'SEASONING'
# 서버 MealPlanCombinationRules와 같은 값: "○○밥"(4글자 이하, 볶음밥 아님)은 쌀로 본다
RICE_MAX_LENGTH = 4
RICE_EXCEPT = ('볶음밥',)
RICE = '쌀'
# 이름 안에서 찾을 사전 이름의 최소 글자 수(IngredientUnitPolicy와 같다)
MIN_CONTAINED_NAME_LENGTH = 2

UNIT_WORDS = OrderedDict([
    # 무게·부피
    ('kg', ('KG', 1)), ('g', ('G', 1)), ('mg', ('G', 0.001)), ('g씩', ('G', 1)),
    ('ml', ('ML', 1)), ('mL', ('ML', 1)), ('㎖', ('ML', 1)), ('cc', ('ML', 1)), ('ℓ', ('L', 1)), ('L', ('L', 1)),
    # 숟가락·컵
    ('T', ('TBSP', 1)), ('Ts', ('TBSP', 1)), ('큰술', ('TBSP', 1)), ('t', ('TSP', 1)), ('ts', ('TSP', 1)),
    ('작은술', ('TSP', 1)), ('컵', ('CUP', 1)),
    # 개수(J1 단순 변환: 마리·알·모·포기 → EA)
    ('개', ('EA', 1)), ('마리', ('EA', 1)), ('알', ('EA', 1)), ('모', ('EA', 1)), ('포기', ('EA', 1)),
    ('톨', ('EA', 1)), ('쪽', ('PIECE', 1)), ('대', ('PIECE', 1)), ('줄기', ('PIECE', 1)), ('뿌리', ('PIECE', 1)),
    ('장', ('SHEET', 1)), ('단', ('BUNCH', 1)),
    # 손대중: 한 줌 50g, 밥 한 공기 210g으로 읽는다
    ('줌', ('G', 50)), ('공기', ('G', 210)),
    # 묶음(J1: 봉지·캔·병·통 → PACK)
    ('봉지', ('PACK', 1)), ('봉', ('PACK', 1)), ('캔', ('PACK', 1)), ('병', ('PACK', 1)), ('통', ('PACK', 1)),
])
TO_TASTE_WORDS = ('적당량', '약간', '조금', '소량', '적량', '기호에따라', '기호에 따라', '약간씩')
WEIGHT = {'G': 1, 'KG': 1000}


# ---------------------------------------------------------------- 읽기

def read_csv(path):
    raw = Path(path).read_bytes()
    if path.endswith('.gz'):
        raw = gzip.decompress(raw)
    return list(csv.DictReader(io.StringIO(raw.decode('utf-8-sig'))))


def read_tsv(name):
    rows = []
    for line in (HERE / name).read_text(encoding='utf-8').splitlines():
        if not line.strip() or line.startswith('#'):
            continue
        rows.append([c.strip() for c in line.split('\t')])
    return rows


def read_catalog():
    """V5 마이그레이션의 재료 사전(이름 → (카테고리, 단위))."""
    sql = CATALOG_SQL.read_text(encoding='utf-8')
    rows = re.findall(r"\('([A-Z_]+)',\s*'([^']+)',\s*'([A-Z]+)',\s*(?:NULL|\d+),\s*(?:true|false)\)", sql)
    return OrderedDict((name, (category, unit)) for category, name, unit in rows)



# ---------------------------------------------------------------- 재료 줄 쪼개기

SERVINGS = re.compile(r'\[\s*(\d)\s*인분\s*\]|(\d)\s*인분\s*기준')
HEADER = re.compile(r'^(주재료|부재료|재료|양념|양념장|소스|장식|고명|드레싱|토핑|육수|반죽|곁들임)$|재료$|양념$|소스$')
# "재료 감자", "양념 다진 마늘", "토마토소스 토마토": 앞의 묶음 이름을 뗀다
PREFIX = re.compile(r'^(?:\S*(?:재료|양념|양념장|소스|드레싱|육수|밑간|반죽)\s+)+(?=[^\d\s])')
LABEL = re.compile(r'^([^():\d]{1,20})\s*:\s*(.+)$')
NUMBER = r'(\d+[½¼¾⅓⅔]|\d+(?:\.\d+)?(?:/\d+)?|[½¼¾⅓⅔]|\d+\s*[~∼-]\s*\d+)'
AMOUNT = NUMBER + r'\s*([^\d\s()=]*)'
IN_PAREN = re.compile(r'^(.+?)\s*\(\s*(?:([^(),]+?)\s*,\s*)?' + AMOUNT + r'\s*\)$')
TRAILING = re.compile(r'^(.+?)\s*' + AMOUNT + r'$')
# 사과(1/4개=60g): 개수와 g이 함께 적힌 줄
EQUALS = re.compile(r'^(.+?)\s*\(\s*' + NUMBER + r'\s*([^\d\s()=]+)\s*=\s*' + NUMBER + r'\s*g\s*\)$')
# 파프리카(빨강, 노랑 각 ¼개): 색마다 같은 양
EACH = re.compile(r'^(.+?)\s*\(([^()]+?)\s*각각?\s*' + AMOUNT + r'\s*\)$')
# 검은깨 8g(1⅔작은술): 뒤 괄호는 다른 표기라 버린다
ALT = re.compile(r'^(.+?\d\s*[^\d\s()]*)\s*\([^()]*\)$')
TASTE = re.compile(r'^(.+?)\s*\(?\s*(' + '|'.join(TO_TASTE_WORDS) + r'|한\s*줌)\s*\)?$')
GLYPH = {'½': '1/2', '¼': '1/4', '¾': '3/4', '⅓': '1/3', '⅔': '2/3'}
GLYPH_VALUE = {'½': 0.5, '¼': 0.25, '¾': 0.75, '⅓': 1 / 3, '⅔': 2 / 3}


def servings_of(text):
    m = SERVINGS.search(text)
    return int(m.group(1) or m.group(2)) if m else 1


def split_outside_parens(text):
    parts, depth, cur = [], 0, ''
    for ch in text:
        if ch in '(（':
            depth += 1
        elif ch in ')）':
            depth = max(0, depth - 1)
        if ch == ',' and depth == 0:
            parts.append(cur)
            cur = ''
        else:
            cur += ch
    parts.append(cur)
    return parts


def tokens(text, title):
    text = SERVINGS.sub(',', text)
    text = text.replace('<br />', ',').replace('<br>', ',').replace('\n', ',')
    text = re.sub(r'\[[^\]]*\]', ',', text)
    text = re.sub(r'[●•]', ',', text)
    out = []
    for part in split_outside_parens(text):
        part = re.sub(r'^-\s*', '', part.strip())
        m = LABEL.match(part)
        if m:
            part = m.group(2).strip()
        elif ':' in part:  # 쉼표가 빠진 "물 400g - 부추양념장 : 다시마국물"
            left, right = part.rsplit(':', 1)
            left = re.sub(r'[-\s]*\S+\s*$', '', left.strip()).strip()
            out.extend(p for p in (left, right.strip()) if p and not HEADER.search(p))
            continue
        part = PREFIX.sub('', part)
        if not part or HEADER.search(part) or part.replace(' ', '') == title.replace(' ', ''):
            continue
        out.append(part)
    return out


def to_number(text):
    if len(text) > 1 and text[-1] in GLYPH_VALUE and text[:-1].isdigit():  # 1½
        return int(text[:-1]) + GLYPH_VALUE[text[-1]]
    text = GLYPH.get(text, text)
    m = re.match(r'^(\d+)\s*[~∼-]\s*(\d+)$', text)
    if m:  # 6~7 → 가운데 값
        return (float(m.group(1)) + float(m.group(2))) / 2
    return float(Fraction(text))


def parse_token(token):
    """재료 줄 하나 → (이름, 양, 단위). 못 읽으면 None."""
    m = EQUALS.match(token)
    if m:
        unit = UNIT_WORDS.get(m.group(3).strip(), (None,))[0]
        if unit and unit != 'PACK':
            return m.group(1).strip(), round(to_number(m.group(2)) * UNIT_WORDS[m.group(3).strip()][1], 3), unit
        return m.group(1).strip(), round(to_number(m.group(4)), 3), 'G'
    m = EACH.match(token)
    if m:
        parsed = parse_token(f'{m.group(1)} {m.group(3)}{m.group(4)}')
        count = len(split_outside_parens(m.group(2)))
        if parsed and parsed[1] is not None:
            return f'{m.group(1).strip()}({m.group(2).strip()})', round(parsed[1] * count, 3), parsed[2]
    token = re.sub(r'\s*각각?\s*(?=\d)', ' ', token)  # 검정깨묵 각각 25g
    m = ALT.match(token)
    if m and TRAILING.match(m.group(1)):  # 두부 120g(1/3모): 앞의 양을 쓴다
        token = m.group(1)
    m = IN_PAREN.match(token)
    if m:
        name = m.group(1) if not m.group(2) else f'{m.group(1)}({m.group(2)})'
        number, word = m.group(3), m.group(4)
        first = re.match(r'^' + AMOUNT + r'$', m.group(2) or '')
        if first:  # 저염베이컨(46g, 3장): 양이 두 개면 무게 쪽을 쓴다
            name = m.group(1)
            if UNIT_WORDS.get(first.group(2).strip(), (None,))[0] in WEIGHT or word.strip() not in UNIT_WORDS:
                number, word = first.group(1), first.group(2)
    else:
        m = TRAILING.match(token)
        if m:
            name, number, word = m.group(1), m.group(2), m.group(3)
        else:
            m = TASTE.match(token)
            if not m:
                return None
            return m.group(1).strip(), None, 'TO_TASTE'
    word = word.strip()
    if not re.search(r'[가-힣A-Za-z]', name):  # 앞 줄에서 떨어져 나온 숫자(포도씨유, 12g)
        return None
    if word in TO_TASTE_WORDS:
        return name.strip(), None, 'TO_TASTE'
    if word == 'cm':  # 생강 2cm: 길이라 양으로 세지 않고 이름에 남긴다
        return f'{name.strip()}({number}cm)', None, 'TO_TASTE'
    if word == '':
        unit, factor = 'G', 1  # 단위 없는 숫자는 g(J1)
    elif word in UNIT_WORDS:
        unit, factor = UNIT_WORDS[word]
    else:
        return None
    return name.strip(), round(to_number(number) * factor, 3), unit


# ---------------------------------------------------------------- 사전 이름

def match_catalog(name, catalog, fixes):
    """E1: 고침표 → 사전 이름과 같음 → 사전 이름이 들어 있으면 가장 긴 것(두 글자 이상). 없으면 None."""
    key = name.replace(' ', '')
    base = re.sub(r'\(.*?\)', '', key)
    for k in (name, key, base):
        if k in fixes:
            return fixes[k][0] or None, 'fix'
    for k in (key, base):
        if k in catalog:
            return k, 'exact'
    found = [c for c in catalog if len(c) >= 2 and c in base]
    if found:
        return max(found, key=len), 'contains'
    return None, 'none'


def canonical(name, synonyms):
    return synonyms.get(name, name)



# ---------------------------------------------------------------- 조합 검사 기준

def read_fixes():
    """name_fixes.tsv → {이름: (사전 이름 또는 None, 카테고리 또는 None)}."""
    fixes = OrderedDict()
    for row in read_tsv('name_fixes.tsv'):
        catalog_name = row[1] if len(row) > 1 and row[1] != '-' else ''
        category = row[2] if len(row) > 2 and row[2] else ''
        fixes[row[0]] = (catalog_name, category)
    return fixes


class Rules:
    def __init__(self):
        self.catalog = read_catalog()
        self.fixes = read_fixes()
        self.synonyms = OrderedDict((r[0], r[1]) for r in read_tsv('synonyms.tsv'))
        raw = json.loads((HERE / 'check_rules.json').read_text(encoding='utf-8'))
        self.groups = OrderedDict()
        for group, members in raw['groups'].items():
            for member in members:
                self.groups[member] = group
        self.skipped = set(raw['skippedNames'])
        c = raw['caution']
        caution_names = ({n for n, (cat, _) in self.catalog.items() if cat in c['categories']}
                         - set(c['exceptNames']) | set(c['names']))
        b = raw['breakfast']
        breakfast_names = {n for n, (cat, _) in self.catalog.items() if cat in b['categories']} | set(b['names'])
        self.excluded = raw['excludedFromAi']
        self.validate(caution_names, breakfast_names)
        # 서버는 검사 키(무리 이름 또는 사전 이름) 하나로 주의·아침 여부를 본다. 무리 안의 재료를 적으면 무리 전체가 그렇다
        self.caution = sorted({self.groups.get(n, n) for n in caution_names})
        self.breakfast = sorted({self.groups.get(n, n) for n in breakfast_names})

    def validate(self, caution_names, breakfast_names):
        known = set(self.catalog) | set(self.groups.values())
        for label, names in (('caution', caution_names), ('breakfast', breakfast_names),
                             ('excludedFromAi', self.excluded)):
            missing = sorted(n for n in names if n not in known)
            if missing:
                sys.exit(f'{label}에 사전에 없는 이름: {missing}')

    def key(self, name):
        """재료 이름 → (검사 키, 카테고리). 사전과 못 맞추거나 양념·물이면 None (서버 MealPlanCombinationRules.keyOf와 같다)."""
        name = name.strip()
        bare = re.sub(r'\(.*?\)', '', name).replace(' ', '')
        if bare.endswith('밥') and len(bare) <= RICE_MAX_LENGTH and bare not in RICE_EXCEPT:
            name = RICE
        matched = self.match(name)
        if not matched:
            return None
        catalog_name, category = matched
        catalog_name = canonical(catalog_name, self.synonyms)
        category = category or self.catalog.get(catalog_name, (None,))[0]
        key = self.groups.get(catalog_name, catalog_name)
        if category == SEASONING or key in self.skipped:
            return None
        return key, category

    def match(self, name):
        """고침표 → 사전 이름과 같음 → 사전 이름이 들어 있으면 가장 긴 것(같은 길이면 사전 순서가 앞선 것)."""
        compact = name.replace(' ', '')
        base = re.sub(r'\(.*?\)', '', compact)
        for k in (compact, base):
            if k in self.fixes:
                catalog_name, category = self.fixes[k]
                return (catalog_name, category) if catalog_name else None
        for k in (compact, base):
            if k in self.catalog:
                return k, None
        found = [c for c in self.catalog if len(c) >= MIN_CONTAINED_NAME_LENGTH and c in base]
        return (max(found, key=len), None) if found else None

    def keys(self, names):
        return {k[0] for k in (self.key(n) for n in names) if k}


def recipe_names(row):
    names = []
    for token in tokens(row['RCP_PARTS_DTLS'], row['RCP_NM']):
        parsed = parse_token(token)
        if parsed:
            names.append(parsed[0])
    return names


def build(csv_path, rules):
    rows = read_csv(csv_path)
    caution = set(rules.caution)
    partners = {name: set() for name in rules.caution}
    for row in rows:
        for a, b in itertools.combinations(sorted(rules.keys(recipe_names(row))), 2):
            if a in caution:
                partners[a].add(b)
            if b in caution:
                partners[b].add(a)
    data = OrderedDict()
    data['source'] = '식품의약품안전처 조리식품의 레시피 DB(COOKRCP01), scripts/mfds/build_combination_check.py로 만듦'
    data['recipeCount'] = len(rows)
    data['catalog'] = OrderedDict((n, cat) for n, (cat, _) in rules.catalog.items())
    data['synonyms'] = rules.synonyms
    data['aliases'] = OrderedDict((n, OrderedDict([('catalogName', c or None), ('categoryCode', cat or None)]))
                                  for n, (c, cat) in rules.fixes.items())
    data['groups'] = rules.groups
    data['skippedNames'] = sorted(rules.skipped)
    data['breakfast'] = rules.breakfast
    data['excludedFromAi'] = rules.excluded
    data['partners'] = OrderedDict((n, sorted(partners[n])) for n in rules.caution)
    return data


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('csv', help='COOKRCP01 CSV(.csv 또는 .csv.gz)')
    parser.add_argument('--out', default=str(DEFAULT_OUT))
    args = parser.parse_args()
    rules = Rules()
    data = build(args.csv, rules)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(data, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    pair_count = sum(len(v) for v in data['partners'].values())
    print(f'레시피 {data["recipeCount"]}개, 주의 재료 {len(data["partners"])}개, 주의 재료 짝 {pair_count}개 → {out}')


if __name__ == '__main__':
    main()
