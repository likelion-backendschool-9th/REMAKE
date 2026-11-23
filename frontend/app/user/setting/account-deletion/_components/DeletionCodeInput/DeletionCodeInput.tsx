"use client";

import { type ClipboardEvent, type KeyboardEvent, useId, useRef } from "react";

import { OtpCell } from "@/components/OtpCell/OtpCell";

import styles from "./DeletionCodeInput.module.css";

export const CODE_LENGTH = 6;

type DeletionCodeInputProps = {
  /** 칸마다 숫자 한 개(빈칸은 "") */
  digits: string[];
  onChange: (digits: string[]) => void;
  /** 틀린 번호: 칸을 빨갛게 한다. 문구는 화면이 오류 토스트로 보인다(에러 정의서 11/22) */
  isError?: boolean;
};

/**
 * "인증 번호" 라벨 + 6칸. 숫자를 넣으면 다음 칸으로, 빈칸에서 지우면 앞 칸으로 간다.
 * 6자리를 한 번에 붙여 넣거나 자동 완성(one-time-code)으로 받으면 앞에서부터 채운다.
 * 칸은 정사각형이고, 6칸이 줄 폭을 나눠 가져 화면이 넓어지면 함께 커진다(360 폭에서 46×46).
 */
export function DeletionCodeInput({ digits, onChange, isError = false }: DeletionCodeInputProps) {
  const labelId = useId();
  const cellsRef = useRef<Array<HTMLInputElement | null>>([]);

  const focusCell = (index: number) => {
    cellsRef.current[Math.max(0, Math.min(index, CODE_LENGTH - 1))]?.focus();
  };

  /** index 칸부터 숫자들을 채운다 */
  const fillFrom = (index: number, raw: string) => {
    const numbers = raw
      .replace(/\D/g, "")
      .slice(0, CODE_LENGTH - index)
      .split("");
    if (numbers.length === 0) return;
    const next = [...digits];
    numbers.forEach((digit, offset) => {
      next[index + offset] = digit;
    });
    onChange(next);
    focusCell(index + numbers.length);
  };

  const handleKeyDown = (index: number, event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Backspace" && digits[index] === "" && index > 0) {
      event.preventDefault();
      const next = [...digits];
      next[index - 1] = "";
      onChange(next);
      focusCell(index - 1);
    }
  };

  const handlePaste = (index: number, event: ClipboardEvent<HTMLInputElement>) => {
    event.preventDefault();
    fillFrom(index, event.clipboardData.getData("text"));
  };

  return (
    <div className={styles.field}>
      <p id={labelId} className={styles.label}>
        인증 번호
      </p>
      <div className={styles.cells} role="group" aria-labelledby={labelId}>
        {digits.map((digit, index) => (
          <OtpCell
            // 칸 6개는 순서가 바뀌지 않아 위치(index)를 key로 쓴다
            key={index}
            ref={(element) => {
              cellsRef.current[index] = element;
            }}
            value={digit}
            isError={isError}
            className={styles.cell}
            aria-label={`인증 번호 ${index + 1}번째 자리`}
            // 자동 완성은 첫 칸에서 6자리를 한 번에 넣는다
            maxLength={index === 0 ? CODE_LENGTH : 1}
            onChange={(event) => {
              const value = event.target.value;
              if (value === "") {
                const next = [...digits];
                next[index] = "";
                onChange(next);
                return;
              }
              // 이미 숫자가 있는 칸 끝에 이어 쳤으면 새로 친 글자만 쓴다
              fillFrom(index, digit && value.startsWith(digit) ? value.slice(digit.length) : value);
            }}
            onKeyDown={(event) => handleKeyDown(index, event)}
            onPaste={(event) => handlePaste(index, event)}
            onFocus={(event) => event.target.select()}
          />
        ))}
      </div>
    </div>
  );
}
