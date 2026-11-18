"""Summarize backend test results in the GitHub Actions job summary.

Reads Gradle's JUnit XML results (build/test-results/test/TEST-*.xml) and reports the total test
time, the slowest test classes, and how many times a Spring application context started.
A context start count close to the number of test classes means the context cache is not reused,
which makes CI slower with every new integration test. It prints a warning annotation when a
threshold is exceeded; it never fails the job.
"""

import argparse
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

# Spring Boot logs this line every time an application context starts.
SPRING_STARTED = re.compile(r"Started \S+ in ([0-9.]+) seconds")
SLOWEST_COUNT = 5


def parse_suites(results_dir: Path) -> list[dict]:
    suites = []
    for path in sorted(results_dir.glob("TEST-*.xml")):
        root = ET.parse(path).getroot()
        output = root.findtext("system-out") or ""
        boots = [float(seconds) for seconds in SPRING_STARTED.findall(output)]
        suites.append(
            {
                "name": root.get("name", path.stem).removeprefix("com.remake."),
                "tests": int(root.get("tests", 0)),
                "failed": int(root.get("failures", 0)) + int(root.get("errors", 0)),
                "time": float(root.get("time", 0)),
                "boots": len(boots),
                "boot_time": sum(boots),
            }
        )
    return suites


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("results_dir", type=Path)
    parser.add_argument("--max-spring-boots", type=int, required=True)
    parser.add_argument("--max-test-seconds", type=float, required=True)
    args = parser.parse_args()

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    out = open(summary_path, "a", encoding="utf-8") if summary_path else sys.stdout

    suites = parse_suites(args.results_dir) if args.results_dir.is_dir() else []
    print("### 백엔드 테스트 요약\n", file=out)
    if not suites:
        # 컴파일 실패처럼 테스트가 실행되지 않은 경우
        print("테스트 결과 파일이 없습니다. 테스트가 실행되기 전에 빌드가 멈췄는지 확인하세요.", file=out)
        return 0

    total_time = sum(s["time"] for s in suites)
    boots = sum(s["boots"] for s in suites)
    boot_time = sum(s["boot_time"] for s in suites)
    print("| 항목 | 값 |\n| --- | --- |", file=out)
    print(f"| 테스트 클래스 / 테스트 | {len(suites)} / {sum(s['tests'] for s in suites)} |", file=out)
    print(f"| 실패한 테스트 | {sum(s['failed'] for s in suites)} |", file=out)
    print(f"| 테스트 전체 시간 | {total_time:.1f}초 (경고 기준 {args.max_test_seconds:.0f}초) |", file=out)
    print(f"| Spring 앱 기동 횟수 | {boots}회, 합계 {boot_time:.1f}초 (경고 기준 {args.max_spring_boots}회) |", file=out)

    print(f"\n#### 느린 테스트 클래스 상위 {SLOWEST_COUNT}개\n", file=out)
    print("| 클래스 | 시간 | 테스트 수 | Spring 기동 |\n| --- | --- | --- | --- |", file=out)
    for s in sorted(suites, key=lambda s: s["time"], reverse=True)[:SLOWEST_COUNT]:
        print(f"| `{s['name']}` | {s['time']:.1f}초 | {s['tests']} | {s['boots']} |", file=out)

    # 경고는 PR 화면에 노란 표시로 뜬다. 머지는 막지 않는다.
    if boots > args.max_spring_boots:
        print(
            f"::warning title=Spring 앱 기동 {boots}회::"
            f"테스트 중 Spring 앱이 {boots}번 떴습니다(기준 {args.max_spring_boots}회). "
            "통합 테스트마다 설정이 달라 앱을 재사용하지 못하고 있을 수 있습니다. 작업 요약의 표를 확인하세요."
        )
    if total_time > args.max_test_seconds:
        print(
            f"::warning title=테스트 {total_time:.0f}초::"
            f"백엔드 테스트가 {total_time:.0f}초 걸렸습니다(기준 {args.max_test_seconds:.0f}초). "
            "작업 요약의 느린 테스트 클래스를 확인하세요."
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
