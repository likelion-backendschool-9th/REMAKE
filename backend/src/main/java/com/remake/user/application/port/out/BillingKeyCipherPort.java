package com.remake.user.application.port.out;

/** billingKey 암호문 보관. 평문은 로그·응답에 넣지 않는다. */
public interface BillingKeyCipherPort {

	String encrypt(String plainBillingKey);

	String decrypt(String encryptedBillingKey);
}
