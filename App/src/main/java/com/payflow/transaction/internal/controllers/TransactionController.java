package com.payflow.transaction.internal.controllers;

import com.payflow.auth.api.AuthFacade;
import com.payflow.transaction.internal.util.sendRelated.SendMoneyResponse;
import com.payflow.transaction.internal.services.TransactionService;
import com.payflow.transaction.internal.util.depositRelated.DepositRequest;
import com.payflow.transaction.internal.util.depositRelated.DepositResponse;
import com.payflow.transaction.internal.util.sendRelated.SendMoneyRequest;
import com.payflow.transaction.internal.util.withdrawalRelated.WithdrawalRequest;
import com.payflow.transaction.internal.util.withdrawalRelated.WithdrawalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final AuthFacade authFacade;
    private final TransactionService transactionService;


    @PostMapping("/request/deposit")
    public ResponseEntity<DepositResponse> requestDeposit(Authentication authentication, @RequestBody DepositRequest depositRequest) {
        Long userId = authFacade.extractUserId(authentication);

        return ResponseEntity.ok(transactionService.requestDeposit(userId, depositRequest.amount(),depositRequest.idempotencyKey(),depositRequest.depositCurrency()));
    }

    @PostMapping("/send/money")
    public ResponseEntity<SendMoneyResponse> sendMoney(Authentication authentication, @RequestBody SendMoneyRequest request){
        Long userId = authFacade.extractUserId(authentication);

        return ResponseEntity.ok(transactionService.sendMoney(userId,request.receiverWalletTag(),request.idempotencyKey(),request.amount(),request.description()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WithdrawalResponse> withdrawMoney(Authentication authentication, @RequestBody WithdrawalRequest request){
        Long userId = authFacade.extractUserId(authentication);

        return ResponseEntity.ok(transactionService.withdrawMoneyMpesa(userId,request.amount(),request.idempotencyKey(),request.phoneNumber(),request.description()));
    }
}
