package com.payflow.transaction.internal.util;

import java.math.BigDecimal;

public record ExchangeRequest(String currency, BigDecimal buyRate,BigDecimal sellRate,BigDecimal rateToUsd) {
}
