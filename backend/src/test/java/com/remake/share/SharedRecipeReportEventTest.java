package com.remake.share;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.remake.event.domain.EventName;
import com.remake.event.domain.EventPropertyFilter;
import com.remake.share.domain.SharedRecipeReportReason;

class SharedRecipeReportEventTest {
	@Test
	void 네_사유는_정의서값으로_통과하고_옛값이나_자유입력은_거절한다() {
		for (var reason : SharedRecipeReportReason.values()) {
			assertThat(EventPropertyFilter.filter(EventName.SHARED_RECIPE_REPORTED,
					Map.of("shared_recipe_id", 1L, "report_reason", reason.eventValue()))).isPresent();
		}
		for (String invalid : new String[]{"PHOTO", "OFFENSIVE", "DANGEROUS", "OTHER", "자유 입력"}) {
			assertThat(EventPropertyFilter.filter(EventName.SHARED_RECIPE_REPORTED,
					Map.of("shared_recipe_id", 1L, "report_reason", invalid))).isEmpty();
		}
		assertThat(EventName.SHARED_RECIPE_PUBLISHED.getAllowedProperties()).containsExactly("shared_recipe_id");
	}
}
