"use client";

import { useEffect, useId, useState } from "react";
import Image from "next/image";

import { AddIcon } from "@solar-icons/react/linear";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { TextField } from "@/components/TextField/TextField";

import { RecipeBody } from "../../../../_components/RecipeBody/RecipeBody";
import type { RecipeContent } from "../../../../_lib/recipe";

import styles from "./ShareRecipeContainer.module.css";

/** 제목 최대 길이: 다듬기 전 UTF-16 100자(Java @Size, HTML maxlength와 같음, domain-rules 7) */
const TITLE_MAX_LENGTH = 100;

type ShareRecipeContainerProps = {
  /** 공유할 내 레시피(API 30, 주소 ?recipeId=). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  recipe?: RecipeContent;
  /** 처음 사진 주소. 미리보기용(피그마 사진 수정·완료) */
  initialPhotoUrl?: string;
  /** 처음 제목. 미리보기용(피그마 완료) */
  initialTitle?: string;
  /** 처음부터 제목 칸에 포커스. 미리보기용(피그마 인풋 포커스) */
  initialTitleFocused?: boolean;
};

/**
 * p4.1.1.1 레시피 공유 작성. 사진 1장(필수, 누르면 고르기·바꾸기), 레시피 제목(필수, 100자), 레시피 내용(읽기 전용, 읽어 주기 없음),
 * "레시피 공유하기"(사진과 제목이 있어야 켜짐, 확인 모달 없음). 같은 레시피는 한 주에 한 번(domain-rules 7).
 * 상태: 기본 / 인풋 포커스 / 사진 수정(사진 고른 뒤) / 완료(사진·제목 다 있음).
 */
export function ShareRecipeContainer({
  recipe,
  initialPhotoUrl,
  initialTitle = "",
  initialTitleFocused = false,
}: ShareRecipeContainerProps) {
  const photoInputId = useId();
  const titleId = useId();
  const [photoUrl, setPhotoUrl] = useState(initialPhotoUrl);
  const [title, setTitle] = useState(initialTitle);
  const canShare = Boolean(recipe && photoUrl && title.trim());

  useEffect(() => {
    if (initialTitleFocused) document.getElementById(titleId)?.focus();
  }, [initialTitleFocused, titleId]);

  // 고른 사진의 임시 주소는 바꾸거나 화면을 떠날 때 지운다(미리보기로 넘긴 주소는 blob이 아니라 그대로 둔다)
  useEffect(
    () => () => {
      if (photoUrl?.startsWith("blob:")) URL.revokeObjectURL(photoUrl);
    },
    [photoUrl],
  );

  const handlePhotoChange = (file: File | undefined) => {
    if (!file) return;
    // TODO: 올리기 전에 줄이기·HEIC 변환(#112, #174). 지금은 고른 사진을 화면에만 보인다
    setPhotoUrl(URL.createObjectURL(file));
  };

  const handleShare = () => {
    // TODO: API 51(multipart image + payload { recipeId, title })을 보내고 공유 상세로. 같은 레시피를 이번 주에 이미
    // 공유했으면 오류 토스트
  };

  return (
    <>
      <SubHeader title="레시피 공유하기" />
      <main className={styles.main}>
        {/* 사진 칸 전체가 파일 고르기. 사진이 있으면 "탭해서 이미지 수정" */}
        <input
          id={photoInputId}
          type="file"
          accept="image/*"
          className={styles.srOnly}
          onChange={(event) => handlePhotoChange(event.target.files?.[0])}
        />
        <label htmlFor={photoInputId} className={styles.photo} data-empty={!photoUrl || undefined}>
          {photoUrl ? (
            <>
              <Image
                src={photoUrl}
                alt="고른 레시피 사진"
                fill
                sizes="(max-width: 480px) 100vw, 480px"
                unoptimized
                className={styles.image}
              />
              <span className={styles.photoEdit}>탭해서 이미지 수정</span>
            </>
          ) : (
            <span className={styles.photoAdd}>
              <Image
                src="/images/recipe/photo-frame.svg"
                alt=""
                width={240}
                height={48}
                className={styles.photoFrame}
              />
              <AddIcon className={styles.photoAddIcon} aria-hidden="true" />
              탭해서 이미지를 추가해 주세요
            </span>
          )}
        </label>

        <div className={styles.titleField}>
          <label htmlFor={titleId} className={styles.titleLabel}>
            레시피 제목
            <span className={styles.required} aria-hidden="true">
              *
            </span>
            <span className={styles.srOnly}>(필수)</span>
          </label>
          <TextField
            id={titleId}
            placeholder="레시피 제목을 작성해 주세요"
            maxLength={TITLE_MAX_LENGTH}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            required
            clearLabel="레시피 제목 지우기"
          />
        </div>

        <RecipeBody recipe={recipe} />
      </main>

      <BottomBar className={styles.bottom}>
        <Button disabled={!canShare} onClick={handleShare}>
          레시피 공유하기
        </Button>
      </BottomBar>
    </>
  );
}
