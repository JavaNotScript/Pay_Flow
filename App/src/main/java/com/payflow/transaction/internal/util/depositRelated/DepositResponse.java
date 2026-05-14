package com.payflow.transaction.internal.util;

import java.math.BigDecimal;

public record DepositResponse(Long transactionId,Long walletId,String transactionStatus,BigDecimal amount,String currency) {
}
