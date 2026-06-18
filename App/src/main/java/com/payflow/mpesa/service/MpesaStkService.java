package com.payflow.mpesa.service;

import com.payflow.mpesa.dto.MpesaConfiguration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MpesaStkService {
    private final MpesaConfiguration mpesaConfiguration;
    private final MpesaAuthService authService;
    private final Logger logger = LoggerFactory.getLogger(MpesaStkService.class);


    public void initiateSTKPush(String mpesaPhoneNumber, BigDecimal amount, Long transactionId) {
        logger.info("initiating a stk push");
        String token = authService.getAccessToken();

        logger.info("token={}", token);

        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String password = Base64.getEncoder().encodeToString(
                (mpesaConfiguration.getShortCode() + mpesaConfiguration.getPasskey() + timestamp)
                        .getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = new HashMap<>();
        body.put("BusinessShortCode", mpesaConfiguration.getShortCode());
        body.put("Password", password);
        body.put("Timestamp", timestamp);
        body.put("TransactionType", mpesaConfiguration.getTransactionType());
        body.put("Amount", amount.setScale(0, RoundingMode.CEILING).toString());
        body.put("PartyA", mpesaPhoneNumber);
        body.put("PartyB", mpesaConfiguration.getShortCode());
        body.put("PhoneNumber", mpesaPhoneNumber);
        body.put("CallBackURL", mpesaConfiguration.getCallBackURL());
        body.put("AccountReference", String.valueOf(transactionId));
        body.put("TransactionDesc", "PayFlow Deposit");
        body.put("Remarks", "PayFlow Deposit");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        logger.info("header info={}", headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    mpesaConfiguration.getStkURL(), HttpMethod.POST, new HttpEntity<>(body, headers), String.class
            );
            logger.info("STK Push response: {}", response.getBody());
        } catch (HttpClientErrorException ex) {
            logger.error("STK Push 4xx error: {}", ex.getResponseBodyAsString());
            throw ex;
        } catch (HttpServerErrorException ex) {
            logger.error("STK Push 5xx error: {}", ex.getResponseBodyAsString());
            throw ex;
        } catch (Exception ex) {
            logger.error("STK Push unexpected error: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
