package com.remake.mealplan.adapter.out.mfds;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.mealplan.application.port.out.LoadMealPlanCombinationRulesPort;
import com.remake.mealplan.domain.MealPlanCombinationRules;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * resources/mfds/combination-check.json을 앱이 켜질 때 한 번 읽어 둔다(#288 F, scripts/mfds/build_combination_check.py가 만든 파일).
 * 파일이 없거나 모양이 틀리면 앱이 뜨지 않는다(스크립트를 다시 돌려야 하는 상태를 배포 전에 알 수 있게). 모르는 카테고리 코드가 있어도 같다.
 */
@Slf4j
@Component
class MealPlanCombinationRulesJsonAdapter implements LoadMealPlanCombinationRulesPort {

	static final String RESOURCE = "mfds/combination-check.json";

	private final MealPlanCombinationRules rules;

	MealPlanCombinationRulesJsonAdapter(JsonMapper jsonMapper) {
		this.rules = read(jsonMapper, RESOURCE);
		log.info("식단표 조합 검사 기준을 읽음: 주의 재료 {}개", rules.partners().size());
	}

	@Override
	public MealPlanCombinationRules load() {
		return rules;
	}

	static MealPlanCombinationRules read(JsonMapper jsonMapper, String resource) {
		RulesJson json;
		try (InputStream in = new ClassPathResource(resource).getInputStream()) {
			json = jsonMapper.readValue(in, RulesJson.class);
		} catch (IOException | JacksonException e) {
			throw new IllegalStateException("식단표 조합 검사 기준 파일을 읽을 수 없습니다: " + resource, e);
		}
		if (json.catalog() == null || json.partners() == null || json.partners().isEmpty()) {
			throw new IllegalStateException("식단표 조합 검사 기준 파일이 비어 있습니다: " + resource);
		}
		Map<String, IngredientCategoryCode> catalog = new LinkedHashMap<>();
		json.catalog().forEach((name, code) -> catalog.put(name, IngredientCategoryCode.valueOf(code)));
		Map<String, MealPlanCombinationRules.Alias> aliases = new LinkedHashMap<>();
		json.aliases().forEach((name, alias) -> aliases.put(name, new MealPlanCombinationRules.Alias(
				alias.catalogName(),
				alias.categoryCode() == null ? null : IngredientCategoryCode.valueOf(alias.categoryCode()))));
		Map<String, Set<String>> partners = new LinkedHashMap<>();
		json.partners().forEach((name, list) -> partners.put(name, Set.copyOf(list)));
		return new MealPlanCombinationRules(catalog, json.synonyms(), aliases, json.groups(),
				Set.copyOf(json.skippedNames()), Set.copyOf(json.dishNames()), json.titleOnlyTerms(),
				Set.copyOf(json.stapleGroups()), Set.copyOf(json.breakfast()), Set.copyOf(json.excludedFromAi()), partners);
	}

	record RulesJson(String source, int recipeCount, LinkedHashMap<String, String> catalog,
			Map<String, String> synonyms, Map<String, AliasJson> aliases, Map<String, String> groups,
			List<String> skippedNames, List<String> dishNames, List<String> titleOnlyTerms, List<String> stapleGroups,
			List<String> breakfast, List<String> excludedFromAi,
			Map<String, List<String>> partners) {
	}

	record AliasJson(String catalogName, String categoryCode) {
	}
}
