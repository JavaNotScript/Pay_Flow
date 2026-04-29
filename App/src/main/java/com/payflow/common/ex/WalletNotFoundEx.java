package com.payflow.common.ex;

public class WalletNotFoundEx extends RuntimeException {
    public WalletNotFoundEx(String message) {
        super(message);
    }
}
