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
public class LiquidityAnalysis {

    private BigDecimal quoteVolume24h;
    private BigDecimal avgTradeSize;
    private BigDecimal bidAskSpread;
    private BigDecimal spreadPercentage;
    private BigDecimal liquidityScore;
    private boolean hasGoodLiquidity;
    private boolean isSuitableForScalping;
    private LiquidityLevel liquidityLevel;
    private BigDecimal totalTrades;
    private BigDecimal marketDepthScore;

    public enum LiquidityLevel {
        EXCELLENT, // Very high liquidity, ideal for scalping
        GOOD, // Good liquidity, suitable for scalping
        MODERATE, // Moderate liquidity, caution needed
        POOR, // Poor liquidity, avoid scalping
        VERY_POOR // Very poor liquidity, avoid trading
    }
}
