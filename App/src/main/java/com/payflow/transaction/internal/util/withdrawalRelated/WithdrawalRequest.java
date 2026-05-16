package com.payflow.transaction.internal.util.withdrawalRelated;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record WithdrawalRequest(
        @NotBlank BigDecimal amount,
        @NotBlank String idempotencyKey,
        @NotBlank String phoneNumber,
        String description) {
}
