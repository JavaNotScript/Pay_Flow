package com.payflow.wallet.internal.util;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrencyRequest(@NotBlank @Size(max = 3) String currency) {
}
