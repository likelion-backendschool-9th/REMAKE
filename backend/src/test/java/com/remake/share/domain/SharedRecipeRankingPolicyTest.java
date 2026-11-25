package com.remake.share.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class SharedRecipeRankingPolicyTest {
	@Test
	void UTC_일요일이어도_KST_월요일부터_새주이고_월연경계의_라벨은_주내내_같다() {
		Clock sundayUtc = Clock.fixed(Instant.parse("2026-12-27T15:00:00Z"), ZoneOffset.UTC);
		Clock newYear = Clock.fixed(Instant.parse("2027-01-01T15:00:00Z"), ZoneOffset.UTC);
		var monday = SharedRecipe.weekStart(sundayUtc);
		assertThat(monday.toString()).isEqualTo("2026-12-28T00:00");
		assertThat(SharedRecipe.weekStart(newYear)).isEqualTo(monday);
		assertThat(SharedRecipeRankingPolicy.weekLabel(monday.toLocalDate())).isEqualTo("12월 넷째 주");
	}

	@Test
	void 순위변동은_상승이양수_하락이음수이고_동일순위와_비교불가는_대시다() {
		assertThat(SharedRecipeRankingPolicy.rankChange(1, 3)).isEqualTo(2);
		assertThat(SharedRecipeRankingPolicy.rankChange(3, 1)).isEqualTo(-2);
		assertThat(SharedRecipeRankingPolicy.rankChange(2, 2)).isNull();
		assertThat(SharedRecipeRankingPolicy.rankChange(1, null)).isNull();
		assertThat(SharedRecipeRankingPolicy.rankChange(null, 1)).isNull();
	}
}
