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
public class MarketMomentum {

    private BigDecimal priceChange1h;
    private BigDecimal priceChange6h;
    private BigDecimal priceChange24h;
    private MomentumDirection shortTermTrend;
    private MomentumDirection mediumTermTrend;
    private MomentumDirection longTermTrend;
    private boolean isTrendAligned;
    private boolean isSuitableForScalping;
    private BigDecimal momentumScore;

    public enum MomentumDirection {
        STRONG_BULLISH, // Price increasing strongly
        BULLISH, // Price increasing moderately
        NEUTRAL, // Price relatively stable
        BEARISH, // Price decreasing moderately
        STRONG_BEARISH // Price decreasing strongly
    }
}
