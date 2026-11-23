"use client";

import { useId, useState, useSyncExternalStore } from "react";

import { Drawer } from "@/components/Drawer/Drawer";
import { SocialLoginButton } from "@/components/SocialLoginButton/SocialLoginButton";

import {
  getRecentProvider,
  SOCIAL_PROVIDERS,
  subscribeRecentProvider,
} from "../../_lib/recent-provider";
import { TERMS, type TermsKind } from "../../_lib/terms";

import styles from "./LoginActions.module.css";

/**
 * p1 로그인의 소셜 로그인 버튼(최근 로그인 말풍선)과 약관 링크·드로워.
 * 상태: 기본 / 최근 로그인(p1-로그인-최근 로그인) / 이용 약관 드로워 / 개인정보 처리 방침 드로워.
 */
export function LoginActions() {
  const bubbleId = useId();
  // 서버 렌더링에서는 말풍선 없이 그리고, 브라우저에서 저장된 값을 읽는다
  const recent = useSyncExternalStore(subscribeRecentProvider, getRecentProvider, () => null);
  const [openTerms, setOpenTerms] = useState<TermsKind | null>(null);

  return (
    <>
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

      <Drawer
        isOpen={openTerms !== null}
        onClose={() => setOpenTerms(null)}
        title={openTerms ? TERMS[openTerms].title : ""}
      >
        {openTerms && TERMS[openTerms].body}
      </Drawer>
    </>
  );
}
