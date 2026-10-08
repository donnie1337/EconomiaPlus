package com.coinseconomy.plugin.economy;

import java.math.BigDecimal;
import java.util.Locale;

public final class MoneyParser {
    private MoneyParser() {}

    public static double parse(String input) {
        if (input == null) return Double.NaN;
        String value = input.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (value.isEmpty()) return Double.NaN;

        BigDecimal multiplier = BigDecimal.ONE;
        char suffix = value.charAt(value.length() - 1);
        switch (suffix) {
            case 'K' -> multiplier = BigDecimal.valueOf(1_000L);
            case 'M' -> multiplier = BigDecimal.valueOf(1_000_000L);
            case 'B' -> multiplier = BigDecimal.valueOf(1_000_000_000L);
            case 'T' -> multiplier = BigDecimal.valueOf(1_000_000_000_000L);
            default -> {}
        }
        if (multiplier.compareTo(BigDecimal.ONE) != 0) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty()) return Double.NaN;

        if (value.contains(",")) {
            value = value.replace(".", "").replace(',', '.');
        } else if (value.matches("[0-9]{1,3}(\\.[0-9]{3})+")) {
            value = value.replace(".", "");
        }

        try {
            double result = new BigDecimal(value).multiply(multiplier).doubleValue();
            if (!Double.isFinite(result) || result <= 0.0D) return Double.NaN;
            return Math.round(result * 100.0D) / 100.0D;
        } catch (NumberFormatException exception) {
            return Double.NaN;
        }
    }
}
