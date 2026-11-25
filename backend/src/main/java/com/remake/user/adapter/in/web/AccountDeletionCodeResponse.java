package com.remake.user.adapter.in.web;

import java.time.LocalDateTime;

/** API 9 응답. */
public record AccountDeletionCodeResponse(LocalDateTime expiresAt) {
}
