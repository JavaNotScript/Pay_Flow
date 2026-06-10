package com.payflow.mpesa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.mpesa.dto.AccessTokenResponse;
import com.payflow.mpesa.dto.MpesaConfiguration;
import com.payflow.mpesa.util.HelperUtility;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.payflow.mpesa.util.Constants.*;

@Service
@RequiredArgsConstructor
public class MpesaAuthService {
    private final MpesaConfiguration mpesaConfiguration;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public AccessTokenResponse generateAccessToken() {
        String credentials = String.format("%s:%s", mpesaConfiguration.getConsumerKey(), mpesaConfiguration.getConsumerSecret());

        String encodedCredentials = HelperUtility.toBase64(credentials);

        Request request = new Request.Builder()
                .url(String.format("%s?grant_type=%s", mpesaConfiguration.getOauthEndpoint(), mpesaConfiguration.getGrantType()))
                .get()
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BASIC_AUTH_STRING, encodedCredentials))
                .addHeader(CACHE_CONTROL_HEADER, CACHE_CONTROL_HEADER_VALUE)
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            assert response.body() != null;

            String responseBody = response.body().string();

            AccessTokenResponse tokenResponse = objectMapper.readValue(responseBody, AccessTokenResponse.class);

            if (tokenResponse.getAccessToken() == null) {
                throw new RuntimeException(
                        String.format("Failed to get access token: [%s] %s", tokenResponse.getErrorCode(), tokenResponse.getErrorMessage())
                );
            }

            return tokenResponse;

        } catch (IOException e) {
            throw new RuntimeException();
        }

    }
}
