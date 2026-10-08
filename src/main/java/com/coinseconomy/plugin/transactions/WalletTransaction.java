package com.coinseconomy.plugin.transactions;

public record WalletTransaction(Type type, double amount, long timestamp, String detail) {
    public enum Type {
        PAYMENT_SENT,
        PAYMENT_RECEIVED,
        BANK_DEPOSIT,
        BANK_WITHDRAW,
        ADMIN_ADD,
        ADMIN_REMOVE,
        SHOP_BUY,
        SHOP_SELL
    }
}
