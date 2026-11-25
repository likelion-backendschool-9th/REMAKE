"use client";

import type { PointerEvent } from "react";

import { CloseCircleIcon } from "@solar-icons/react/bold";

import { cx } from "@/lib/cx";

import styles from "./InputClearButton.module.css";

type InputClearButtonProps = {
  /** 스크린 리더용 이름. 예: "검색어 지우기", "레시피 제목 지우기" */
  label: string;
  className?: string;
};

/** 입력칸의 값을 사용자가 지운 것처럼 비운다. React의 onChange가 불리므로 useState 값이든 React Hook Form의 register든 같이 비워진다 */
function clearInput(input: HTMLInputElement) {
  // value를 그냥 바꾸면 React가 바뀐 줄 모른다. 원래 setter로 바꾼 뒤 input 이벤트를 보내야 onChange가 불린다
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")?.set?.call(input, "");
  input.dispatchEvent(new Event("input", { bubbles: true }));
}

/**
 * 입력칸 오른쪽 지우기(✕) 버튼. 피그마 검색창 값 있음(5780-4568)·입력 필드 완료(5991-7263).
 * TextField·SearchField 안에서 입력칸과 같은 상자에 넣는다(같은 상자의 input을 비움).
 * 값이 없을 때 숨기는 건 넣는 쪽 CSS(:placeholder-shown)가 맡는다.
 */
export function InputClearButton({ label, className }: InputClearButtonProps) {
  return (
    <button
      type="button"
      className={cx(styles.clear, className)}
      aria-label={label}
      // 눌러도 입력창 포커스(모바일 키보드)가 빠지지 않게 한다
      onPointerDown={(event: PointerEvent) => event.preventDefault()}
      onClick={(event) => {
        const input = event.currentTarget.parentElement?.querySelector("input");
        if (!input) return;
        clearInput(input);
        input.focus();
      }}
    >
      <CloseCircleIcon className={styles.icon} aria-hidden="true" />
    </button>
  );
}
