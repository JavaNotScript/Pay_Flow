package com.payflow.transaction.internal.controllers;

import com.payflow.auth.api.AuthAdapter;
import com.payflow.auth.api.AuthFacade;
import com.payflow.auth.internal.dtos.UserDTO;
import com.payflow.transaction.internal.util.sendRelated.SendMoneyResponse;
import com.payflow.transaction.internal.services.TransactionService;
import com.payflow.transaction.internal.util.depositRelated.DepositRequestMpesa;
import com.payflow.transaction.internal.util.depositRelated.DepositResponse;
import com.payflow.transaction.internal.util.sendRelated.SendMoneyRequest;
import com.payflow.transaction.internal.util.TransactionStatementDTO;
import com.payflow.transaction.internal.util.withdrawalRelated.WithdrawalRequest;
import com.payflow.transaction.internal.util.withdrawalRelated.WithdrawalResponse;
import com.payflow.wallet.api.WalletAdapter;
import com.payflow.wallet.internal.util.WalletInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
public class TransactionController {
    private final AuthFacade authFacade;
    private final TransactionService transactionService;
    private final WalletAdapter walletAdapter;
    private final AuthAdapter authAdapter;


    @PostMapping("/request/deposit/mpesa")
    public ResponseEntity<DepositResponse> requestDepositMpesa(Authentication authentication, @RequestBody DepositRequestMpesa depositRequestMpesa) {
        Long userId = authAdapter.extractUserId(authentication);
        UserDTO mpesaPhoneNumber = authAdapter.getUserInfoByUserId(userId);

        return ResponseEntity.ok(transactionService.requestDepositMpesa(userId, depositRequestMpesa.amount(), depositRequestMpesa.idempotencyKey(),mpesaPhoneNumber.getMpesaPhoneNumber()));
    }

    @PostMapping("/send/money")
    public ResponseEntity<SendMoneyResponse> sendMoney(Authentication authentication, @RequestBody SendMoneyRequest request){
        Long userId = authFacade.extractUserId(authentication);

        return ResponseEntity.ok(transactionService.sendMoney(userId,request.receiverWalletTag(),request.idempotencyKey(),request.amount(),request.description()));
    }

    @PostMapping("/request/withdraw/mpesa")
    public ResponseEntity<WithdrawalResponse> withdrawMoney(Authentication authentication, @RequestBody WithdrawalRequest request){
        Long userId = authFacade.extractUserId(authentication);

        return ResponseEntity.ok(transactionService.withdrawMoneyMpesa(userId,request.amount(),request.idempotencyKey(),request.phoneNumber(),request.description()));
    }

    @GetMapping("/get/statement")
    public ResponseEntity<Page<TransactionStatementDTO>> getTransactionStatement(Authentication authentication){
        Long userId = authFacade.extractUserId(authentication);

        WalletInfo walletInfo = walletAdapter.getWalletByUserId(userId);

        return ResponseEntity.ok(transactionService.getTransactionStatement(walletInfo.walletId()));
    }

    @GetMapping("/get/statement/{transactionId}")
    public TransactionStatementDTO getTransactionStatementById(Authentication authentication,@PathVariable Long transactionId){
        Long userId = authFacade.extractUserId(authentication);

        WalletInfo walletInfo = walletAdapter.getWalletByUserId(userId);
        return ResponseEntity.ok(transactionService.getTransactionStatementById(walletInfo.walletId(),transactionId)).getBody();
    }
}
