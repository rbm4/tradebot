package com.tradebot.rbm.utils.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TradeMomentum {
    private final String direction;
    private final long buyCount;
    private final long sellCount;
    private final BigDecimal buyVolume;
    private final BigDecimal sellVolume;
    private final BigDecimal totalVolume;

    public TradeMomentum(String direction, long buyCount, long sellCount,
            BigDecimal buyVolume, BigDecimal sellVolume, BigDecimal totalVolume) {
        this.direction = direction;
        this.buyCount = buyCount;
        this.sellCount = sellCount;
        this.buyVolume = buyVolume;
        this.sellVolume = sellVolume;
        this.totalVolume = totalVolume;
    }
}
