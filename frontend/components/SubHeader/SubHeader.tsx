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
   * 아이콘: <IconButton icon={<MenuDotsVerticalIcon />} label="더보기" />
   * 글자: <TextButton tone="danger">삭제</TextButton>
   */
  right?: ReactNode;
  className?: string;
};

/** 피그마 디자인 시스템 > 1. 앱 틀 > 서브 헤더. ← + 제목 + 오른쪽. */
export function SubHeader({ title, onBack, right, className }: SubHeaderProps) {
  const router = useRouter();
  return (
    <header className={cx(styles.header, className)}>
      <IconButton
        icon={<ArrowLeftIcon />}
        label="뒤로 가기"
        onClick={onBack ?? (() => router.back())}
      />
      <h1 className={styles.title}>{title}</h1>
      {right && <div className={styles.right}>{right}</div>}
    </header>
  );
}
