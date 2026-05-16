package com.payflow.transaction.internal.services;

import com.payflow.common.ex.TransactionException;
import com.payflow.transaction.internal.domain.CurrencyEnum;
import com.payflow.transaction.internal.domain.ExchangeRate;
import com.payflow.transaction.internal.repos.ExchangeRepository;
import com.payflow.transaction.internal.util.exchange.ExchangeResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@AllArgsConstructor
public class ExchangeService {
    private final ExchangeRepository exchangeRepository;

    public ExchangeResponse addRate(BigDecimal buyRate, BigDecimal sellRate, String currency, BigDecimal rateToUsd) {
        CurrencyEnum currencyEnum;
        try {
            currencyEnum = CurrencyEnum.valueOf(currency.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TransactionException("No currency type found of: " + currency);
        }

        if (exchangeRepository.existsByCurrency(currencyEnum)) {
            throw new TransactionException("Exchange for currency already exists try updating" + currencyEnum);
        }

        ExchangeRate rate = new ExchangeRate();
        rate.setBuyRate(buyRate);
        rate.setSellRate(sellRate);
        rate.setCurrency(currencyEnum);
        rate.setRateToUsd(rateToUsd);

        ExchangeRate savedRate = exchangeRepository.save(rate);
        return mapToResponse(savedRate);
    }

    public ExchangeResponse updateRate(BigDecimal buyRate, BigDecimal sellRate, String currency, BigDecimal rateToUsd) {
        CurrencyEnum currencyEnum;
        try {
            currencyEnum = CurrencyEnum.valueOf(currency.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TransactionException("No currency type found of: " + currency);
        }

        ExchangeRate exchangeRate = exchangeRepository.findByCurrency(currencyEnum)
                .orElseThrow(() -> new TransactionException("Currency not found" + currencyEnum));

        exchangeRate.setSellRate(sellRate);
        exchangeRate.setBuyRate(buyRate);
        exchangeRate.setRateToUsd(rateToUsd);

        ExchangeRate savedRated = exchangeRepository.save(exchangeRate);

        return mapToResponse(savedRated);
    }

    private ExchangeResponse mapToResponse(ExchangeRate savedRate) {
        return new ExchangeResponse(
                savedRate.getExchangeId(),
                savedRate.getCurrency(),
                savedRate.getRateToUsd()
        );
    }
}
