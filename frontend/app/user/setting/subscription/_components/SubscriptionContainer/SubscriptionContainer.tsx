"use client";

import { useId, useState } from "react";
import Image from "next/image";

import { Badge } from "@/components/Badge/Badge";
import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { Checkbox } from "@/components/Checkbox/Checkbox";
import { ConfirmModal } from "@/components/ConfirmModal/ConfirmModal";
import { Drawer } from "@/components/Drawer/Drawer";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { TextButton } from "@/components/TextButton/TextButton";
import { formatFullDate, formatWon } from "@/lib/format";

import { BENEFITS, PAYMENT_TERMS, type Subscription } from "../../_lib/subscription";

import styles from "./SubscriptionContainer.module.css";

type SubscriptionContainerProps = {
  /** 화면 값(API 57). 없으면 불러오는 중으로 그린다. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  subscription?: Subscription;
  /** 결제 약관 동의 체크. 미리보기용, 기본 해제 */
  defaultAgreed?: boolean;
  /** 결제 약관 드로워를 처음부터 연다. 미리보기용 */
  initialOpenTerms?: boolean;
  /** 구독 해지 확인 모달을 처음부터 연다. 미리보기용 */
  initialOpenCancel?: boolean;
};

/**
 * p1.3.4 구독 관리. 구독 전이면 결제 약관 동의 후 "구독 결제", 구독 중이면 "구독 중" 배지와 "구독 해지하기".
 * 상태: 구독 전(p1.3.4-구독하기) / 약관 동의 / 결제 약관 드로워 / 구독 중(p1.3.4-구독 해지) / 해지 확인 모달(p1.3.4-구독혜지 모달).
 */
export function SubscriptionContainer({
  subscription,
  defaultAgreed = false,
  initialOpenTerms = false,
  initialOpenCancel = false,
}: SubscriptionContainerProps) {
  const titleId = useId();
  const benefitsTitleId = useId();
  const agreeId = useId();
  const [isAgreed, setIsAgreed] = useState(defaultAgreed);
  const [isTermsOpen, setIsTermsOpen] = useState(initialOpenTerms);
  const [isCancelOpen, setIsCancelOpen] = useState(initialOpenCancel);
  // 해지 예약(CANCELED) 뒤 화면은 미결이라 구독 중과 같게 그린다
  const isSubscribed = subscription !== undefined && subscription.status !== "NONE";

  const handleSubscribe = () => {
    // TODO: E83 subscription_checkout_started → API 58로 받은 값으로 토스 SDK requestBillingAuth(카드 등록 창)
  };

  const handleCancel = () => {
    setIsCancelOpen(false);
    // TODO: API 60 해지 예약(E87 subscription_canceled는 서버가 기록). 해지 예약 뒤 화면은 미결(정책)
  };

  return (
    <>
      <SubHeader title="구독 관리" />
      <main className={styles.main}>
        <section
          className={styles.summary}
          aria-labelledby={titleId}
          aria-busy={!subscription || undefined}
        >
          <h2 id={titleId} className={styles.titleRow}>
            <span className={styles.icon} aria-hidden="true">
              <Image src="/images/premium-sparkle.svg" alt="" width={18} height={18} />
            </span>
            <span className={styles.brand}>REMAKE</span>
            <span className={styles.plan}>프리미엄 멤버십</span>
            {isSubscribed && <Badge tone="primary">구독 중</Badge>}
          </h2>
          <dl className={styles.info}>
            <div className={styles.infoRow}>
              <dt className={styles.infoLabel}>다음 결제일</dt>
              <dd className={styles.infoValue}>
                {subscription ? (
                  formatFullDate(subscription.nextBillingDate)
                ) : (
                  <Skeleton width="6rem" height="1.25rem" />
                )}
              </dd>
            </div>
            <div className={styles.infoRow}>
              <dt className={styles.infoLabel}>결제 금액</dt>
              <dd className={styles.infoValue}>
                {subscription ? (
                  formatWon(subscription.price)
                ) : (
                  <Skeleton width="4rem" height="1.25rem" />
                )}
              </dd>
            </div>
          </dl>
        </section>

        <section className={styles.benefits} aria-labelledby={benefitsTitleId}>
          <h2 id={benefitsTitleId} className={styles.benefitsTitle}>
            리메이크 프리미엄의 특별한 혜택
          </h2>
          <ul className={styles.benefitList}>
            {BENEFITS.map((benefit) => (
              <li key={benefit.label} className={styles.benefit}>
                <div className={styles.benefitHead}>
                  <Image src={benefit.image} alt="" width={74} height={74} />
                  <p className={styles.benefitName}>
                    {benefit.label}
                    <br />
                    {benefit.name} <span className={styles.highlight}>{benefit.highlight}</span>
                  </p>
                </div>
                <p className={styles.benefitDescription}>{benefit.description}</p>
              </li>
            ))}
          </ul>
        </section>
      </main>

      <BottomBar className={styles.bottom}>
        {isSubscribed ? (
          <Button tone="danger" onClick={() => setIsCancelOpen(true)}>
            구독 해지하기
          </Button>
        ) : (
          <>
            <div className={styles.agree}>
              <Checkbox
                id={agreeId}
                checked={isAgreed}
                onChange={(event) => setIsAgreed(event.target.checked)}
              />
              <label htmlFor={agreeId} className={styles.agreeLabel}>
                결제 약관 동의
              </label>
              <TextButton className={styles.terms} onClick={() => setIsTermsOpen(true)}>
                자세히 보기
              </TextButton>
            </div>
            {/* 결제 약관에 동의해야 결제할 수 있다(정책). 값을 불러오기 전에도 막는다 */}
            <Button disabled={!subscription || !isAgreed} onClick={handleSubscribe}>
              구독 결제
            </Button>
          </>
        )}
      </BottomBar>

      {/* 피그마 문구의 "혜지"는 "해지"로 고쳐 쓴다 */}
      <ConfirmModal
        isOpen={isCancelOpen}
        onClose={() => setIsCancelOpen(false)}
        onConfirm={handleCancel}
        title="구독 해지"
        message="구독을 해지하면 다음 결제일에 자동 결제가 이루어지지 않습니다. 다시 구독을 원하실 경우 재결제 과정이 필요합니다. 해지하시겠습니까?"
        confirmLabel="구독 해지"
        tone="danger"
      />

      <Drawer
        isOpen={isTermsOpen}
        onClose={() => setIsTermsOpen(false)}
        title={PAYMENT_TERMS.title}
      >
        {PAYMENT_TERMS.body}
      </Drawer>
    </>
  );
}
