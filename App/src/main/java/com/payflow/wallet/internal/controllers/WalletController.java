package com.payflow.wallet.internal.controllers;

import com.payflow.auth.internal.security.AuthenticatedUser;
import com.payflow.wallet.internal.dto.WalletDTO;
import com.payflow.wallet.internal.services.WalletService;
import com.payflow.wallet.internal.util.UpdateCurrencyRequest;
import com.payflow.wallet.internal.util.UpdateCurrencyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @GetMapping(value = "/me")
    public ResponseEntity<WalletDTO> getCurrentWallet(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        Long ownerId = user.getUserId();

        return ResponseEntity.ok(walletService.getCurrentWallet(ownerId));
    }

    @PutMapping("/update/currency")
    public ResponseEntity<UpdateCurrencyResponse> updateCurrency(Authentication authentication, @RequestBody UpdateCurrencyRequest request) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        Long ownerId = user.getUserId();

        return ResponseEntity.ok(walletService.updateCurrency(ownerId, request.currency()));
    }

}
