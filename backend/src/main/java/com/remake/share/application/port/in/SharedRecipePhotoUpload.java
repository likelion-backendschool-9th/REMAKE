package com.remake.share.application.port.in;

import org.springframework.core.io.InputStreamSource;

/** 웹 어댑터는 MultipartFile의 MIME·크기·스트림을 전달한다. 원본 파일명은 필요하지 않다. */
public record SharedRecipePhotoUpload(String contentType, long size, InputStreamSource content) {
}
