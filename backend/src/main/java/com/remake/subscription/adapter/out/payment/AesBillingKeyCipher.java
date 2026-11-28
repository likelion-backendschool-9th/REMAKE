package com.remake.subscription.adapter.out.payment;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.remake.global.config.TossPaymentsProperties;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.subscription.application.port.out.BillingKeyCipherPort;

import lombok.RequiredArgsConstructor;

/**
 * AES-GCM으로 billingKey를 암호화한다. 키는 remake.toss.billing-key-secret에서 유도한다.
 */
@Component
@RequiredArgsConstructor
public class AesBillingKeyCipher implements BillingKeyCipherPort {

	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_BITS = 128;

	private final TossPaymentsProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public String encrypt(String plainBillingKey) {
		if (!StringUtils.hasText(plainBillingKey)) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
		try {
			byte[] iv = new byte[GCM_IV_LENGTH];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] cipherText = cipher.doFinal(plainBillingKey.getBytes(StandardCharsets.UTF_8));
			ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
			buffer.put(iv);
			buffer.put(cipherText);
			return Base64.getEncoder().encodeToString(buffer.array());
		} catch (GeneralSecurityException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public String decrypt(String encryptedBillingKey) {
		if (!StringUtils.hasText(encryptedBillingKey)) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
		try {
			byte[] decoded = Base64.getDecoder().decode(encryptedBillingKey);
			ByteBuffer buffer = ByteBuffer.wrap(decoded);
			byte[] iv = new byte[GCM_IV_LENGTH];
			buffer.get(iv);
			byte[] cipherText = new byte[buffer.remaining()];
			buffer.get(cipherText);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
			return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
	}

	private SecretKeySpec secretKey() {
		String secret = properties.billingKeySecret();
		if (!StringUtils.hasText(secret)) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
			return new SecretKeySpec(hash, "AES");
		} catch (GeneralSecurityException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
	}
}
