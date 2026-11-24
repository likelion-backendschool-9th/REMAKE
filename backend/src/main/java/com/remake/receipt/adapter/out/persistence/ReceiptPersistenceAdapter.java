package com.remake.receipt.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Repository;

import com.remake.receipt.application.port.in.RegisterReceiptUseCase.Command;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.Item;
import com.remake.receipt.application.port.out.FindDuplicateReceiptPort;
import com.remake.receipt.application.port.out.SaveReceiptPort;
import com.remake.receipt.domain.ReceiptStoreCode;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class ReceiptPersistenceAdapter implements SaveReceiptPort, FindDuplicateReceiptPort {

	private final EntityManager entityManager;

	@Override
	public boolean lockOwnerAndCheckDuplicate(Long userId, Command command) {
		// 서로 다른 멱등 키로 동시에 등록해도 같은 사용자의 중복 검사와 저장을 직렬화한다.
		entityManager.createNativeQuery("SELECT id FROM users WHERE id = :userId FOR NO KEY UPDATE")
				.setParameter("userId", userId)
				.getSingleResult();

		return exists(userId, new Key(command.storeName(), command.purchasedOn(), command.purchasedTime(),
				command.receiptNumber(), command.items().stream().map(item -> item.name().strip()).toList()));
	}

	@Override
	public boolean exists(Long userId, Key key) {
		if (!key.hasRequiredFields()) {
			return false;
		}
		var candidates = entityManager.createQuery("""
				SELECT r FROM ReceiptJpaEntity r
				WHERE r.userId = :userId AND r.storeName = :storeName AND r.purchasedOn = :purchasedOn
				""", ReceiptJpaEntity.class)
				.setParameter("userId", userId)
				.setParameter("storeName", key.storeName())
				.setParameter("purchasedOn", key.purchasedOn())
				.getResultList();

		var itemNames = key.itemNames().stream().sorted().toList();
		LocalTime purchasedTime = toDatabaseTime(key.purchasedTime());
		for (var candidate : candidates) {
			if (candidate.getReceiptNumber() == null || key.receiptNumber() == null) {
				var storedNames = entityManager.createQuery("""
						SELECT i.name FROM ReceiptItemJpaEntity i WHERE i.receiptId = :receiptId
						""", String.class)
						.setParameter("receiptId", candidate.getId())
						.getResultList().stream().sorted().toList();
				if (storedNames.equals(itemNames)) {
					return true;
				}
			}
			else if (candidate.getReceiptNumber().equals(key.receiptNumber())
					&& (candidate.getPurchasedTime() == null || purchasedTime == null
							|| candidate.getPurchasedTime().equals(purchasedTime))) {
				return true;
			}
		}
		return false;
	}

	private LocalTime toDatabaseTime(LocalTime time) {
		return time == null ? null : time.truncatedTo(ChronoUnit.MICROS);
	}

	@Override
	public Long saveReceipt(Long userId, LocalDate purchasedOn, LocalTime purchasedTime, String receiptNumber,
			String storeName, ReceiptStoreCode storeCode,
			String imageKey) {
		var receipt = new ReceiptJpaEntity(userId, purchasedOn, toDatabaseTime(purchasedTime), receiptNumber,
				storeName, storeCode, imageKey);
		entityManager.persist(receipt);
		return receipt.getId();
	}

	@Override
	public Long saveItem(Long receiptId, Long categoryId, Item item) {
		var receiptItem = new ReceiptItemJpaEntity(receiptId, categoryId, item);
		entityManager.persist(receiptItem);
		return receiptItem.getId();
	}
}
