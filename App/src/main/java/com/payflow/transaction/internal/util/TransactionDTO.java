package com.payflow.transaction.internal.util;

import java.math.BigDecimal;

public record TransactionDTO(Long walletId, String currency, BigDecimal amount) {
}
