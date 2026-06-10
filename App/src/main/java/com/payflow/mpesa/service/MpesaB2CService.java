package com.payflow.mpesa.service;

import com.payflow.mpesa.dto.AccessTokenResponse;
import com.payflow.mpesa.dto.MpesaConfiguration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MpesaB2CService {
    private final MpesaConfiguration mpesaConfiguration;
    private final MpesaAuthService mpesaAuthService;
    private final Logger logger = LoggerFactory.getLogger(MpesaB2CService.class);

    public void sendB2C(String phoneNumber, BigDecimal amount,String transactionId,String description){
        AccessTokenResponse accessToken = mpesaAuthService.generateAccessToken();
        logger.info("accessToken={}",accessToken);

        Map<String,Object> body = Map.of(
                "OriginatorConversationID",transactionId,
                "InitiatorName","testapi",
                "SecurityCredential",mpesaConfiguration.getSecurityCredential(),
                "CommandID","BusinessPayment",
                "Amount",amount.setScale(0, RoundingMode.FLOOR).toString(),
                "PartyA",mpesaConfiguration.getShortCode(),
                "PartyB",phoneNumber,
                "Remarks",description,
                "QueueTimeOutUrl",mpesaConfiguration.getQueueTimeOutUrl(),
                "ResultURL",mpesaConfiguration.getResultUrl()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(accessToken.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange(mpesaConfiguration.getB2cUrl(), HttpMethod.POST,new HttpEntity<>(body,headers), Map.class);

    }
}
