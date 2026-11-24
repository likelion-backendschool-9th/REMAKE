package com.remake.user.adapter.in.web;

import java.util.List;

/**
 * 제외 재료 통째 교체. 0개여도 된다.
 */
public record ReplaceExcludedIngredientsRequest(List<String> allergenCodes) {
}
