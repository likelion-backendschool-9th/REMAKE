import { CloseIcon } from "@solar-icons/react/linear";

import { IconButton } from "@/components/IconButton/IconButton";
import { cx } from "@/lib/cx";

import styles from "./SheetHeader.module.css";

type SheetHeaderProps = {
  title: string;
  /** 시트의 aria-labelledby가 가리킬 제목 id */
  titleId: string;
  onClose: () => void;
  className?: string;
};

/** 피그마 디자인 시스템 > 6. 겹쳐 뜨는 것 > 시트 제목. 바텀시트·입력 시트 맨 위 제목 + 닫기. */
export function SheetHeader({ title, titleId, onClose, className }: SheetHeaderProps) {
  return (
    <div className={cx(styles.header, className)}>
      <h2 id={titleId} className={styles.title}>
        {title}
      </h2>
      <IconButton icon={<CloseIcon />} label="닫기" onClick={onClose} />
    </div>
  );
}
