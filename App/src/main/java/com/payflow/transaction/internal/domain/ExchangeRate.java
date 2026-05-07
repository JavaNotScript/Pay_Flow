package com.payflow.transaction.internal.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    @Column(name = "exchange_id")
    private Long exchangeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private CurrencyEnum currency;

    @Column(name = "rate_to_usd")
    private BigDecimal rateToUsd;

    @Column(name = "buy_rate")
    private BigDecimal buyRate;

    @Column(name = "sell_rate")
    private BigDecimal sellRate;

    @CreationTimestamp
    @Column(name = "exchange_time")
    private OffsetDateTime exchangeTime;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
