package com.remake.ingredient.domain;

/** 보관 위치. DB와 JSON에는 이름 그대로(FRIDGE) 들어간다. 이미지 저장소(storage)와 헷갈리지 않게 StorageLocation으로 부른다. */
public enum StorageLocation {
	FRIDGE,
	FREEZER,
	PANTRY
}
