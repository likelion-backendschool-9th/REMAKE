"use client";

import { useState } from "react";

import { Button } from "@/components/Button/Button";
import { TextButton } from "@/components/TextButton/TextButton";
import { Toast } from "@/components/Toast/Toast";
import { useTimedToast } from "@/hooks/useTimedToast";
import { getErrorMessageByCode } from "@/lib/error-messages";

import { QuestionLayout } from "../../../../_components/QuestionLayout/QuestionLayout";
import { CODE_LENGTH, DeletionCodeInput } from "../DeletionCodeInput/DeletionCodeInput";
import { formatCountdown, useCountdown } from "./useCountdown";

import styles from "./AccountDeletionContainer.module.css";

/** 인증번호 유효 시간 5분(정책). 피그마는 보낸 직후 04:59 */
const CODE_VALID_SECONDS = 299;
const EMPTY_DIGITS = Array.from({ length: CODE_LENGTH }, () => "");

type AccountDeletionContainerProps = {
  /** 처음 입력값. 미리보기용, 기본 빈칸 */
  defaultDigits?: string[];
  /** 처음부터 보일 오류 code. 미리보기용(p1.3.3-계정 삭제-실패). 예: "DELETION_CODE_INVALID" */
  defaultErrorCode?: string;
};

/**
 * p1.3.3 계정 삭제 이메일 인증번호(카카오·구글 회원). 6자리를 다 넣어야 "계정 삭제"가 활성이 된다.
 * 틀림·만료·횟수 초과는 칸을 빨갛게 하고 오류 토스트로 알린다(에러 정의서 11/22: 칸 아래 빨간 글자 대신 토스트).
 * 상태: 빈칸(p1.3.3-계정 삭제) / 입력 중(포커스) / 6자리(액티브) / 틀림(실패).
 */
export function AccountDeletionContainer({
  defaultDigits = EMPTY_DIGITS,
  defaultErrorCode,
}: AccountDeletionContainerProps) {
  const [digits, setDigits] = useState(defaultDigits);
  const [errorCode, setErrorCode] = useState(defaultErrorCode);
  // 오류 토스트: 아래에서 올라와 3초 뒤 내려가며 사라진다
  const toast = useTimedToast<string>(defaultErrorCode ?? null);
  const { seconds, restart } = useCountdown(CODE_VALID_SECONDS);
  const isComplete = digits.every((digit) => digit !== "");

  const handleChange = (next: string[]) => {
    setDigits(next);
    setErrorCode(undefined);
    // 칸이 다시 평소 색이 되므로 오류 토스트도 내린다
    toast.dismiss();
  };

  const handleResend = () => {
    // TODO: API 9(POST /user/account/deletion-code)로 새 번호를 받고 expiresAt으로 남은 시간을 맞춘다
    setDigits(EMPTY_DIGITS);
    setErrorCode(undefined);
    toast.dismiss();
    restart(CODE_VALID_SECONDS);
  };

  const handleDelete = () => {
    // TODO: API 10({ method: "EMAIL_CODE", code })으로 삭제. 성공하면 로그인 화면으로.
    // DELETION_CODE_INVALID·EXPIRED·LOCKED면 setErrorCode(code)와 toast.show(code)로 알린다
  };

  return (
    <>
      <QuestionLayout
        title="계정 삭제를 위해 인증 번호를 전송했습니다"
        description="인증 번호 6자리를 이메일로 전송했습니다"
        footer={
          <Button tone="danger" disabled={!isComplete} onClick={handleDelete}>
            계정 삭제
          </Button>
        }
      >
        <div className={styles.body}>
          <DeletionCodeInput digits={digits} onChange={handleChange} isError={Boolean(errorCode)} />
          <div className={styles.resend}>
            <p className={styles.timer}>
              <span className={styles.srOnly}>남은 시간 </span>
              <time>{formatCountdown(seconds)}</time>
            </p>
            <TextButton tone="danger" className={styles.resendButton} onClick={handleResend}>
              재전송하기
            </TextButton>
          </div>
        </div>
      </QuestionLayout>

      {/* 문구를 바꿔 끼우면 읽히도록 항상 그려 두는 영역(blueprint §6.3) */}
      <div className={styles.toastArea} role="status" aria-live="polite">
        {toast.value && (
          <Toast variant="error" isLeaving={toast.isLeaving} onLeft={toast.remove}>
            {getErrorMessageByCode(toast.value)}
          </Toast>
        )}
      </div>
    </>
  );
}
