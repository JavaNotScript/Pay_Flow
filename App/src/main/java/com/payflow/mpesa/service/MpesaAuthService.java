package com.payflow.mpesa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.common.ex.MpesaWafBlockException;
import com.payflow.mpesa.dto.AccessTokenResponse;
import com.payflow.mpesa.dto.MpesaConfiguration;
import com.payflow.mpesa.util.HelperUtility;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;

import static com.payflow.mpesa.util.Constants.*;

@Service
public class MpesaAuthService {
    private final MpesaConfiguration mpesaConfiguration;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(MpesaAuthService.class);
    private AccessTokenResponse cachedToken;
    private Instant expiryTime;

    public MpesaAuthService(MpesaConfiguration mpesaConfiguration, OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.mpesaConfiguration = mpesaConfiguration;
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    public synchronized String getAccessToken(){
        if (cachedToken != null && Instant.now().isBefore(expiryTime)){
            return cachedToken.getAccessToken();
        }

        cachedToken = generateAccessToken();

        if (cachedToken.getAccessToken() == null || cachedToken.getAccessToken().isBlank()) {
            throw new MpesaWafBlockException("Received empty access token");
        }

        expiryTime = Instant.now().plusSeconds(Long.parseLong(cachedToken.getExpiresIn()) - 60);
        return cachedToken.getAccessToken();
    }

    public AccessTokenResponse generateAccessToken() {
        String credentials = String.format("%s:%s", mpesaConfiguration.getConsumerKey(), mpesaConfiguration.getConsumerSecret());

        String encodedCredentials = HelperUtility.toBase64(credentials);

        Request request = new Request.Builder()
                .url(String.format("%s?grant_type=%s", mpesaConfiguration.getOauthEndpoint(), mpesaConfiguration.getGrantType()))
                .get()
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BASIC_AUTH_STRING, encodedCredentials))
                .addHeader(CACHE_CONTROL_HEADER, CACHE_CONTROL_HEADER_VALUE)
                .addHeader("User-Agent","PayFlow/1.0")
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            assert response.body() != null;

            String responseBody = response.body().string();
//            logger.info("Mpesa auth response code={} body={}", response.code(), responseBody);

            if (responseBody.trim().isEmpty()) {
                throw new MpesaWafBlockException("Mpesa auth returned empty body — likely Incapsula throttling. Code=" + response.code());
            }

            if (responseBody.trim().startsWith("<")) {
                throw new MpesaWafBlockException("Mpesa returned HTML instead of JSON. Possible WAF block.");
            }

            AccessTokenResponse tokenResponse = objectMapper.readValue(responseBody, AccessTokenResponse.class);

            if (tokenResponse.getAccessToken() == null) {
                throw new RuntimeException(
                        String.format("Failed to get access token: [%s] %s", tokenResponse.getErrorCode(), tokenResponse.getErrorMessage())
                );
            }

            return tokenResponse;

        } catch (IOException e) {
            logger.error("Failed to generate access token", e);
            throw new RuntimeException("Failed to generate mpesa access token", e);
        }

    }
}
