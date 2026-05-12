package com.payflow.transaction.internal.controllers;

import com.payflow.transaction.internal.services.ExchangeService;
import com.payflow.transaction.internal.util.ExchangeRequest;
import com.payflow.transaction.internal.util.ExchangeResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/exchange")
@RequiredArgsConstructor
public class ExchangeController {
    private final ExchangeService exchangeService;
    private static final Logger logger = LoggerFactory.getLogger(ExchangeController.class);

    @PostMapping("/add/rate")
    public ResponseEntity<ExchangeResponse> addRate(@RequestBody ExchangeRequest request){
        logger.info("Currency={}",request.currency());
        return ResponseEntity.ok(exchangeService.addRate(request.buyRate(),request.sellRate(),request.currency(),request.rateToUsd()));
    }
}
