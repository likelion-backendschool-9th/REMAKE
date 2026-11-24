import { type ReactNode, useId } from "react";
import Link from "next/link";

import { AltArrowRightIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./SettingRow.module.css";

type BaseProps = {
  label: string;
  /** 라벨 옆 빨간 * (필수 항목) */
  isRequired?: boolean;
  className?: string;
};

type NavigableProps = BaseProps & {
  /**
   * 피그마 오른쪽=값: 지금 값. 없으면 placeholder("선택하지 않음")를 회색으로 보인다.
   * 피그마 오른쪽=화살표만: value와 placeholder를 모두 생략하고 showValue={false}.
   */
  value?: string;
  placeholder?: string;
  showValue?: boolean;
  /** 누르면 이동(링크) */
  href?: string;
  /** 누르면 동작(시트 열기 등) */
  onClick?: () => void;
  control?: never;
};

type ControlProps = BaseProps & {
  /**
   * 피그마 오른쪽=스테퍼/토글: 오른쪽에 둘 컨트롤.
   * 라벨과 연결되도록 함수로 받아 aria-labelledby용 id를 넘겨준다.
   * 예: control={(labelId) => <Switch aria-labelledby={labelId} ... />}
   */
  control: (labelId: string) => ReactNode;
  value?: never;
  placeholder?: never;
  showValue?: never;
  href?: never;
  onClick?: never;
};

type SettingRowProps = NavigableProps | ControlProps;

/**
 * 피그마 디자인 시스템 > 7. 목록 행·카드 > 설정형 행. 라벨 + 오른쪽(값/스테퍼/토글/화살표만).
 * 설정, 재료 수정·추가, 커스텀 레시피, 식단표 설정에서 쓴다.
 */
export function SettingRow(props: SettingRowProps) {
  const labelId = useId();
  const { label, isRequired = false, className } = props;

  const labelNode = (
    <span className={styles.label} id={labelId}>
      {label}
      {isRequired && (
        <>
          <span className={styles.required} aria-hidden="true">
            *
          </span>
          <span className={styles.srOnly}>(필수)</span>
        </>
      )}
    </span>
  );

  if (props.control) {
    return (
      <div className={cx(styles.row, className)}>
        {labelNode}
        {props.control(labelId)}
      </div>
    );
  }

  const { value, placeholder = "선택하지 않음", showValue = true, href, onClick } = props;
  const content = (
    <>
      {labelNode}
      {showValue && (
        <span className={styles.value} data-empty={!value || undefined}>
          {value || placeholder}
        </span>
      )}
      <AltArrowRightIcon className={styles.arrow} aria-hidden="true" />
    </>
  );

  if (href) {
    return (
      <Link href={href} className={cx(styles.row, styles.interactive, className)}>
        {content}
      </Link>
    );
  }

  return (
    <button
      type="button"
      className={cx(styles.row, styles.interactive, className)}
      onClick={onClick}
    >
      {content}
    </button>
  );
}
