package com.tradebot.rbm.utils.dto.stochasticOscilator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * Represents the result of Stochastic Oscillator calculation
 */
@Data
public class StochasticResult {
    private final BigDecimal kPercent;
    private final BigDecimal dPercent;
    private final StochasticSignal signal;
    private final LocalDateTime timestamp;

    public StochasticResult(BigDecimal kPercent, BigDecimal dPercent, StochasticSignal signal,
            LocalDateTime timestamp) {
        this.kPercent = kPercent;
        this.dPercent = dPercent;
        this.signal = signal;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("StochasticResult{K=%.2f%%, D=%.2f%%, Signal=%s, Time=%s}",
                kPercent, dPercent, signal, timestamp);
    }
}
