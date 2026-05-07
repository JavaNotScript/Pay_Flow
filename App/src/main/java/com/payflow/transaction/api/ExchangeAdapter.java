package com.payflow.transaction.api;

import com.payflow.common.ex.TransactionException;
import com.payflow.transaction.internal.domain.CurrencyEnum;
import com.payflow.transaction.internal.domain.ExchangeRate;
import com.payflow.transaction.internal.repos.ExchangeRepository;
import com.payflow.transaction.internal.util.ConversionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExchangeAdapter implements ExchangeFacade {
    private final ExchangeRepository exchangeRepository;

    @Override
    public ConversionResult convert(BigDecimal amount, CurrencyEnum from, CurrencyEnum to) {
        if (from.equals(to)) {
            return new ConversionResult(amount, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, OffsetDateTime.now());
        }


        Map<CurrencyEnum, BigDecimal> rates = exchangeRepository
                .findByCurrencyIn(List.of(from, to))
                .stream()
                .collect(Collectors.toMap(ExchangeRate::getCurrency, ExchangeRate::getRateToUsd));

        BigDecimal fromRate = from == CurrencyEnum.USD
                ? BigDecimal.ONE
                : exchangeRepository.findByCurrency(from)
                .orElseThrow(() -> new TransactionException("No exchange rate found from :" + from))
                .getRateToUsd();

        BigDecimal toRate = to == CurrencyEnum.USD
                ? BigDecimal.ONE
                : exchangeRepository.findByCurrency(to)
                .orElseThrow(() -> new TransactionException("No exchange rate found to "+to)).
                getRateToUsd();


        BigDecimal amountInUsd = amount.multiply(fromRate)
                .setScale(10, RoundingMode.HALF_UP);

        BigDecimal destinationAmount =
                amountInUsd.divide(
                        toRate,
                        6,
                        RoundingMode.HALF_UP
                );

        BigDecimal rateUsed =
                destinationAmount.divide(
                        amount,
                        2,
                        RoundingMode.HALF_UP
                );

        return new ConversionResult(
                destinationAmount,
                rateUsed,
                fromRate,
                toRate,
                OffsetDateTime.now()
        );
    }
}
