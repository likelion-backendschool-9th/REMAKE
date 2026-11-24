"""Check a pull request's Flyway migrations against the latest base branch.

Backend CI tests start from an empty database, so they pass even when a migration would fail on the
development server, whose database already has every migration merged into dev applied.
This script compares the PR with the base branch as it is now (not when the PR was opened) and fails when:

- a new migration file name is not V<number>__<description>.sql
- a new migration number is not greater than the base branch's last number (duplicate or out-of-order number)
- two new files in the PR use the same number
- the PR changes, deletes, or renames a migration file that is already on the base branch

Every file on dev is treated as applied on the server, because each merge into dev is deployed.
Re-running the check right before merging compares with the latest dev again without a new push.
(.github/workflows/migration-recheck.yml re-runs it on open PRs whenever a migration lands on dev.)

One exception: if two files on the base branch share a number (a PR passed the check before another PR with the same
number merged), Backend CD stops at its tests from that merge on, so the file that arrived later was never applied.
A PR may delete or rename that later file (revert, or move it to a new number). The earlier one stays protected.

It also warns, without failing, when a new migration contains a statement that breaks the previous version
still running during a deployment or that cannot be undone (see RISKY below).

Usage: python3 scripts/check_migrations.py <base ref> <head ref>
"""

import os
import re
import subprocess
import sys

MIGRATION_DIR = "backend/src/main/resources/db/migration/"
# Same format as the backend convention. No leading zero: Flyway reads V01 and V1 as the same version.
NAME = re.compile(r"^V([1-9][0-9]*)__[A-Za-z0-9_]+\.sql$")

# During a deployment the previous version keeps serving on the same DB for a few tens of seconds after the new slot
# applies the migration, and a backend rollback starts the previous image on the new schema. These statements break
# the previous version or destroy data, so they get a yellow warning. UPDATE and DROP INDEX are left out: they are
# common and do not break the previous version. This is a text search, so unusual forms (DROP without COLUMN) can slip through.
RISKY = [
    (re.compile(r"\bDROP\s+TABLE\b", re.I), "표 삭제(DROP TABLE)"),
    (re.compile(r"\bDROP\s+COLUMN\b", re.I), "열 삭제(DROP COLUMN)"),
    (re.compile(r"\bRENAME\b", re.I), "이름 변경(RENAME)"),
    (re.compile(r"\bALTER\s+(?:COLUMN\s+)?\S+\s+(?:SET\s+DATA\s+)?TYPE\b", re.I), "열 타입 변경(ALTER COLUMN ... TYPE)"),
    (re.compile(r"\bSET\s+NOT\s+NULL\b", re.I), "NOT NULL 추가(SET NOT NULL)"),
    (re.compile(r"\bDELETE\s+FROM\b", re.I), "행 삭제(DELETE)"),
    (re.compile(r"\bTRUNCATE\b", re.I), "행 전체 삭제(TRUNCATE)"),
]
RISKY_MESSAGE = (
    "배포 중 이전 버전과 함께 돌 때 오류가 나거나 되돌릴 수 없는 변경입니다. "
    "새 슬롯이 이 마이그레이션을 적용한 뒤 수십 초 동안 이전 버전이 이 테이블을 쓰다 오류를 낼 수 있고, "
    "이 배포 뒤에는 백엔드 롤백이 되지 않습니다(서버는 적용 직전에 DB를 백업합니다). "
    "데이터가 있는 테이블이면 머지 전에 배포 담당과 순서를 정하세요. "
    "예: 먼저 코드에서 쓰지 않게 배포하고, 지우는 마이그레이션은 다음 PR에서."
)


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], text=True, encoding="utf-8", errors="replace")


def strip_comments(sql: str) -> str:
    """Blank out /* */ and -- comments, keeping line breaks so that line numbers stay the same."""
    sql = re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), sql, flags=re.S)
    return re.sub(r"--[^\n]*", "", sql)


def risky_statements(sql: str) -> list[tuple[int, str]]:
    """(line number, label) for each risky statement. Statements may span lines."""
    text = strip_comments(sql)
    found = []
    for pattern, label in RISKY:
        for match in pattern.finditer(text):
            found.append((text.count("\n", 0, match.start()) + 1, label))
    return sorted(found)


# GitHub workflow command values: % and line breaks are escaped everywhere, : and , also inside properties.
def escape_data(value: str) -> str:
    return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def escape_property(value: str) -> str:
    return escape_data(value).replace(":", "%3A").replace(",", "%2C")


def report_risky(head: str, added: list[str]) -> None:
    warnings = []
    for path in sorted(added):
        for line, label in risky_statements(git("show", f"{head}:{path}")):
            warnings.append((path, line, label))
    for path, line, label in warnings:
        title = escape_property(f"되돌리기 어려운 마이그레이션 {label}")
        print(f"::warning file={escape_property(path)},line={line},title={title}::{escape_data(RISKY_MESSAGE)}")
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if warnings and summary:
        with open(summary, "a", encoding="utf-8") as out:
            out.write("### 되돌리기 어려운 마이그레이션\n\n")
            out.write(f"{RISKY_MESSAGE}\n\n| 파일 | 줄 | 변경 |\n| --- | --- | --- |\n")
            for path, line, label in warnings:
                out.write(f"| `{path}` | {line} | {label} |\n")


# Paths are read with -z (NUL-separated). Otherwise git quotes non-ASCII names ("V2__\355...sql") and they slip past the checks.
def sql_files(ref: str) -> list[str]:
    out = git("ls-tree", "-r", "-z", "--name-only", ref, "--", MIGRATION_DIR)
    return [path for path in out.split("\0") if path.endswith(".sql")]


def version(path: str) -> int | None:
    match = NAME.match(path.rsplit("/", 1)[-1])
    return int(match.group(1)) if match else None


def first_parent_position(base: str, path: str) -> int | None:
    """How far along the base branch's first-parent history the commit that added path is (larger = later)."""
    commits = git("log", "--first-parent", "--diff-filter=A", "--format=%H", base, "--", path).split()
    if not commits:
        return None
    return int(git("rev-list", "--first-parent", "--count", commits[-1]).strip())


def late_duplicates(base: str, base_files: set[str]) -> set[str]:
    """Files on base whose number is also used by a file that reached base earlier.

    The merge that added such a file left two migrations with one number, so the app cannot start from that commit on
    and Backend CD stops at its tests: the file was never applied on the server. If the order cannot be told
    (history missing, or both added by the same commit), nothing is returned and every file stays protected.
    """
    by_version: dict[int, list[str]] = {}
    for path in base_files:
        v = version(path)
        if v is not None:
            by_version.setdefault(v, []).append(path)
    late = set()
    for paths in by_version.values():
        if len(paths) < 2:
            continue
        positions = {path: first_parent_position(base, path) for path in paths}
        if None in positions.values():
            continue
        earliest = min(positions.values())
        if list(positions.values()).count(earliest) == 1:
            late.update(path for path, position in positions.items() if position != earliest)
    return late


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__.strip().splitlines()[-1], file=sys.stderr)
        return 2
    base, head = sys.argv[1], sys.argv[2]

    # What the PR itself changed: compare with the point where the branch left the base branch.
    # Without rename detection a rename shows up as a delete plus an add, so both rules below apply.
    merge_base = git("merge-base", base, head).strip()
    fields = git("diff", "--name-status", "-z", "--no-renames", merge_base, head, "--", MIGRATION_DIR).split("\0")
    changes = list(zip(fields[0::2], fields[1::2]))  # (status, path) pairs
    base_files = set(sql_files(base))
    branch_point_files = set(sql_files(merge_base))

    late = late_duplicates(base, base_files)

    errors = []
    added = []
    for status, path in changes:
        if not path.endswith(".sql"):
            continue
        if status == "D" and path in late:
            print(f"::notice::{path}: {base}에 같은 번호 파일이 먼저 들어와 있어 이 파일은 서버에 적용되지 않았습니다. "
                  "지우거나 새 번호로 옮기는 것을 허용합니다.")
            continue
        if path in branch_point_files or path in base_files:
            same_number_late = sorted(p for p in late if version(p) == version(path) and p != path)
            hint = (f" 번호 겹침을 고치려면 나중에 들어온 {', '.join(same_number_late)}를 지우거나 새 번호로 옮기세요."
                    if same_number_late and status == "D" else "")
            errors.append(f"{path}: dev에 이미 있는(서버에 적용된) 파일은 고치거나 지우거나 이름을 바꿀 수 없습니다. "
                          f"바꿀 내용은 새 번호 파일로 추가하세요.{hint}")
        elif status == "A":
            added.append(path)

    base_versions = [v for v in (version(p) for p in base_files) if v is not None]
    last = max(base_versions, default=0)
    seen: dict[int, str] = {}
    for path in sorted(added):
        v = version(path)
        if v is None:
            errors.append(f"{path}: 파일 이름은 V<번호>__<설명>.sql 형식이어야 합니다. "
                          "(번호는 0으로 시작하지 않는 정수, 설명은 영문·숫자·밑줄)")
            continue
        if v <= last:
            errors.append(f"{path}: {base}의 마지막 번호가 V{last}입니다. V{last + 1} 이상으로 바꾸세요. "
                          "(같은 번호는 앱이 뜨지 않고, 더 작은 번호는 서버 DB에 적용되지 못합니다)")
        if v in seen:
            errors.append(f"{path}: {seen[v]}와 번호(V{v})가 같습니다.")
        seen[v] = path

    if added and not errors:
        last_text = f"마지막 번호 V{last}" if last else "기존 마이그레이션 없음"
        print(f"새 마이그레이션 {len(added)}개 확인 ({base}의 {last_text}):")
        for path in sorted(added):
            print(f"  {path}")
    elif not errors:
        print("바뀐 마이그레이션 파일이 없습니다.")

    report_risky(head, added)
    for error in errors:
        print(f"::error::{error}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
