package com.remake.share.domain;

/** 저장소 키와 게시글 안에서의 사진 순서. URL은 저장하지 않는다. */
public record SharedRecipePhoto(String imageKey, int sortOrder) {
}
