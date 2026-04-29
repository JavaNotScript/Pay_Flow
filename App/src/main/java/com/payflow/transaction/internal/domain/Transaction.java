package com.payflow.transaction.internal.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id",updatable = false,unique = true)
    private Long transactionId;

    @Column(name= "idempotency_key",unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_direction")//debit,credit
    private TransactionDirection transactionDirection;

    @Column(name ="source_wallet_id")
    private Long sourceWalletId;

    @Column(name = "wallet_destination_id")
    private Long walletDestinationId;

    @Column(name = "source_amount",nullable = false)
    private BigDecimal sourceAmount;

    @Column(name = "source_currency")
    private String sourceCurrency;

    @Column(name = "destination_amount") //currency set after conversion
    private BigDecimal destinationAmount;

    @Column(name = "destination_currency")
    private String destinationCurrency;

    @Column(name = "exchange_rate")
    private BigDecimal exchangeRate;

    @Column(name = "fee_amount")
    private BigDecimal feeAmount;

    @Column(name = "fee_currency")
    private String feeCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method") //DEPOSIT,WITHDRAWAL,TRANSFER
    private PaymentMethod paymentMethod;

    @Column(name = "description")
    private String description;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "external_Reference",unique = true)//transaction receipt reference on clients end
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status",nullable = false)//PENDING,PROCESSING,SUCCESS,FAILED,REVERSED
    private TransactionStatus transactionStatus;

    @CreationTimestamp
    @Column(name = "transaction_at", updatable = false)
    private OffsetDateTime transactionAt;
}
