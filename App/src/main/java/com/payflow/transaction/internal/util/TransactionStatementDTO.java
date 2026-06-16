package com.payflow.transaction.internal.util;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionStatementDTO(
        Long transactionId,Long sourceWalletId,Long walletDestinationId,BigDecimal amount,String transactionStatus, OffsetDateTime transactionAt
        ) {
}
