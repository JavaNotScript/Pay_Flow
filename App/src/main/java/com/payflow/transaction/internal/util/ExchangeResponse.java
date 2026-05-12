package com.payflow.transaction.internal.util;

import com.payflow.transaction.internal.domain.CurrencyEnum;

import java.math.BigDecimal;

public record ExchangeResponse( Long exchangeId,CurrencyEnum currency, BigDecimal rateToUsd) {
}
