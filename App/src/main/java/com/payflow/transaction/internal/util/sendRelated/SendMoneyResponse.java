package com.payflow.transaction.internal.util.sendRelated;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SendMoneyResponse(Long receiversWalletId, BigDecimal amount, OffsetDateTime transactionAt,String reference) {
}
