package com.tradebot.rbm.utils.dto.stochasticOscilator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * Represents a single price data point for analysis
 */
@Data
public class PriceData {
    private final BigDecimal high;
    private final BigDecimal low;
    private final BigDecimal close;
    private final LocalDateTime timestamp;

    public PriceData(BigDecimal high, BigDecimal low, BigDecimal close, LocalDateTime timestamp) {
        this.high = high;
        this.low = low;
        this.close = close;
        this.timestamp = timestamp;
    }
}
