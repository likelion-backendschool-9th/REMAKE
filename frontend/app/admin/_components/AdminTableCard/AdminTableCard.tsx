import { Badge, type BadgeTone } from "@/components/Badge/Badge";
import { cx } from "@/lib/cx";

import styles from "./AdminTableCard.module.css";

/** 칸 값: 글만 있거나, 글 옆에 배지(예: "달성")가 붙는다 */
export type AdminTableCell = string | { text: string; badge?: { tone: BadgeTone; label: string } };

export type AdminTableColumn = {
  key: string;
  label: string;
  /** 숫자 칸은 오른쪽 맞춤. 기본 왼쪽 */
  align?: "left" | "right";
  /** 칸 폭(CSS 길이). 없으면 남는 폭을 나눠 가진다 */
  width?: string;
};

type AdminTableCardProps = {
  title: string;
  /** 제목 오른쪽 작은 글(지표 번호·집계 기준 등) */
  meta?: string;
  columns: AdminTableColumn[];
  rows: Record<string, AdminTableCell>[];
  /** 표 아래 보조 글 */
  note?: string;
  /** 행 위아래 여백을 14로 넓힌다(기본 12). 피그마 a3 AI 탭 표가 이 모양이다 */
  relaxed?: boolean;
  /** 카드가 옆 카드 높이만큼 늘어날 때 표 영역이 남는 높이를 차지해 보조 글이 카드 맨 아래에 붙는다 */
  fillHeight?: boolean;
  className?: string;
};

/**
 * 운영자 화면의 표 카드(피그마 a3 표): 제목 줄 + 머리줄 + 행. 칸 폭은 열마다 정하고 폭이 없는 열이 남는 폭을 나눈다.
 * 행이 늘어나면 카드가 같이 길어진다. 행 칸은 ARIA table 역할로 읽힌다.
 * 옵션: `relaxed`(행 여백 14), `fillHeight`(옆 카드와 높이를 맞출 때 보조 글을 맨 아래에 붙임)
 */
export function AdminTableCard({
  title,
  meta,
  columns,
  rows,
  note,
  relaxed = false,
  fillHeight = false,
  className,
}: AdminTableCardProps) {
  // 칸 폭: 폭이 있으면 그 길이, 없으면 남는 폭(1fr). 칸 간격 16은 CSS에서 준다
  const gridTemplateColumns = columns.map((column) => column.width ?? "minmax(0, 1fr)").join(" ");

  return (
    <section
      className={cx(styles.card, className)}
      data-relaxed={relaxed || undefined}
      data-fill={fillHeight || undefined}
    >
      <div className={styles.header}>
        <h2 className={styles.title}>{title}</h2>
        {meta && <p className={styles.meta}>{meta}</p>}
      </div>
      <div className={styles.table} role="table" aria-label={title}>
        <div className={cx(styles.row, styles.head)} role="row" style={{ gridTemplateColumns }}>
          {columns.map((column) => (
            <span
              key={column.key}
              className={styles.cell}
              data-align={column.align}
              role="columnheader"
            >
              {column.label}
            </span>
          ))}
        </div>
        {rows.map((row, rowIndex) => (
          <div key={rowIndex} className={styles.row} role="row" style={{ gridTemplateColumns }}>
            {columns.map((column) => (
              <TableCell key={column.key} cell={row[column.key]} align={column.align} />
            ))}
          </div>
        ))}
      </div>
      {note && <p className={styles.note}>{note}</p>}
    </section>
  );
}

function TableCell({ cell, align }: { cell: AdminTableCell; align: AdminTableColumn["align"] }) {
  const { text, badge } = typeof cell === "string" ? { text: cell, badge: undefined } : cell;
  return (
    <span className={styles.cell} data-align={align} role="cell">
      {text}
      {badge && <Badge tone={badge.tone}>{badge.label}</Badge>}
    </span>
  );
}
