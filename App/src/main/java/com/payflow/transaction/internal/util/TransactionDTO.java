package com.payflow.transaction.internal.util;

import java.math.BigDecimal;

public record TransactionDTO(Long walletDestinationId,Long walletSourceId, String currency, BigDecimal destinationAmount,BigDecimal sourceAmount) {
}
