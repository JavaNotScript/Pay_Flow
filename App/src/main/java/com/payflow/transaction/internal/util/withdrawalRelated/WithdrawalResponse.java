package com.payflow.transaction.internal.util.withdrawalRelated;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WithdrawalResponse(String reference, BigDecimal amount, OffsetDateTime time,String description) {
}
