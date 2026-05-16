package com.payflow.transaction.internal.controllers;

import com.payflow.transaction.internal.services.ExchangeService;
import com.payflow.transaction.internal.util.exchange.ExchangeRequest;
import com.payflow.transaction.internal.util.exchange.ExchangeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/exchange")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ExchangeController {
    private final ExchangeService exchangeService;

    @PostMapping("/add/rate")
    public ResponseEntity<ExchangeResponse> addRate(@RequestBody ExchangeRequest request) {
        return ResponseEntity.ok(exchangeService.addRate(request.buyRate(), request.sellRate(), request.currency(), request.rateToUsd()));
    }

    @PutMapping("/update/rate")
    public ResponseEntity<ExchangeResponse> updateRate(@RequestBody ExchangeRequest request) {
        return ResponseEntity.ok(exchangeService.updateRate(request.buyRate(), request.sellRate(), request.currency(), request.rateToUsd()));
    }
}
