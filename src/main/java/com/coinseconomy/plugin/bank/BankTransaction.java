package com.coinseconomy.plugin.bank;

public record BankTransaction(Type type, double amount, long timestamp) {
    public enum Type {
        DEPOSIT,
        WITHDRAW,
        INTEREST
    }
}
