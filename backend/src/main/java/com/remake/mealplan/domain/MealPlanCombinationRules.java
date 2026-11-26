package com.remake.mealplan.domain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.remake.ingredient.domain.IngredientCategoryCode;

/**
 * 식단표 AI 레시피의 재료 조합 검사 기준(#288 F). 식약처 조리식품 레시피 1,156개로 만든 표를 앱이 켜질 때 한 번 읽어 바꾸지 않는다
 * (scripts/mfds/build_combination_check.py가 만든 resources/mfds/combination-check.json).
 *
 * <p><b>F2 조합 검사</b>: 한 레시피의 재료 짝 가운데 식약처 레시피에서 한 번도 같이 쓰인 적 없는 짝에 주의 재료(과일·음료·단것·요거트·견과류)가
 * 끼면 엉터리 조합("토마토초코밥")으로 본다. 두 재료가 모두 아침 재료(과일·견과류·빵·시리얼·요거트·우유·버터·잼·꿀 등)면 허용한다
 * (바나나토스트, 우유 시리얼).
 * 재료 이름은 정리 스크립트와 같은 규칙으로 검사 키에 맞춘다: 4글자 이하 "○○밥"(볶음밥 아님)은 쌀 → 고침표 → 사전 이름과 같음 →
 * 사전 이름(두 글자 이상)이 들어 있으면 가장 긴 것(같은 길이면 이름에서 더 뒤에 있는 것: 당근주스 → 주스) → 같은 재료 다른 표기는 기준 이름 → 무리(김치류·면류 등)는
 * 무리 이름. 사전과 못 맞춘 이름, 양념(SEASONING), 물·생수·육수는 검사하지 않는다.
 *
 * <p><b>이름·재료 검사</b>: 요리 이름에 든 재료(사전 이름, 두부조림·샐러드 같은 요리 이름인 사전 항목은 뺌)가 재료 목록에 없는지 본다.
 * 이름은 낱말마다 앞에서부터 가장 긴 사전 이름으로 끊어 읽는다(감자두부 → 감자·두부). 같은 재료 다른 표기·무리는 같은 재료로 본다.
 * 지금은 막지 않고 걸린 칸 수만 센다(11-26 결정, 실측 AI 레시피의 3.1%가 걸려 막으면 재시도가 크게 늘어남).
 *
 * <p><b>F1 입력 거르기</b>: 식약처 요리(후식 제외)에 한 번도 나오지 않는 재료(콜라·초콜릿·과자 등 18개)와, 음료 카테고리인데 사전과 맞지 않는
 * 냉장고 재료는 AI에 보내지 않는다. 냉장고 재료 이름은 #145 규칙(사전 이름과 같음 → 같은 카테고리의 사전 이름이 들어 있으면 가장 긴 것)으로 맞춘다.
 * 길이가 같을 때는 #145(id가 작은 것)와 달리 위와 같이 이름에서 더 뒤에 있는 것을 고른다(같은 카테고리 안의 동률은 드물다).
 *
 * @param catalog        재료 사전 이름 → 카테고리(V5 마이그레이션 사본)
 * @param synonyms       같은 재료 다른 표기 → 기준 이름
 * @param aliases        재료 이름(공백 뺀 것) → 고침표 값. 사전 이름이 null이면 사전과 연결하지 않는다
 * @param groups         무리에 든 재료 → 무리 이름
 * @param skippedNames   검사하지 않는 이름(물·생수·육수)
 * @param dishNames      요리 이름인 사전 항목(두부조림·샐러드 등). 이름·재료 검사에서 재료로 보지 않는다
 * @param breakfast      아침 재료(검사 키)
 * @param excludedFromAi AI에 보내지 않을 냉장고 재료(사전 이름)
 * @param partners       주의 재료 → 식약처 레시피에서 함께 쓰인 검사 키. 키 목록이 곧 주의 재료다
 */
public record MealPlanCombinationRules(Map<String, IngredientCategoryCode> catalog, Map<String, String> synonyms,
		Map<String, Alias> aliases, Map<String, String> groups, Set<String> skippedNames, Set<String> dishNames,
		Set<String> breakfast, Set<String> excludedFromAi, Map<String, Set<String>> partners) {

	/** 이름 안에서 찾을 사전 이름의 최소 글자 수(IngredientUnitPolicy와 같다) */
	static final int MIN_CONTAINED_NAME_LENGTH = 2;
	/** 이 글자 수 이하의 "○○밥"은 쌀로 본다(정리 스크립트 RICE_MAX_LENGTH와 같다) */
	static final int RICE_MAX_LENGTH = 4;
	static final String RICE = "쌀";
	private static final Set<String> RICE_EXCEPT = Set.of("볶음밥");
	private static final Pattern PARENTHESES = Pattern.compile("\\(.*?\\)");
	/** 요리 이름을 낱말로 나누는 문자(정리 스크립트 TITLE_SPLIT와 같다) */
	private static final Pattern TITLE_SPLIT = Pattern.compile("[\\s,()·&/+]+");

	/**
	 * 고침표 한 줄.
	 *
	 * @param catalogName  맞출 사전 이름. null이면 사전과 연결하지 않는다(검사하지 않음)
	 * @param categoryCode 카테고리. null이면 사전 카테고리
	 */
	public record Alias(String catalogName, IngredientCategoryCode categoryCode) {
	}

	public MealPlanCombinationRules {
		catalog = Map.copyOf(catalog);
		synonyms = Map.copyOf(synonyms);
		aliases = Map.copyOf(aliases);
		groups = Map.copyOf(groups);
		skippedNames = Set.copyOf(skippedNames);
		dishNames = Set.copyOf(dishNames);
		breakfast = Set.copyOf(breakfast);
		excludedFromAi = Set.copyOf(excludedFromAi);
		partners = partners.entrySet().stream()
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
						entry -> Set.copyOf(entry.getValue())));
	}

	/** 검사하지 않는 빈 기준(테스트 등). */
	public static MealPlanCombinationRules empty() {
		return new MealPlanCombinationRules(Map.of(), Map.of(), Map.of(), Map.of(), Set.of(), Set.of(), Set.of(),
				Set.of(), Map.of());
	}

	/**
	 * 레시피 재료 이름들에서 처음 보는 주의 재료 짝의 수. 0이면 통과다.
	 * 같은 검사 키는 하나로 센다(대파·쪽파는 "파" 하나).
	 */
	public int unseenCautionPairs(Collection<String> ingredientNames) {
		List<String> keys = ingredientNames.stream()
				.map(this::keyOf)
				.flatMap(Optional::stream)
				.collect(Collectors.toCollection(TreeSet::new))
				.stream()
				.toList();
		int count = 0;
		for (int i = 0; i < keys.size(); i++) {
			for (int j = i + 1; j < keys.size(); j++) {
				if (isUnseenCautionPair(keys.get(i), keys.get(j))) {
					count++;
				}
			}
		}
		return count;
	}

	private boolean isUnseenCautionPair(String a, String b) {
		Set<String> partnersOfA = partners.get(a);
		Set<String> partnersOfB = partners.get(b);
		if (partnersOfA == null && partnersOfB == null) {
			return false;
		}
		if ((partnersOfA != null && partnersOfA.contains(b)) || (partnersOfB != null && partnersOfB.contains(a))) {
			return false;
		}
		return !(breakfast.contains(a) && breakfast.contains(b));
	}

	/**
	 * 재료 이름의 검사 키(무리 이름 또는 사전 이름). 사전과 못 맞추거나 양념·물이면 비어 있다.
	 * 정리 스크립트 Rules.key와 같아야 하며, 스크립트가 만든 기대값과 비교하는 테스트가 있다.
	 */
	public Optional<String> keyOf(String name) {
		if (name == null || name.isBlank()) {
			return Optional.empty();
		}
		String stripped = name.strip();
		String bare = PARENTHESES.matcher(stripped).replaceAll("").replace(" ", "");
		if (bare.endsWith("밥") && bare.length() <= RICE_MAX_LENGTH && !RICE_EXCEPT.contains(bare)) {
			stripped = RICE;
		}
		Optional<Alias> matched = match(stripped);
		if (matched.isEmpty()) {
			return Optional.empty();
		}
		String catalogName = canonical(matched.get().catalogName());
		IngredientCategoryCode category = matched.get().categoryCode() != null
				? matched.get().categoryCode()
				: catalog.get(catalogName);
		String key = groups.getOrDefault(catalogName, catalogName);
		if (category == IngredientCategoryCode.SEASONING || skippedNames.contains(key)) {
			return Optional.empty();
		}
		return Optional.of(key);
	}

	/**
	 * 요리 이름에 들었지만 재료 목록에 없는 재료(이름에 나온 순서, 정리 스크립트 Rules.title_missing과 같다).
	 * 예: 재료가 계란·김치·물·소금인 "양파계란국" → [양파]. 비어 있으면 이름과 재료가 맞다.
	 */
	public List<String> missingTitleIngredients(String title, Collection<String> ingredientNames) {
		if (title == null || title.isBlank()) {
			return List.of();
		}
		return titleTerms(title).stream()
				.filter(term -> !titleTermUsed(term, ingredientNames))
				.toList();
	}

	/** 낱말마다 앞에서부터 가장 긴 사전 이름(요리 이름인 항목 뺌)으로 끊는다. 한 글자 사전 이름은 낱말 전체가 그 이름일 때만. */
	private List<String> titleTerms(String title) {
		LinkedHashSet<String> terms = new LinkedHashSet<>();
		for (String word : TITLE_SPLIT.split(title)) {
			int i = 0;
			while (i < word.length()) {
				String best = null;
				for (String catalogName : catalog.keySet()) {
					if (dishNames.contains(catalogName) || !word.startsWith(catalogName, i)) {
						continue;
					}
					boolean longEnough = catalogName.length() >= MIN_CONTAINED_NAME_LENGTH || word.equals(catalogName);
					if (longEnough && (best == null || catalogName.length() > best.length())) {
						best = catalogName;
					}
				}
				if (best != null) {
					terms.add(best);
					i += best.length();
				} else {
					i++;
				}
			}
		}
		return List.copyOf(terms);
	}

	/** 이름의 재료가 재료 목록에 있는지: 기준 이름이나 무리가 같거나, 재료 이름(공백 뺌)에 그 이름이나 무리 이름이 들어 있으면 있다. */
	private boolean titleTermUsed(String term, Collection<String> ingredientNames) {
		String termName = canonical(term);
		String termKey = groups.getOrDefault(termName, termName);
		for (String name : ingredientNames) {
			if (name == null || name.isBlank()) {
				continue;
			}
			Optional<String> nameCanonical = match(name.strip()).map(alias -> canonical(alias.catalogName()));
			String nameKey = nameCanonical.map(n -> groups.getOrDefault(n, n)).orElse(null);
			String compact = name.replace(" ", "");
			if (nameCanonical.filter(termName::equals).isPresent() || termKey.equals(nameKey)
					|| compact.contains(termName) || compact.contains(term)
					|| (!termKey.equals(termName) && compact.contains(termKey))) {
				return true;
			}
		}
		return false;
	}

	/** 고침표 → 사전 이름과 같음 → 사전 이름이 들어 있으면 가장 긴 것. 고침표에서 연결하지 않기로 한 이름은 비어 있다. */
	private Optional<Alias> match(String name) {
		String compact = name.replace(" ", "");
		String base = PARENTHESES.matcher(compact).replaceAll("");
		for (String candidate : List.of(compact, base)) {
			Alias alias = aliases.get(candidate);
			if (alias != null) {
				return alias.catalogName() == null ? Optional.empty() : Optional.of(alias);
			}
		}
		for (String candidate : List.of(compact, base)) {
			if (catalog.containsKey(candidate)) {
				return Optional.of(new Alias(candidate, null));
			}
		}
		return longestContained(base, null).map(found -> new Alias(found, null));
	}

	/**
	 * F1: 이 냉장고 재료를 AI에 보내지 않는지. 사전 이름이 excludedFromAi에 있거나, 음료 카테고리인데 사전과 맞지 않으면 보내지 않는다.
	 * 카테고리를 모르면 사전 이름과 정확히 같을 때만 맞춘다(#145와 같다).
	 */
	public boolean excludedFromAi(String name, IngredientCategoryCode category) {
		if (name == null || name.isBlank()) {
			return false;
		}
		Optional<String> catalogName = fridgeCatalogName(name.strip(), category);
		if (catalogName.isEmpty()) {
			return category == IngredientCategoryCode.BEVERAGE;
		}
		return excludedFromAi.contains(catalogName.get());
	}

	private Optional<String> fridgeCatalogName(String name, IngredientCategoryCode category) {
		if (catalog.containsKey(name)) {
			return Optional.of(canonical(name));
		}
		if (category == null) {
			return Optional.empty();
		}
		return longestContained(name, category).map(this::canonical);
	}

	/** 이름에 들어 있는 사전 이름(두 글자 이상) 가운데 가장 긴 것. 같은 길이면 이름에서 더 뒤에 있는 것. category가 있으면 그 카테고리만 본다. */
	private Optional<String> longestContained(String name, IngredientCategoryCode category) {
		String best = null;
		int bestIndex = -1;
		for (Map.Entry<String, IngredientCategoryCode> entry : catalog.entrySet()) {
			String catalogName = entry.getKey();
			if (catalogName.length() < MIN_CONTAINED_NAME_LENGTH
					|| (category != null && entry.getValue() != category)) {
				continue;
			}
			int index = name.lastIndexOf(catalogName);
			if (index < 0) {
				continue;
			}
			if (best == null || catalogName.length() > best.length()
					|| (catalogName.length() == best.length() && index > bestIndex)) {
				best = catalogName;
				bestIndex = index;
			}
		}
		return Optional.ofNullable(best);
	}

	private String canonical(String name) {
		return synonyms.getOrDefault(name, name);
	}
}
