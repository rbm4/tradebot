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
public class VolumeAnalysis {

    private BigDecimal volumeRatio;
    private BigDecimal volume6h;
    private BigDecimal volume1h;
    private BigDecimal avgVolumePerHour6h;
    private boolean isVolumeIncreasing;
    private boolean isPriceVolumeAlignment;
    private MarketStrength marketStrength;
    private BigDecimal liquidityScore;
    private BigDecimal avgTradeSize;
    private BigDecimal bidAskSpread;
    private boolean hasGoodLiquidity;
    private VolumeSignal signal;

    public enum MarketStrength {
        VERY_STRONG, // High volume + strong price movement
        STRONG, // Good volume + moderate price movement
        MODERATE, // Average volume + price movement
        WEAK, // Low volume or conflicting signals
        VERY_WEAK // Very low volume or negative signals
    }

    public enum VolumeSignal {
        STRONG_BUY, // High volume with bullish price action
        MODERATE_BUY, // Moderate volume with bullish price action
        NEUTRAL, // Mixed or unclear signals
        AVOID, // Low volume or bearish signals
        STRONG_AVOID // Very low volume or very bearish signals
    }
}
