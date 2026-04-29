package com.payflow.wallet.api;

import com.payflow.wallet.internal.util.WalletInfo;

public interface WalletFacade {

    WalletInfo getWalletByUserId(Long userId);
}
