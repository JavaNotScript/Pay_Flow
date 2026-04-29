package com.payflow.wallet.internal.dto;

import com.payflow.wallet.internal.domain.CurrencyEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WalletDTO {
    private Long walletId;
    private BigDecimal balance;
    private CurrencyEnum currency;
    private Long ownerId;
}
