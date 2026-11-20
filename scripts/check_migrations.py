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

Usage: python3 scripts/check_migrations.py <base ref> <head ref>
"""

import re
import subprocess
import sys

MIGRATION_DIR = "backend/src/main/resources/db/migration/"
# Same format as the backend convention. No leading zero: Flyway reads V01 and V1 as the same version.
NAME = re.compile(r"^V([1-9][0-9]*)__[A-Za-z0-9_]+\.sql$")


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], text=True, encoding="utf-8")


# Paths are read with -z (NUL-separated). Otherwise git quotes non-ASCII names ("V2__\355...sql") and they slip past the checks.
def sql_files(ref: str) -> list[str]:
    out = git("ls-tree", "-r", "-z", "--name-only", ref, "--", MIGRATION_DIR)
    return [path for path in out.split("\0") if path.endswith(".sql")]


def version(path: str) -> int | None:
    match = NAME.match(path.rsplit("/", 1)[-1])
    return int(match.group(1)) if match else None


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

    errors = []
    added = []
    for status, path in changes:
        if not path.endswith(".sql"):
            continue
        if path in branch_point_files or path in base_files:
            errors.append(f"{path}: dev에 이미 있는(서버에 적용된) 파일은 고치거나 지우거나 이름을 바꿀 수 없습니다. "
                          "바꿀 내용은 새 번호 파일로 추가하세요.")
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

    for error in errors:
        print(f"::error::{error}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
