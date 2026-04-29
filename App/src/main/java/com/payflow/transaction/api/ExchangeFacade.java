package com.payflow.transaction.api;

import com.payflow.transaction.internal.domain.CurrencyEnum;

import java.math.BigDecimal;

public interface ExchangeFacade {
    BigDecimal convertToUSD(BigDecimal amount, CurrencyEnum from, CurrencyEnum to);
}
