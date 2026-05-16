package com.payflow.outbox;

import java.math.BigDecimal;

public record DepositEvent(Long transactionId, Long walletId) {
}
