package com.payflow.transaction.api;

import com.payflow.transaction.internal.repos.ExchangeRepository;
import com.payflow.transaction.internal.domain.CurrencyEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class ExchangeAdapter implements ExchangeFacade {
    private final ExchangeRepository exchangeRepository;

    @Override
    public BigDecimal convertToUSD(BigDecimal amount, CurrencyEnum from, CurrencyEnum to) {
        if (from.equals(to)) {
            return amount;
        }

        BigDecimal fromRate = exchangeRepository
                .findByCurrency(from)
                .orElseThrow()
                .getRateToUsd();

        BigDecimal toRate = exchangeRepository
                .findByCurrency(to)
                .orElseThrow()
                .getRateToUsd();

        BigDecimal amountInUSD = amount.multiply(fromRate);

        return amountInUSD.divide(toRate,2, RoundingMode.HALF_UP);
    }
}
