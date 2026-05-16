package com.payflow.transaction.internal.util.depositRelated;

import java.math.BigDecimal;

public record DepositRequest(BigDecimal amount,String idempotencyKey,String depositCurrency) {
}
