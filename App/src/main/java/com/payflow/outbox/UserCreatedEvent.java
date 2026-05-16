package com.payflow.outbox;

public record UserCreatedEvent(Long userId,String email,String walletTag) {
}
