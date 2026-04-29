package com.payflow.transaction.internal.util;

import java.math.BigDecimal;

public record DepositRequest(BigDecimal amount,String idempotencyKey) {
}
