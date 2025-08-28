package com.tradebot.rbm.utils.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ScalpingAction {
    private final String type;
    private final BigDecimal price;
    private final BigDecimal quantity;
    private final String reason;

    public ScalpingAction(String type, BigDecimal price, BigDecimal quantity, String reason) {
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.reason = reason;
    }
}
