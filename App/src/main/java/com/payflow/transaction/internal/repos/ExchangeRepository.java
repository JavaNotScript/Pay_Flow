package com.payflow.transaction.internal.repos;

import com.payflow.transaction.internal.domain.ExchangeRate;
import com.payflow.transaction.internal.domain.CurrencyEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface ExchangeRepository extends JpaRepository<ExchangeRate,Long> {
    Optional<ExchangeRate> findByCurrency(CurrencyEnum from);
}
