package com.remake.ingredient.application.port.in;

import java.math.BigDecimal;

/**
 * 사용·폐기 결과. 남은 양이 0이 돼서 재료 행을 지웠으면 deleted가 true이다.
 * costAmount는 버린 재료의 가격 비례 금액이고, 사용했거나 가격이 없으면 null이다.
 */
public record ConsumeIngredientResult(BigDecimal remainingAmount, boolean deleted, Long costAmount) {
}
