package com.payflow.mpesa.service;

import com.payflow.mpesa.dto.MpesaConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MpesaStkService {
    private final MpesaConfiguration mpesaConfiguration;
    private final MpesaAuthService authService;

    private String description = "Payflow Deposit";

    public void initiateSTKPush(String mpesaPhoneNumber, BigDecimal amount,Long transactionId){
        String token = authService.getAccessToken();

        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String password = Base64.getEncoder().encodeToString(
                (mpesaConfiguration.getShortCode()+mpesaConfiguration.getPasskey()+timestamp)
                        .getBytes(StandardCharsets.UTF_8));

        Map<String ,Object> body = Map.of(
                "BusinessShortCode",mpesaConfiguration.getShortCode(),
                "Password",password,
                "Timestamp",timestamp,
                "TransactionType",mpesaConfiguration.getTransactionType(),
                "Amount",amount.setScale(0, RoundingMode.CEILING).toString(),
                "PartyA",mpesaPhoneNumber,
                "PartyB",mpesaConfiguration.getShortCode(),
                "PhoneNumber",mpesaPhoneNumber,
                "CallBackURL",mpesaConfiguration.getCallBackURL(),
                "AccountReference",transactionId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        RestTemplate restTemplate = new RestTemplate();

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    mpesaConfiguration.getStkURL(), HttpMethod.POST,new HttpEntity<>(body,headers),String.class
            );
        }catch (HttpClientErrorException ex){
            throw ex;
        }
    }
}
