package com.payflow.transaction.internal.util.exchange;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ConversionResult(BigDecimal destinationAmount,
                               BigDecimal rateUsed,
                               BigDecimal fromRateToUsd,
                               BigDecimal toRateFromUsd,
                               OffsetDateTime timestamp) {
}
