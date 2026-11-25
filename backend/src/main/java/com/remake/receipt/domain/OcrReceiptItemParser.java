package com.remake.receipt.domain;

import static com.remake.receipt.domain.OcrReceiptAmounts.MONEY;
import static com.remake.receipt.domain.OcrReceiptAmounts.MONEY_WITH_LEADING_ONE_VARIANT;
import static com.remake.receipt.domain.OcrReceiptAmounts.QUANTITY;
import static com.remake.receipt.domain.OcrReceiptAmounts.QUANTITY_WITH_OCR_VARIANTS;
import static com.remake.receipt.domain.OcrReceiptAmounts.moneyDigits;
import static com.remake.receipt.domain.OcrReceiptAmounts.verifiedQuantity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.remake.ingredient.domain.IngredientUnit;

public class OcrReceiptItemParser {

	private static final String AMOUNT_SUFFIX = "(?:\\h*원)?\\h*[*#]*";
	private static final Pattern HEADER = Pattern.compile("(?:상품명|품목명|품명|[상삼]품\\h*\\(코드\\)).*수\\h*량.*금\\h*액");
	private static final Pattern UNIT_PRICE_HEADER = Pattern.compile("단\\h*가");
	private static final Pattern FOOTER = Pattern.compile("^(?:합\\h*계|총\\h*액|총\\h*합계|총\\h*구\\h*매\\h*액|할인\\h*금액|결제\\h*금액|부가세|[(][*#][)]).*");
	private static final Pattern SEPARATOR = Pattern.compile("[-=_─━]+");
	private static final Pattern NUMBERED_NAME = Pattern.compile(
			"^(?<number>[0-9]{1,3})\\.?(?:\\h+|(?=\\p{L}))(?<name>.+)$");
	private static final Pattern UNNUMBERED_PROMOTIONAL_NAME = Pattern.compile(
			"^(?<name>P\\h*\\p{IsHangul}.*?)\\h+\\[" + MONEY + "\\]$");
	private static final Pattern PACK_LABEL = Pattern.compile("^(.+?)\\h+[1|\\]ㅣ]\\h*(봉지|봉|팩|캔|병|통)$");
	private static final Pattern PRODUCT_VOLUME = Pattern.compile(
			"(?<![\\d.,])(" + QUANTITY + ")\\h*(kg|ml|g|l|킬로그램|밀리리터|그램|리터|㎏|㎖|ℓ)$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern MULTIPACK_PREFIX = Pattern.compile("[0-9]\\h*[xX×*]\\h*$");

	private static final Pattern NUMERIC_ROW = Pattern.compile(
			"^[*#]?(?:[0-9]{6,14}|[0-9]{3}-[0-9]{9,11})\\h+(" + MONEY_WITH_LEADING_ONE_VARIANT + ")\\h+(?:(" + QUANTITY_WITH_OCR_VARIANTS + ")\\h+)?("
					+ MONEY_WITH_LEADING_ONE_VARIANT + ")[sS]?" + AMOUNT_SUFFIX + "(?:\\h{3,}ER)?$");
	private static final Pattern DAMAGED_NUMERIC_ROW = Pattern.compile(
			"^(?:[0-9]{4,8}[\":]\\h*[0-9]{4,9}\\h+|\\p{L}{1,4}\\h{3,})"
					+ "(" + MONEY_WITH_LEADING_ONE_VARIANT + ")\\h+(" + QUANTITY_WITH_OCR_VARIANTS + ")\\h+("
					+ MONEY_WITH_LEADING_ONE_VARIANT + ")" + AMOUNT_SUFFIX + "$");
	private static final Pattern SINGLE_ROW = Pattern.compile(
			"^(.+?)\\h+(" + QUANTITY + ")([\\p{IsHangul}a-zA-Z]*)\\h+(" + MONEY + ")" + AMOUNT_SUFFIX + "$");
	private static final Pattern PRICED_SINGLE_ROW = Pattern.compile(
			"^([*#]?\\h*\\p{L}.*?)\\h+(" + MONEY + ")\\h+(" + QUANTITY_WITH_OCR_VARIANTS + ")\\h+(" + MONEY + ")"
					+ AMOUNT_SUFFIX + "$");
	private static final Pattern PRICED_ROW_WITH_MISSING_QUANTITY = Pattern.compile(
			"^([*#]?\\h*\\p{L}.*?)\\h+(" + MONEY + ")\\h+(" + MONEY + ")" + AMOUNT_SUFFIX + "$");
	public List<Item> parse(String text) {
		List<String> lines = text.lines().map(String::strip).toList();
		if (lines.stream().noneMatch(line -> HEADER.matcher(line).find())) {
			List<Item> singleRows = parseWithoutHeader(lines);
			List<Item> pairedRows = List.copyOf(parsePairedRows(lines).values());
			if (pairedRows.size() < 2) {
				return singleRows;
			}
			return pairedRows.size() > singleRows.size() ? pairedRows : singleRows;
		}

		var items = new ArrayList<Item>();
		boolean inItems = false;
		boolean hasUnitPrice = false;
		String pendingName = null;
		for (String line : lines) {
			if (!inItems) {
				inItems = HEADER.matcher(line).find();
				hasUnitPrice = inItems && UNIT_PRICE_HEADER.matcher(line).find();
				continue;
			}
			if (line.isEmpty() || SEPARATOR.matcher(line).matches()) {
				continue;
			}
			if (FOOTER.matcher(line).matches()) {
				break;
			}

			// 상품 코드로 시작하는 숫자 줄은 앞 품목과만 연결한다.
			Matcher numbers = pairedNumbers(line, pendingName != null);
			if (numbers != null) {
				if (pendingName != null) {
					String amount = verifiedQuantity(numbers.group(1), numbers.group(2), numbers.group(3));
					if (amount != null || (numbers.group(2) == null && hasProductVolume(pendingName))) {
						items.add(toItem(pendingName, amount, "", numbers.group(3)));
					}
				}
				pendingName = null;
				continue;
			}

			Matcher numberedName = NUMBERED_NAME.matcher(line);
			String nameAndValues = numberedName.matches() ? numberedName.group("name") : line;
			Matcher pricedRow = PRICED_SINGLE_ROW.matcher(nameAndValues);
			if (pricedRow.matches()) {
				addPricedItem(items, pricedRow);
				pendingName = null;
				continue;
			}
			Matcher missingQuantity = PRICED_ROW_WITH_MISSING_QUANTITY.matcher(nameAndValues);
			if (hasUnitPrice && missingQuantity.matches()) {
				String name = missingQuantity.group(1).replaceFirst("^[*#]\\h*", "");
				String amount = verifiedQuantity(missingQuantity.group(2), null, missingQuantity.group(3));
				if (amount != null || hasProductVolume(name)) {
					items.add(toItem(name, amount, "", missingQuantity.group(3)));
				}
				pendingName = null;
				continue;
			}
			Matcher singleRow = SINGLE_ROW.matcher(nameAndValues);
			if (singleRow.matches() && singleRow.group(1).codePoints().anyMatch(Character::isLetter)) {
				items.add(toItem(singleRow.group(1), singleRow.group(2), singleRow.group(3), singleRow.group(4)));
				pendingName = null;
			}
			else {
				pendingName = pendingItemName(numberedName, nameAndValues);
			}
		}
		return List.copyOf(items);
	}

	private static String pendingItemName(Matcher numberedName, String nameAndValues) {
		if (numberedName.matches() && nameAndValues.codePoints().anyMatch(Character::isLetter)) {
			return nameAndValues;
		}
		// 순번이 빠져도 행사 표기와 대괄호 가격이 있으면 다음 숫자 줄의 검증을 시도한다.
		Matcher promotionalName = UNNUMBERED_PROMOTIONAL_NAME.matcher(nameAndValues);
		return promotionalName.matches() ? promotionalName.group("name") : null;
	}

	private static List<Item> parseWithoutHeader(List<String> lines) {
		var items = new ArrayList<Item>();
		boolean inItems = false;
		for (String line : lines) {
			if (line.isEmpty()) {
				continue;
			}
			if (FOOTER.matcher(line).matches()) {
				break;
			}
			Matcher pricedRow = PRICED_SINGLE_ROW.matcher(line);
			if (pricedRow.matches() && addPricedItem(items, pricedRow)) {
				// 표 제목이 없으면 금액이 검증된 품목 두 줄이 이어져야 표로 본다.
				inItems = inItems || items.size() >= 2;
			}
			else if (!inItems) {
				items.clear();
			}
		}
		return inItems ? List.copyOf(items) : List.of();
	}

	private static Map<Integer, Item> parsePairedRows(List<String> lines) {
		var items = new LinkedHashMap<Integer, Item>();
		String pendingName = null;
		int pendingNumber = 0;
		for (String line : lines) {
			if (line.isEmpty()) {
				continue;
			}
			if (FOOTER.matcher(line).matches()) {
				break;
			}
			Matcher numbers = pairedNumbers(line, pendingName != null);
			if (pendingName != null && numbers != null) {
				String amount = verifiedQuantity(numbers.group(1), numbers.group(2), numbers.group(3));
				if (amount != null) {
					items.putIfAbsent(pendingNumber, toItem(pendingName, amount, "", numbers.group(3)));
				}
				pendingName = null;
				continue;
			}
			// 제목 없이 읽을 때도 상품 번호가 붙은 이름과 바로 아래 숫자 줄만 묶는다.
			Matcher numberedName = NUMBERED_NAME.matcher(line);
			if (numberedName.matches() && numberedName.group("name").codePoints().anyMatch(Character::isLetter)) {
				pendingName = numberedName.group("name");
				pendingNumber = Integer.parseInt(numberedName.group("number"));
			}
			else {
				pendingName = null;
			}
		}
		return items;
	}

	private static Matcher pairedNumbers(String line, boolean hasPendingItem) {
		Matcher strict = NUMERIC_ROW.matcher(line);
		if (strict.matches()) {
			return strict;
		}
		if (!hasPendingItem) {
			return null;
		}
		// 숫자 세 열을 모두 읽었을 때만 깨진 상품 코드 칸을 허용한다.
		Matcher damagedCode = DAMAGED_NUMERIC_ROW.matcher(line);
		return damagedCode.matches() ? damagedCode : null;
	}

	private static boolean addPricedItem(List<Item> items, Matcher row) {
		String amount = verifiedQuantity(row.group(2), row.group(3), row.group(4));
		if (amount == null) {
			return false;
		}
		items.add(toItem(row.group(1).replaceFirst("^[*#]\\h*", ""), amount, "", row.group(4)));
		return true;
	}

	private static Item toItem(String name, String amount, String unit, String total) {
		// 상품명 끝의 1팩 표기는 포장 단위이며 실제 수량은 수량 열에서 읽는다.
		Matcher packaging = PACK_LABEL.matcher(name);
		if (unit.isEmpty() && packaging.matches()) {
			name = packaging.group(1);
			unit = packaging.group(2);
		}
		BigDecimal parsedAmount = amount == null ? null : new BigDecimal(amount);
		OcrQuantity quantity = parsedAmount == null || parsedAmount.signum() <= 0
				? null : OcrQuantity.normalize(parsedAmount, unit);
		Volume volume = quantity != null && isMeasurement(quantity.unit())
				? new Volume(quantity.amount(), quantity.unit(), VolumeScope.TOTAL)
				: productVolume(name);
		return new Item(name, quantity, Long.parseLong(moneyDigits(total)), volume);
	}

	private static boolean hasProductVolume(String name) {
		Matcher packaging = PACK_LABEL.matcher(name);
		return productVolume(packaging.matches() ? packaging.group(1) : name) != null;
	}

	private static Volume productVolume(String name) {
		Matcher matcher = PRODUCT_VOLUME.matcher(name);
		if (!matcher.find() || MULTIPACK_PREFIX.matcher(name.substring(0, matcher.start())).find()) {
			return null;
		}
		BigDecimal amount = new BigDecimal(matcher.group(1));
		if (amount.signum() <= 0) {
			return null;
		}
		String unit = switch (matcher.group(2)) {
			case "㎏" -> "kg";
			case "㎖" -> "ml";
			case "ℓ" -> "l";
			default -> matcher.group(2);
		};
		return new Volume(amount, OcrQuantity.normalize(amount, unit).unit(), VolumeScope.PER_PACK);
	}

	private static boolean isMeasurement(IngredientUnit unit) {
		return switch (unit) {
			case G, KG, ML, L -> true;
			default -> false;
		};
	}

	public record Volume(BigDecimal amount, IngredientUnit unit, VolumeScope scope) {
	}

	public enum VolumeScope {
		PER_PACK, TOTAL
	}

	public record Item(String name, OcrQuantity quantity, long totalPrice, Volume volume) {

		public String nameWithoutPackageVolume() {
			if (volume == null || volume.scope() != VolumeScope.PER_PACK) {
				return name;
			}
			Matcher matcher = PRODUCT_VOLUME.matcher(name);
			return matcher.find() ? name.substring(0, matcher.start()).stripTrailing() : name;
		}

		public BigDecimal purchaseQuantity() {
			return quantity == null || isMeasurement(quantity.unit()) ? null : quantity.amount();
		}
	}
}
