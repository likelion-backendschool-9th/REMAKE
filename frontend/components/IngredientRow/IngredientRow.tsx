import type { ReactNode } from "react";
import Image from "next/image";

import { AltArrowRightIcon } from "@solar-icons/react/linear";

import { IconButton } from "@/components/IconButton/IconButton";
import { cx } from "@/lib/cx";

import styles from "./IngredientRow.module.css";

type IngredientRowProps = {
  name: string;
  /** 왼쪽 그림(3:2). 보통 카테고리 기본 이미지 */
  imageSrc: string;
  /** 이름 아래 회색 글. 예: "냉동실 · 600g" */
  description?: string;
  /** 오른쪽 값. 피그마 오른쪽=소비기한(<ExpiryLabel />)·가격·수량·작은 버튼 */
  trailing?: ReactNode;
  /** 있으면 행 전체(그림·글·오른쪽 값)가 버튼이 된다. 냉장고: 재료 사용 시트 열기 */
  onClick?: () => void;
  /** 있으면 맨 오른쪽에 > 링크. 냉장고: 재료 수정으로 이동 */
  editHref?: string;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 7. 목록 행·카드 > 재료 행. 그림 44×29 + 이름(Remake/content) + 설명(Remake/assist) + 오른쪽 값.
 * 냉장고에서는 행을 누르면 사용·폐기 시트, 오른쪽 >를 누르면 수정 화면(domain-rules 2).
 */
export function IngredientRow({
  name,
  imageSrc,
  description,
  trailing,
  onClick,
  editHref,
  className,
}: IngredientRowProps) {
  const content = (
    <>
      <Image src={imageSrc} alt="" width={44} height={29} className={styles.image} />
      <span className={styles.text}>
        <span className={styles.name}>{name}</span>
        {description && <span className={styles.description}>{description}</span>}
      </span>
      {trailing && <span className={styles.trailing}>{trailing}</span>}
    </>
  );

  return (
    <div className={cx(styles.row, className)}>
      {onClick ? (
        <button type="button" className={styles.main} onClick={onClick}>
          {content}
        </button>
      ) : (
        <div className={styles.main}>{content}</div>
      )}
      {editHref && (
        <IconButton
          href={editHref}
          icon={<AltArrowRightIcon />}
          iconSize={16}
          label={`${name} 수정`}
          className={styles.edit}
        />
      )}
    </div>
  );
}
