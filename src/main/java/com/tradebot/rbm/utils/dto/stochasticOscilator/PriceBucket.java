package com.tradebot.rbm.utils.dto.stochasticOscilator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * Represents aggregated price data for a time bucket (candle)
 */
@Data
public class PriceBucket {
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private int tradeCount;
    private final LocalDateTime timestamp;

    public PriceBucket(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        this.volume = BigDecimal.ZERO;
        this.tradeCount = 0;
    }

    public void addTrade(BigDecimal price, BigDecimal qty) {
        if (open == null) {
            open = price;
            high = price;
            low = price;
        }

        close = price;
        high = high.max(price);
        low = low.min(price);
        volume = volume.add(qty);
        tradeCount++;
    }

}
