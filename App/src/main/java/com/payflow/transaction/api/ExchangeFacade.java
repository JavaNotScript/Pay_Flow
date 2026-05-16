package com.payflow.transaction.api;

import com.payflow.transaction.internal.domain.CurrencyEnum;
import com.payflow.transaction.internal.util.exchange.ConversionResult;

import java.math.BigDecimal;

public interface ExchangeFacade {
    ConversionResult convert(BigDecimal amount, CurrencyEnum from, CurrencyEnum to);
}
