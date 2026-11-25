"use client";

import type { ReactNode } from "react";
import { useRouter } from "next/navigation";

import { ArrowLeftIcon } from "@solar-icons/react/linear";

import { IconButton } from "@/components/IconButton/IconButton";
import { cx } from "@/lib/cx";

import styles from "./SubHeader.module.css";

type SubHeaderProps = {
  /** 화면 제목. 화면의 h1이 된다. 없으면 제목 칸을 비운다. */
  title?: string;
  /** 뒤로 가기 동작. 기본은 router.back() */
  onBack?: () => void;
  /**
   * 오른쪽 영역(피그마 오른쪽=없음/아이콘/글자).
   * 아이콘: <IconButton icon={<MenuDotsVerticalIcon />} iconSize={16} label="더보기" /> (bold 스타일, 크기는 IconButton 참고)
   * 글자: <TextButton tone="danger">삭제</TextButton>
   */
  right?: ReactNode;
  /** 제목 요소. 화면 위에 겹쳐 뜨는 드로워 안에서는 "h2"(화면의 h1은 하나만) */
  titleAs?: "h1" | "h2";
  /** 제목 id. 드로워의 aria-labelledby에 쓴다 */
  titleId?: string;
  className?: string;
};

/** 피그마 디자인 시스템 > 1. 앱 틀 > 서브 헤더. ← + 제목 + 오른쪽. */
export function SubHeader({
  title,
  onBack,
  right,
  titleAs: Title = "h1",
  titleId,
  className,
}: SubHeaderProps) {
  const router = useRouter();
  return (
    <header className={cx(styles.header, className)}>
      <IconButton
        icon={<ArrowLeftIcon strokeWidth={2} />}
        iconSize={18}
        align="start"
        label="뒤로 가기"
        onClick={onBack ?? (() => router.back())}
      />
      {/* 제목이 없으면(화면 본문에 h1이 있는 경우) 빈 제목 요소를 만들지 않고 자리만 채운다 */}
      {title ? (
        <Title id={titleId} className={styles.title}>
          {title}
        </Title>
      ) : (
        <span className={styles.title} />
      )}
      {right && <div className={styles.right}>{right}</div>}
    </header>
  );
}
