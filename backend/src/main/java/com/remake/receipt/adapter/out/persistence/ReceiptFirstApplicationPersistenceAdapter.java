package com.remake.receipt.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.remake.receipt.application.port.out.ReceiptFirstApplicationPort;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class ReceiptFirstApplicationPersistenceAdapter implements ReceiptFirstApplicationPort {

	private final EntityManager entityManager;

	@Override
	public boolean exists(Long userId, String storeName, LocalDate purchasedOn, String receiptNumber) {
		Long count = entityManager.createQuery("""
				SELECT COUNT(a) FROM ReceiptFirstApplicationJpaEntity a
				WHERE a.userId = :userId AND a.storeName = :storeName
				  AND a.purchasedOn = :purchasedOn AND a.receiptNumber = :receiptNumber
				""", Long.class)
				.setParameter("userId", userId)
				.setParameter("storeName", storeName)
				.setParameter("purchasedOn", purchasedOn)
				.setParameter("receiptNumber", receiptNumber)
				.getSingleResult();
		return count > 0;
	}

	@Override
	public void markIfAbsent(Long userId, String storeName, LocalDate purchasedOn, String receiptNumber,
			LocalTime purchasedTime) {
		if (exists(userId, storeName, purchasedOn, receiptNumber)) {
			return;
		}
		LocalTime time = purchasedTime == null ? null : purchasedTime.truncatedTo(ChronoUnit.MICROS);
		entityManager.persist(new ReceiptFirstApplicationJpaEntity(userId, storeName, purchasedOn, receiptNumber, time));
	}

	@Override
	@Transactional
	public void deletePurchasedBefore(LocalDate cutoff) {
		entityManager.createQuery("""
				DELETE FROM ReceiptFirstApplicationJpaEntity a WHERE a.purchasedOn < :cutoff
				""")
				.setParameter("cutoff", cutoff)
				.executeUpdate();
	}

	@Override
	@Transactional
	public void deleteAllForUser(Long userId) {
		entityManager.createQuery("""
				DELETE FROM ReceiptFirstApplicationJpaEntity a WHERE a.userId = :userId
				""")
				.setParameter("userId", userId)
				.executeUpdate();
	}
}
