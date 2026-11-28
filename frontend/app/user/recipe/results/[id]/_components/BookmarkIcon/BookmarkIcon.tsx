type BookmarkIconProps = {
  /** 저장 후: 속까지 채운다. 선과 채움 모두 currentColor라 색은 버튼이 정한다 */
  filled?: boolean;
};

/**
 * 피그마 p4.1.3 서브 헤더 오른쪽 lucide/bookmark(24, 선 1.5).
 * 아이콘은 Solar만 쓰는 게 규칙이지만(frontend/AGENTS.md), Solar 북마크 모양이 어색해 사용자 요청(2026-11-28)으로
 * 이 북마크만 lucide 모양을 쓴다. 라이브러리는 들이지 않고 lucide 원본 path(피그마 에셋과 같은 모양)만 옮겼다.
 */
export function BookmarkIcon({ filled = false }: BookmarkIconProps) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill={filled ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z" />
    </svg>
  );
}
