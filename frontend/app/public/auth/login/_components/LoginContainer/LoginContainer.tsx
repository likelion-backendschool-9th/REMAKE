"use client";

import { useId, useState, useSyncExternalStore } from "react";
import Image from "next/image";

import { Drawer } from "@/components/Drawer/Drawer";
import { LegalDocument } from "@/components/LegalDocument/LegalDocument";
import { SocialLoginButton } from "@/components/SocialLoginButton/SocialLoginButton";

import {
  getRecentProvider,
  SOCIAL_PROVIDERS,
  type SocialProvider,
  subscribeRecentProvider,
} from "../../_lib/recent-provider";
import { TERMS, type TermsKind } from "../../_lib/terms";

import styles from "./LoginContainer.module.css";

type LoginContainerProps = {
  /** 최근 로그인 제공자. 기본(undefined)은 기기에 저장된 값을 읽는다. 미리보기에서 상태를 정할 때 넘긴다 */
  recentProvider?: SocialProvider | null;
  /** 처음부터 열어 둘 약관 드로워. 미리보기용, 기본 닫힘 */
  initialOpenTerms?: TermsKind | null;
};

/**
 * p1 로그인 화면. 화면 가운데에 로고, 소셜 로그인 버튼(최근 로그인 말풍선), 약관 링크·드로워.
 * 상태: 기본 / 최근 로그인(p1-로그인-최근 로그인) / 이용 약관 드로워 / 개인정보 처리 방침 드로워.
 */
export function LoginContainer({ recentProvider, initialOpenTerms = null }: LoginContainerProps) {
  const bubbleId = useId();
  // 서버 렌더링에서는 말풍선 없이 그리고, 브라우저에서 저장된 값을 읽는다
  const storedRecent = useSyncExternalStore(subscribeRecentProvider, getRecentProvider, () => null);
  const recent = recentProvider === undefined ? storedRecent : recentProvider;
  const [openTerms, setOpenTerms] = useState<TermsKind | null>(initialOpenTerms);

  return (
    <main className={styles.main}>
      <div className={styles.content}>
        <h1 className={styles.logo}>
          <Image src="/images/logo-mark.png" alt="" width={110} height={65} priority />
          REMAKE
        </h1>

        <div className={styles.buttons}>
          {SOCIAL_PROVIDERS.map((provider) => {
            const isRecent = recent === provider;
            return (
              <div key={provider} className={styles.item}>
                {isRecent && (
                  <span id={bubbleId} className={styles.bubble}>
                    최근 로그인
                  </span>
                )}
                {/* TODO: 소셜 로그인 연동(제공자 인증 → callback → API 1)과 E08 login_started는 다음 작업에서 붙인다 */}
                <SocialLoginButton
                  provider={provider}
                  aria-describedby={isRecent ? bubbleId : undefined}
                />
              </div>
            );
          })}
        </div>

        <div className={styles.links}>
          <button type="button" className={styles.link} onClick={() => setOpenTerms("terms")}>
            {TERMS.terms.title}
          </button>
          <button type="button" className={styles.link} onClick={() => setOpenTerms("privacy")}>
            {TERMS.privacy.title}
          </button>
        </div>
      </div>

      <Drawer
        isOpen={openTerms !== null}
        onClose={() => setOpenTerms(null)}
        title={openTerms ? TERMS[openTerms].title : ""}
      >
        {openTerms && <LegalDocument document={TERMS[openTerms]} />}
      </Drawer>
    </main>
  );
}
