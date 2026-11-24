package com.remake.share.application.port.out;

/** 이미 존재하던 키는 이번 요청의 정리 대상에 포함하지 않도록 구분한다. */
public class PhotoStorageKeyConflictException extends RuntimeException {
}
