package com.tradebot.rbm.utils.dto.stochasticOscilator;

import lombok.Data;

/**
 * Represents trend information derived from Stochastic analysis
 */
@Data
public class StochasticTrend {
    private final String direction; // BULLISH, BEARISH, NEUTRAL
    private final int strength; // 1-5 scale
    private final boolean isOverbought;
    private final boolean isOversold;
    private final boolean hasRecentCrossover;

    public StochasticTrend(String direction, int strength, boolean isOverbought,
            boolean isOversold, boolean hasRecentCrossover) {
        this.direction = direction;
        this.strength = strength;
        this.isOverbought = isOverbought;
        this.isOversold = isOversold;
        this.hasRecentCrossover = hasRecentCrossover;
    }

    @Override
    public String toString() {
        return String.format(
                "StochasticTrend{Direction=%s, Strength=%d, Overbought=%s, Oversold=%s, RecentCrossover=%s}",
                direction, strength, isOverbought, isOversold, hasRecentCrossover);
    }
}