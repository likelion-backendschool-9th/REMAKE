import type { ReactNode } from "react";

import { Button } from "@/components/Button/Button";
import { ResultState } from "@/components/ResultState/ResultState";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { TextButton } from "@/components/TextButton/TextButton";
import { isApiError } from "@/lib/api/errors";
import { getErrorMessage, getErrorMessageByCode } from "@/lib/error-messages";
import { SETTINGS_HREF } from "@/lib/navigation";

import styles from "./RecipeGenerationStatus.module.css";

type RecipeGenerationStatusProps = {
  status: "loading" | "error";
  /** 서브 헤더 제목 */
  headerTitle?: string;
  /** status가 "error"일 때의 오류. code로 화면을 나눈다(message는 쓰지 않음) */
  error?: unknown;
  /** 있으면 "다시 시도" 버튼을 보인다(횟수 소진·재료 부족에는 보이지 않음) */
  onRetry?: () => void;
  /** 뒤로 가기(서브 헤더 ←)와 돌아가기 버튼 */
  onBack: () => void;
  backLabel?: string;
};

/**
 * 빠른 생성(p4.1)·커스텀 생성(p4.1.2)·다시 받기가 함께 쓰는 생성 중·실패·횟수 소진 화면.
 * 오류 code별 화면: GENERATION_LIMIT_EXCEEDED·NO_INGREDIENTS(안내, 재시도 없음),
 * AI_TIMEOUT·CLIENT_TIMEOUT(오래 걸림), 그 밖(만들지 못함).
 */
export function RecipeGenerationStatus({
  status,
  headerTitle = "레시피 생성",
  error,
  onRetry,
  onBack,
  backLabel = "돌아가기",
}: RecipeGenerationStatusProps) {
  return (
    <>
      <SubHeader title={headerTitle} onBack={onBack} />
      <main className={styles.main}>
        {status === "loading" ? (
          <ResultState
            variant="loading"
            title="레시피를 만들고 있어요"
            description="냉장고 재료에 맞는 레시피를 준비하고 있어요. 잠시만 기다려 주세요"
          />
        ) : (
          <ErrorState error={error} onRetry={onRetry} onBack={onBack} backLabel={backLabel} />
        )}
      </main>
    </>
  );
}

function ErrorState({
  error,
  onRetry,
  onBack,
  backLabel,
}: Pick<RecipeGenerationStatusProps, "error" | "onRetry" | "onBack"> & { backLabel: string }) {
  const code = isApiError(error) ? error.code : null;

  if (code === "GENERATION_LIMIT_EXCEEDED") {
    return (
      <ResultState
        variant="info"
        title="오늘의 레시피 생성 횟수를 모두 사용했어요"
        description="내일 다시 이용하거나 멤버십을 확인해 주세요"
        action={<BackButton onBack={onBack} label={backLabel} isOnly />}
      />
    );
  }

  if (code === "NO_INGREDIENTS") {
    return (
      <ResultState
        variant="info"
        title="냉장고 재료가 부족해요"
        description={getErrorMessageByCode("NO_INGREDIENTS")}
        action={
          <Actions>
            <TextButton href={SETTINGS_HREF}>설정으로 이동</TextButton>
            <BackButton onBack={onBack} label={backLabel} isOnly={false} />
          </Actions>
        }
      />
    );
  }

  const isSlow = code === "AI_TIMEOUT" || code === "CLIENT_TIMEOUT";
  return (
    <ResultState
      variant="error"
      title={isSlow ? "생성이 오래 걸리고 있어요" : "레시피를 만들지 못했어요"}
      description={getErrorMessage(error)}
      action={
        <Actions>
          {onRetry && <Button onClick={onRetry}>다시 시도</Button>}
          <BackButton onBack={onBack} label={backLabel} isOnly={!onRetry} />
        </Actions>
      }
    />
  );
}

function Actions({ children }: { children: ReactNode }) {
  return <div className={styles.actions}>{children}</div>;
}

/** 다른 동작이 없으면 큰 버튼, 있으면 글자 버튼으로 그린다 */
function BackButton({
  onBack,
  label,
  isOnly,
}: {
  onBack: () => void;
  label: string;
  isOnly: boolean;
}) {
  return isOnly ? (
    <Button onClick={onBack}>{label}</Button>
  ) : (
    <TextButton tone="neutral" onClick={onBack}>
      {label}
    </TextButton>
  );
}
