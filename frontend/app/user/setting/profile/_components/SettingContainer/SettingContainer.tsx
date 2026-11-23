"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { AltArrowRightIcon } from "@solar-icons/react/linear";

import { ConfirmModal } from "@/components/ConfirmModal/ConfirmModal";
import { SettingRow } from "@/components/SettingRow/SettingRow";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { Switch } from "@/components/Switch/Switch";

import { type Profile, PROVIDER_LABEL, SAMPLE_PROFILE } from "../../_lib/profile";

import styles from "./SettingContainer.module.css";

type SettingModal = "logout" | "delete";

type SettingContainerProps = {
  /** 화면 값. 기본은 마크업 확인용 값(API 5를 붙이기 전) */
  profile?: Profile;
  /** 처음부터 열어 둘 모달. 미리보기용, 기본 닫힘 */
  initialModal?: SettingModal | null;
};

/**
 * p1.3 설정. 계정 카드, 프리미엄 이용권, 설정 행(가구원 수·알레르기 관리·토글 2개·로그아웃·계정 삭제).
 * 상태: 기본(p1.3-설정) / 로그아웃 모달(p1.3-설정-로그아웃) / 계정 삭제 모달(p1.3-설정-계정 삭제).
 */
export function SettingContainer({
  profile = SAMPLE_PROFILE,
  initialModal = null,
}: SettingContainerProps) {
  const router = useRouter();
  const [modal, setModal] = useState<SettingModal | null>(initialModal);
  // TODO: 토글은 API 6(PATCH /user/profile)으로 저장하고 E14 setting_changed를 보낸다
  const [recipeSupplement, setRecipeSupplement] = useState(profile.recipeSupplementEnabled);
  const [notification, setNotification] = useState(profile.notificationEnabled);

  const closeModal = () => setModal(null);

  const handleLogout = () => {
    // TODO: API 3 로그아웃(E16 logout_confirmed) 뒤 로그인 화면으로. 이동하기 전에 먼저 닫는다
    closeModal();
  };

  const handleDelete = () => {
    // 다른 화면으로 가기 전에 먼저 닫는다(blueprint §7.1)
    closeModal();
    // TODO: 카카오·구글은 API 9로 인증번호를 보낸 뒤 이동. 네이버는 이 화면 대신 재인증(auth_type=reauthenticate)
    router.push("/user/setting/account-deletion");
  };

  return (
    <>
      <SubHeader title="설정" />
      <main className={styles.main}>
        <section className={styles.account} aria-labelledby="setting-account-title">
          <h2 id="setting-account-title" className={styles.accountTitle}>
            계정
          </h2>
          <div className={styles.accountBody}>
            <p className={styles.accountLabel}>{PROVIDER_LABEL[profile.provider]}</p>
            <dl className={styles.accountList}>
              <div className={styles.accountRow}>
                <dt className={styles.accountLabel}>가구원</dt>
                <dd className={styles.accountValue}>{profile.householdSize}명</dd>
              </div>
              <div className={styles.accountRow}>
                <dt className={styles.accountLabel}>알레르기</dt>
                <dd className={styles.accountValue}>{profile.excludedIngredientCount}개</dd>
              </div>
            </dl>
          </div>
        </section>

        <Link href="/user/setting/subscription" className={styles.premium}>
          <span className={styles.premiumIcon} aria-hidden="true">
            <Image src="/images/premium-sparkle.svg" alt="" width={18} height={18} />
          </span>
          <span className={styles.premiumLabel}>프리미엄 이용권</span>
          <span className={styles.premiumAction}>구독하기</span>
          <AltArrowRightIcon className={styles.premiumArrow} aria-hidden="true" />
        </Link>

        <div className={styles.rows}>
          <SettingRow
            label="가구원 수"
            value={`${profile.householdSize}명`}
            href="/user/setting/household"
          />
          <SettingRow
            label="알레르기 관리"
            value={`${profile.excludedIngredientCount}개`}
            href="/user/setting/excluded-ingredients"
          />
          <SettingRow
            label="레시피 재료 보충 여부"
            control={(labelId) => (
              <Switch
                aria-labelledby={labelId}
                checked={recipeSupplement}
                onCheckedChange={setRecipeSupplement}
              />
            )}
          />
          <SettingRow
            label="알림 받기"
            control={(labelId) => (
              <Switch
                aria-labelledby={labelId}
                checked={notification}
                onCheckedChange={setNotification}
              />
            )}
          />
          <SettingRow label="로그아웃" showValue={false} onClick={() => setModal("logout")} />
          <SettingRow label="계정 삭제" showValue={false} onClick={() => setModal("delete")} />
        </div>
      </main>

      <ConfirmModal
        isOpen={modal === "logout"}
        onClose={closeModal}
        onConfirm={handleLogout}
        title="로그아웃"
        message="로그아웃을 하시면 서비스를 이어서 사용하실 수 없습니다. 로그아웃하시겠습니까?"
        confirmLabel="로그아웃"
        tone="danger"
      />
      <ConfirmModal
        isOpen={modal === "delete"}
        onClose={closeModal}
        onConfirm={handleDelete}
        title="계정 삭제"
        message="계정이 삭제되면 기존에 있던 데이터가 삭제될 수 있습니다. 계정을 삭제하시겠습니까?"
        confirmLabel="계정 삭제"
        tone="danger"
      />
    </>
  );
}
