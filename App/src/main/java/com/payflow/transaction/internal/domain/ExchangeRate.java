package com.payflow.transaction.internal.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "exchange_rate")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private CurrencyEnum currency;

    @Column(name = "rate_to_usd")
    private BigDecimal rateToUsd;

    @CreationTimestamp
    @Column(name = "exchange_time")
    private OffsetDateTime exchangeTime;
}
