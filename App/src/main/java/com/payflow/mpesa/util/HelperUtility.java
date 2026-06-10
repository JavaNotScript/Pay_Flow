package com.payflow.mpesa.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HelperUtility {

    public static String toBase64(String credentials) {
        byte[] bytes = credentials.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
