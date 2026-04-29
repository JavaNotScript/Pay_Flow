package com.payflow.common.security;

public interface CurrentUser {
    Long getUserId();

    String getEmail();

    String getRole();
}
