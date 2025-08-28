package com.tradebot.rbm.utils.dto.stochasticOscilator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Complete analysis result containing current state and trend information
 */
@Data
public class StochasticAnalysis {
    private final StochasticResult current;
    private final List<StochasticResult> history;
    private final StochasticTrend trend;
    private final String recommendation;
    private final BigDecimal confidence;

    public StochasticAnalysis(StochasticResult current, List<StochasticResult> history,
            StochasticTrend trend, String recommendation, BigDecimal confidence) {
        this.current = current;
        this.history = new ArrayList<>(history);
        this.trend = trend;
        this.recommendation = recommendation;
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return String.format("StochasticAnalysis{Current=%s, Trend=%s, Recommendation='%s', Confidence=%.2f%%}",
                current, trend, recommendation, confidence);
    }
}
