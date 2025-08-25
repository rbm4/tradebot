package com.tradebot.rbm.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DoubleLimitExample {
    public static Double limitDecimal(Double value, int decimalPlaces) {
        BigDecimal bigDecimal = BigDecimal.valueOf(value);
        BigDecimal roundedValue = bigDecimal.setScale(decimalPlaces, RoundingMode.HALF_UP);

        return roundedValue.doubleValue();
    }

    public static void main(String[] args) {
        Double value = 123.456789; // Example value
        int decimalPlaces = 3; // Example number of decimal places

        Double limitedValue = limitDecimal(value, decimalPlaces);
        System.out.println("Original value: " + value);
        System.out.println("Limited value: " + limitedValue);
    }
}