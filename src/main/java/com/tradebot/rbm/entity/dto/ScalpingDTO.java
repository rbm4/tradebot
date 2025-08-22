package com.tradebot.rbm.entity.dto;

import java.math.BigDecimal;

import com.binance.connector.client.spot.rest.model.GetAccountResponse;
import com.binance.connector.client.spot.rest.model.GetOpenOrdersResponse;
import com.binance.connector.client.spot.rest.model.TickerBookTickerResponse1;
import com.tradebot.rbm.utils.dto.LiquidityAnalysis;
import com.tradebot.rbm.utils.dto.MarketMomentum;
import com.tradebot.rbm.utils.dto.ScalpingDecision;
import com.tradebot.rbm.utils.dto.VolumeAnalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalpingDTO {

    // Trading symbol information
    private String symbol;
    private String baseAsset;
    private String quoteAsset;

    // Market price information
    private BigDecimal currentPrice;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal bidAskSpread;
    private BigDecimal spreadPercentage;

    // Account balance information
    private BigDecimal baseBalance;
    private BigDecimal quoteBalance;
    private BigDecimal totalBalanceUSDT;
    private BigDecimal availableTradeAmount;

    // Ticker information for different timeframes
    private TickerDto tickerInfo1h;
    private TickerDto tickerInfo6h;
    private TickerDto tickerInfo24h;
    private TickerBookTickerResponse1 tickerBook;

    // Account and order information
    private GetAccountResponse accountInfo;
    private GetOpenOrdersResponse openOrders;
    private boolean hasOpenOrders;
    private int openOrdersCount;

    // Volume and market analysis
    private VolumeAnalysis volumeAnalysis;
    private LiquidityAnalysis liquidityAnalysis;
    private MarketMomentum marketMomentum;
    private ScalpingDecision scalpingDecision;

    // Trading strategy parameters
    private BigDecimal profitMarginPercentage;
    private BigDecimal minTradeAmountUSDT;
    private BigDecimal maxTradePercentage;
    private BigDecimal priceDropThreshold;
    private int quantityPrecision;
    private int pricePrecision;

    // Calculated trading values
    private BigDecimal suggestedTradeSize;
    private BigDecimal suggestedQuantity;
    private BigDecimal suggestedBuyPrice;
    private BigDecimal suggestedSellPrice;
    private BigDecimal expectedProfitUSDT;
    private BigDecimal expectedProfitPercentage;

    // Risk management
    private BigDecimal riskScore;
    private BigDecimal confidenceLevel;
    private BigDecimal maxLossAmount;
    private BigDecimal stopLossPrice;
    private BigDecimal takeProfitPrice;

    // Trading signals and decisions
    private boolean shouldBuy;
    private boolean shouldSell;
    private boolean shouldHold;
    private boolean shouldAvoid;
    private String tradingSignal;
    private String decisionReason;

    // Execution information
    private String executionStrategy;
    private String orderType;
    private String timeInForce;
    private BigDecimal urgencyScore;

    // Market conditions assessment
    private String marketCondition; // BULLISH, BEARISH, SIDEWAYS, VOLATILE
    private String volumeCondition; // HIGH, NORMAL, LOW
    private String liquidityCondition; // EXCELLENT, GOOD, MODERATE, POOR
    private String momentumCondition; // STRONG_UP, UP, NEUTRAL, DOWN, STRONG_DOWN

    // Timestamps and tracking
    private Long analysisTimestamp;
    private String analysisId;
    private String sessionId;

    // Additional metadata
    private String strategyVersion;
    private String configurationUsed;
    private String analysisNotes;

    /**
     * Creates a ScalpingDTO with default trading parameters
     */
    public static ScalpingDTO createDefault(String symbol) {
        return ScalpingDTO.builder()
                .symbol(symbol)
                .profitMarginPercentage(new BigDecimal("0.3"))
                .minTradeAmountUSDT(new BigDecimal("10.0"))
                .maxTradePercentage(new BigDecimal("20.0"))
                .priceDropThreshold(new BigDecimal("1.0"))
                .quantityPrecision(6)
                .pricePrecision(8)
                .analysisTimestamp(System.currentTimeMillis())
                .strategyVersion("1.0")
                .build();
    }

    /**
     * Calculates bid-ask spread percentage
     */
    public void calculateSpreadPercentage() {
        if (bidPrice != null && askPrice != null && bidPrice.compareTo(BigDecimal.ZERO) > 0) {
            this.bidAskSpread = askPrice.subtract(bidPrice);
            this.spreadPercentage = bidAskSpread.divide(bidPrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
    }

    /**
     * Calculates suggested trade size based on balance and risk parameters
     */
    public void calculateSuggestedTradeSize() {
        if (quoteBalance != null && maxTradePercentage != null && currentPrice != null) {
            BigDecimal maxTradeAmount = quoteBalance.multiply(maxTradePercentage)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

            this.suggestedTradeSize = maxTradeAmount.max(minTradeAmountUSDT);
            this.suggestedQuantity = suggestedTradeSize.divide(currentPrice, quantityPrecision,
                    java.math.RoundingMode.DOWN);
        }
    }

    /**
     * Calculates expected profit based on suggested trade size and profit margin
     */
    public void calculateExpectedProfit() {
        if (suggestedTradeSize != null && profitMarginPercentage != null) {
            this.expectedProfitUSDT = suggestedTradeSize.multiply(profitMarginPercentage)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            this.expectedProfitPercentage = profitMarginPercentage;
        }
    }

    /**
     * Calculates suggested buy and sell prices
     */
    public void calculateSuggestedPrices() {
        if (currentPrice != null && profitMarginPercentage != null) {
            this.suggestedBuyPrice = currentPrice; // Market price for buy

            BigDecimal profitMultiplier = BigDecimal.ONE.add(
                    profitMarginPercentage.divide(new BigDecimal("100")));
            this.suggestedSellPrice = currentPrice.multiply(profitMultiplier)
                    .setScale(pricePrecision, java.math.RoundingMode.UP);
        }
    }

    /**
     * Updates market condition assessments
     */
    public void updateMarketConditions() {
        if (marketMomentum != null) {
            // Update momentum condition
            switch (marketMomentum.getShortTermTrend()) {
                case STRONG_BULLISH -> this.momentumCondition = "STRONG_UP";
                case BULLISH -> this.momentumCondition = "UP";
                case NEUTRAL -> this.momentumCondition = "NEUTRAL";
                case BEARISH -> this.momentumCondition = "DOWN";
                case STRONG_BEARISH -> this.momentumCondition = "STRONG_DOWN";
            }
        }

        if (volumeAnalysis != null) {
            // Update volume condition
            if (volumeAnalysis.getVolumeRatio().compareTo(new BigDecimal("1.5")) > 0) {
                this.volumeCondition = "HIGH";
            } else if (volumeAnalysis.getVolumeRatio().compareTo(new BigDecimal("0.8")) > 0) {
                this.volumeCondition = "NORMAL";
            } else {
                this.volumeCondition = "LOW";
            }
        }

        if (liquidityAnalysis != null) {
            // Update liquidity condition
            this.liquidityCondition = liquidityAnalysis.getLiquidityLevel().name();
        }
    }

    /**
     * Generates a comprehensive summary of the scalping analysis
     */
    public String generateAnalysisSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== SCALPING ANALYSIS SUMMARY ===\n");
        summary.append("Symbol: ").append(symbol).append("\n");
        summary.append("Current Price: ").append(currentPrice).append("\n");
        summary.append("Signal: ").append(tradingSignal).append("\n");
        summary.append("Confidence: ").append(confidenceLevel).append("%\n");
        summary.append("Suggested Trade Size: $").append(suggestedTradeSize).append("\n");
        summary.append("Expected Profit: $").append(expectedProfitUSDT).append("\n");
        summary.append("Risk Score: ").append(riskScore).append("\n");
        summary.append("Decision: ").append(decisionReason).append("\n");
        summary.append("Market Conditions: ").append(marketCondition).append("\n");
        summary.append("Volume: ").append(volumeCondition).append("\n");
        summary.append("Liquidity: ").append(liquidityCondition).append("\n");
        summary.append("Momentum: ").append(momentumCondition).append("\n");
        summary.append("==================================");

        return summary.toString();
    }

    /**
     * Checks if all required data for trading decision is available
     */
    public boolean isDataComplete() {
        return symbol != null &&
                currentPrice != null &&
                baseBalance != null &&
                quoteBalance != null &&
                volumeAnalysis != null &&
                liquidityAnalysis != null &&
                marketMomentum != null;
    }

    /**
     * Validates if trading conditions meet minimum requirements
     */
    public boolean isValidForTrading() {
        return isDataComplete() &&
                quoteBalance.compareTo(minTradeAmountUSDT) >= 0 &&
                liquidityAnalysis.isHasGoodLiquidity() &&
                riskScore != null && riskScore.compareTo(new BigDecimal("70")) < 0; // Risk score < 70%
    }
}
