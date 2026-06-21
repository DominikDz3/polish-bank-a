package com.polishbank.bank_a.integration.swift;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class SwiftExchangeRateProvider {

    private static final Map<String, BigDecimal> RATES_TO_PLN = new LinkedHashMap<>();

    static {
        RATES_TO_PLN.put("PLN", new BigDecimal("1.00"));
        RATES_TO_PLN.put("EUR", new BigDecimal("4.30"));
        RATES_TO_PLN.put("USD", new BigDecimal("4.00"));
        RATES_TO_PLN.put("GBP", new BigDecimal("5.00"));
        RATES_TO_PLN.put("CHF", new BigDecimal("4.50"));
        RATES_TO_PLN.put("CZK", new BigDecimal("0.18"));
        RATES_TO_PLN.put("SEK", new BigDecimal("0.40"));
        RATES_TO_PLN.put("NOK", new BigDecimal("0.39"));
        RATES_TO_PLN.put("JPY", new BigDecimal("0.027"));
    }

    public boolean supports(String currency) {
        return currency != null && RATES_TO_PLN.containsKey(currency.toUpperCase());
    }

    public Set<String> supportedCurrencies() {
        return RATES_TO_PLN.keySet();
    }

    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (amount == null) {
            throw new IllegalArgumentException("Brak kwoty do przeliczenia.");
        }
        String f = from == null ? "" : from.toUpperCase();
        String t = to == null ? "" : to.toUpperCase();
        BigDecimal rateFrom = RATES_TO_PLN.get(f);
        BigDecimal rateTo = RATES_TO_PLN.get(t);
        if (rateFrom == null) {
            throw new IllegalArgumentException("Niewspierana waluta: " + f);
        }
        if (rateTo == null) {
            throw new IllegalArgumentException("Niewspierana waluta: " + t);
        }
        if (f.equals(t)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(rateFrom).divide(rateTo, 2, RoundingMode.HALF_UP);
    }
}