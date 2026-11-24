package com.remake.cart.adapter.in.web;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartSelectionRequest(@NotEmpty List<@NotNull @Positive Long> ids) { }
