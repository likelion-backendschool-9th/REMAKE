package com.remake.share.domain;

import java.util.Locale;

public enum SharedRecipeReportReason {
	OFFENSIVE_IMAGE,
	AD_TITLE,
	DANGEROUS_RECIPE,
	OTHER;

	public String eventValue() { return name().toLowerCase(Locale.ROOT); }
}
