package com.payflow.transaction.internal.util.sendRelated;

import java.math.BigDecimal;

public record SendMoneyRequest(BigDecimal amount,String receiverWalletTag,String idempotencyKey,String description) {
}
