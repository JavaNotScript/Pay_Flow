package com.payflow.mpesa.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
@Getter
@Setter
public class MpesaConfiguration {
    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.oauth-endpoint}")
    private String oauthEndpoint;

    @Value("${mpesa.grant-type}")
    private String grantType;

    @Value("${mpesa.security-credential}")
    private String securityCredential;

    @Value("${mpesa.shortcode}")
    private String shortCode;

    @Value("${mpesa.b2c.url}")
    private String b2cUrl;

    @Value("${mpesa.b2c.queue.timeout.url}")
    private String queueTimeOutUrl;

    @Value("${mpesa.b2c.result-url}")
    private String resultUrl;
}
