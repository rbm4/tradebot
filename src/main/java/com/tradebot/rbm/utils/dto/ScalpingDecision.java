package com.tradebot.rbm.utils.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalpingDecision {

    private boolean shouldBuy;
    private boolean shouldSell;
    private boolean shouldAvoid;
    private ScalpingSignal signal;
    private BigDecimal confidence;
    private String reason;
    private VolumeAnalysis volumeAnalysis;
    private MarketMomentum marketMomentum;
    private LiquidityAnalysis liquidityAnalysis;
    private BigDecimal suggestedTradeSize;
    private BigDecimal suggestedProfitMargin;
    private BigDecimal riskScore;

    public enum ScalpingSignal {
        STRONG_BUY, // All indicators positive, high confidence
        MODERATE_BUY, // Most indicators positive, moderate confidence
        WEAK_BUY, // Some indicators positive, low confidence
        HOLD, // Mixed signals, wait for better opportunity
        AVOID, // Negative signals, avoid trading
        EMERGENCY_EXIT // Very negative signals, exit positions immediately
    }
}
