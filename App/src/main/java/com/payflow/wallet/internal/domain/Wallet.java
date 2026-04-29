package com.payflow.wallet.internal.domain;

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
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "wallet")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;

    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "owner_Id",nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency",nullable = false)
    private CurrencyEnum currency;

    @CreationTimestamp
    @Column(name = "created_at",nullable = false,updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusEnum statusEnum;

    @Version
    @Column(name = "version")
    private Long version;
}
