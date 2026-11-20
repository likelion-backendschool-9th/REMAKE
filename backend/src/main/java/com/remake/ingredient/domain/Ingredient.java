package com.remake.ingredient.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 냉장고 재료의 도메인 규칙. Spring·JPA 어노테이션을 붙이지 않는다.
 * 규칙을 어기면 IllegalArgumentException을 던지고, Service가 BusinessException(INVALID_INPUT)으로 바꾼다.
 * 현재 시각은 읽지 않는다. 필요한 날짜는 인수로 받는다.
 */
public class Ingredient {

	public static final int NAME_MAX_LENGTH = 50;
	/** numeric(10,3): 정수 7자리, 소수 3자리 */
	public static final int AMOUNT_MAX_INTEGER_DIGITS = 7;
	public static final int AMOUNT_MAX_SCALE = 3;

	private final Long id;
	private final Long userId;
	private final Long receiptItemId;
	private final BigDecimal initialAmount;
	private final IngredientUnit unit;

	private Long categoryId;
	private String name;
	private StorageLocation storage;
	private LocalDate expirationDate;
	private boolean expirationEstimated;
	private BigDecimal amount;
	private Long price;
	private IngredientStatus status;

	private Ingredient(Long id, Long userId, Long categoryId, Long receiptItemId, String name, StorageLocation storage,
			LocalDate expirationDate, boolean expirationEstimated, BigDecimal amount, BigDecimal initialAmount,
			IngredientUnit unit, Long price, IngredientStatus status) {
		this.id = id;
		this.userId = userId;
		this.categoryId = categoryId;
		this.receiptItemId = receiptItemId;
		this.name = name;
		this.storage = storage;
		this.expirationDate = expirationDate;
		this.expirationEstimated = expirationEstimated;
		this.amount = amount;
		this.initialAmount = initialAmount;
		this.unit = unit;
		this.price = price;
		this.status = status;
	}

	/**
	 * 새 재료를 등록한다. 남은 양과 처음 양은 같고 상태는 ACTIVE이다.
	 * 소비기한을 직접 입력했으면 expirationEstimated는 false, 카테고리 기본값으로 추정했으면 true를 넘긴다.
	 */
	public static Ingredient register(Long userId, Long categoryId, Long receiptItemId, String name,
			StorageLocation storage, LocalDate expirationDate, boolean expirationEstimated, BigDecimal amount,
			IngredientUnit unit, Long price) {
		requireNonNull(userId, "사용자");
		requireNonNull(categoryId, "카테고리");
		requireNonNull(storage, "보관 위치");
		requireNonNull(expirationDate, "소비기한");
		requireNonNull(unit, "단위");
		String normalizedName = normalizeName(name);
		validateAmount(amount);
		validatePrice(price);
		return new Ingredient(null, userId, categoryId, receiptItemId, normalizedName, storage, expirationDate,
				expirationEstimated, amount, amount, unit, price, IngredientStatus.ACTIVE);
	}

	/** 저장된 값으로 되살린다. 저장소 어댑터만 쓴다. 이미 저장된 값이라 규칙을 다시 확인하지 않는다. */
	public static Ingredient restore(Long id, Long userId, Long categoryId, Long receiptItemId, String name,
			StorageLocation storage, LocalDate expirationDate, boolean expirationEstimated, BigDecimal amount,
			BigDecimal initialAmount, IngredientUnit unit, Long price, IngredientStatus status) {
		return new Ingredient(id, userId, categoryId, receiptItemId, name, storage, expirationDate,
				expirationEstimated, amount, initialAmount, unit, price, status);
	}

	public void changeName(String newName) {
		this.name = normalizeName(newName);
	}

	public void changeCategory(Long newCategoryId) {
		requireNonNull(newCategoryId, "카테고리");
		this.categoryId = newCategoryId;
	}

	/** 소비기한을 사용자가 직접 고친다. 사용자가 정한 값이므로 추정 표시를 끈다. */
	public void changeExpirationDate(LocalDate newExpirationDate) {
		requireNonNull(newExpirationDate, "소비기한");
		this.expirationDate = newExpirationDate;
		this.expirationEstimated = false;
	}

	public void changeStorage(StorageLocation newStorage) {
		requireNonNull(newStorage, "보관 위치");
		this.storage = newStorage;
	}

	/** 가격을 고친다. null이면 가격을 지운다. */
	public void changePrice(Long newPrice) {
		validatePrice(newPrice);
		this.price = newPrice;
	}

	private static String normalizeName(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("재료 이름은 비워 둘 수 없습니다.");
		}
		String trimmed = name.strip();
		if (trimmed.length() > NAME_MAX_LENGTH) {
			throw new IllegalArgumentException("재료 이름은 " + NAME_MAX_LENGTH + "자 이내여야 합니다.");
		}
		return trimmed;
	}

	/** 0보다 크고, 소수 셋째 자리까지, 정수 7자리까지(DB의 numeric(10,3)에 들어가는 범위) */
	private static void validateAmount(BigDecimal amount) {
		if (amount == null || amount.signum() <= 0) {
			throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
		}
		BigDecimal stripped = amount.stripTrailingZeros();
		if (stripped.scale() > AMOUNT_MAX_SCALE) {
			throw new IllegalArgumentException("수량은 소수 셋째 자리까지 입력할 수 있습니다.");
		}
		if (stripped.precision() - stripped.scale() > AMOUNT_MAX_INTEGER_DIGITS) {
			throw new IllegalArgumentException("수량이 너무 큽니다.");
		}
	}

	private static void validatePrice(Long price) {
		if (price != null && price < 0) {
			throw new IllegalArgumentException("가격은 0원 이상이어야 합니다.");
		}
	}

	private static void requireNonNull(Object value, String label) {
		if (value == null) {
			throw new IllegalArgumentException(label + "은(는) 필수입니다.");
		}
	}

	public Long id() {
		return id;
	}

	public Long userId() {
		return userId;
	}

	public Long categoryId() {
		return categoryId;
	}

	public Long receiptItemId() {
		return receiptItemId;
	}

	public String name() {
		return name;
	}

	public StorageLocation storage() {
		return storage;
	}

	public LocalDate expirationDate() {
		return expirationDate;
	}

	public boolean expirationEstimated() {
		return expirationEstimated;
	}

	public BigDecimal amount() {
		return amount;
	}

	public BigDecimal initialAmount() {
		return initialAmount;
	}

	public IngredientUnit unit() {
		return unit;
	}

	public Long price() {
		return price;
	}

	public IngredientStatus status() {
		return status;
	}
}
