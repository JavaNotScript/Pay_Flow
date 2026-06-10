package com.payflow.mpesa.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.payflow.mpesa.dto.AccessTokenResponse;
import com.payflow.mpesa.service.MpesaAuthService;
import com.payflow.transaction.api.TransactionAdapter;
import com.payflow.wallet.api.WalletAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mpesa")
@RequiredArgsConstructor
public class MpesaCallbackController {
    private final TransactionAdapter transactionAdapter;
    private final WalletAdapter walletAdapter;
    private final MpesaAuthService authService;

    @GetMapping("/token")
    public ResponseEntity<AccessTokenResponse> getAccessToken(){
        return ResponseEntity.ok(authService.generateAccessToken());
    }

    @PostMapping("/b2c/result")
    public ResponseEntity<Void> handleB2CResult(@RequestBody JsonNode payload){
        JsonNode result = payload.get("Result");

        String originatorConversationId = result.get("OriginatorConversationID").asText();

        int resultCode = result.get("ResultCode").asInt();

        if (resultCode == 0){
            transactionAdapter.updateTransactionStatus(Long.valueOf(originatorConversationId),"SUCCESS");
        }else {
            transactionAdapter.updateTransactionStatus(Long.valueOf(originatorConversationId),"FAILED");
            walletAdapter.reverseDebit(Long.valueOf(originatorConversationId));
        }
        return ResponseEntity.ok().build();
    }
}
