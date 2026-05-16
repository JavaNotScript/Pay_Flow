package com.payflow.transaction.internal.util.withdrawalRelated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WithdrawalRequest(
        @NotBlank BigDecimal amount,
        @NotBlank String idempotencyKey,
        @NotBlank @Size(min = 10, max = 12) int phoneNumber,
        String description) {
}
