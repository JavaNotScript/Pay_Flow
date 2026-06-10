package com.payflow.mpesa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.common.ex.MpesaWafBlockException;
import com.payflow.mpesa.dto.AccessTokenResponse;
import com.payflow.mpesa.dto.MpesaConfiguration;
import com.payflow.mpesa.util.HelperUtility;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.payflow.mpesa.util.Constants.*;

@Service
@RequiredArgsConstructor
public class MpesaAuthService {
    private final MpesaConfiguration mpesaConfiguration;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(MpesaAuthService.class);

    public AccessTokenResponse generateAccessToken() {
        String credentials = String.format("%s:%s", mpesaConfiguration.getConsumerKey(), mpesaConfiguration.getConsumerSecret());

        String encodedCredentials = HelperUtility.toBase64(credentials);


        logger.info("Using consumerKey='{}' consumerSecret='{}'",
                mpesaConfiguration.getConsumerKey(),
                mpesaConfiguration.getConsumerSecret());
        logger.info("Encoded credentials='{}'", encodedCredentials);



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
            logger.info("Mpesa auth response code={} body={}", response.code(), responseBody);

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
            logger.error("Failed to generate access token", e); // ✅ log the actual exception
            throw new RuntimeException("Failed to generate mpesa access token", e);
        }

    }
}
