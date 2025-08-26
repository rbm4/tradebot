package com.tradebot.rbm.utils.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PendingBuyOrderDTO {
    final String orderId;
    final String symbol;
    final BigDecimal buyPrice;
    final BigDecimal quantity;
    final LocalDateTime timestamp;
    final BigDecimal expectedProfitPrice;
    boolean isExecuted;

    public PendingBuyOrderDTO(String orderId, String symbol, BigDecimal buyPrice, BigDecimal quantity,
            BigDecimal expectedProfitPrice) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.buyPrice = buyPrice;
        this.quantity = quantity;
        this.timestamp = LocalDateTime.now();
        this.expectedProfitPrice = expectedProfitPrice;
        this.isExecuted = false;
    }
}
