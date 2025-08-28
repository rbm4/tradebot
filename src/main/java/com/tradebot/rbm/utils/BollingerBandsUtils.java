package com.tradebot.rbm.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for calculating and analyzing Bollinger Bands
 * Bollinger Bands consist of a moving average (middle band) and two standard
 * deviation bands
 * (upper and lower bands) that help identify overbought/oversold conditions and
 * volatility.
 */
public class BollingerBandsUtils {

    private static final int DEFAULT_PERIOD = 20;
    private static final BigDecimal DEFAULT_STANDARD_DEVIATION_MULTIPLIER = new BigDecimal("2.0");
    private static final int SCALE = 8;

    /**
     * Represents a single price data point for Bollinger Bands analysis
     */
    public static class PricePoint {
        private final BigDecimal price;
        private final BigDecimal volume;
        private final LocalDateTime timestamp;

        public PricePoint(BigDecimal price, BigDecimal volume, LocalDateTime timestamp) {
            this.price = price;
            this.volume = volume;
            this.timestamp = timestamp;
        }

        public PricePoint(BigDecimal price, LocalDateTime timestamp) {
            this(price, BigDecimal.ZERO, timestamp);
        }

        public BigDecimal getPrice() {
            return price;
        }

        public BigDecimal getVolume() {
            return volume;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Represents a single Bollinger Bands calculation result
     */
    public static class BollingerBandsResult {
        private final BigDecimal upperBand;
        private final BigDecimal middleBand; // Simple Moving Average
        private final BigDecimal lowerBand;
        private final BigDecimal currentPrice;
        private final BigDecimal bandWidth;
        private final BigDecimal percentB;
        private final BollingerSignal signal;
        private final LocalDateTime timestamp;

        public BollingerBandsResult(BigDecimal upperBand, BigDecimal middleBand, BigDecimal lowerBand,
                BigDecimal currentPrice, BollingerSignal signal, LocalDateTime timestamp) {
            this.upperBand = upperBand;
            this.middleBand = middleBand;
            this.lowerBand = lowerBand;
            this.currentPrice = currentPrice;
            this.signal = signal;
            this.timestamp = timestamp;

            // Calculate derived values
            this.bandWidth = calculateBandWidth(upperBand, middleBand, lowerBand);
            this.percentB = calculatePercentB(currentPrice, upperBand, lowerBand);
        }

        private BigDecimal calculateBandWidth(BigDecimal upper, BigDecimal middle, BigDecimal lower) {
            BigDecimal totalWidth = upper.subtract(lower);
            return totalWidth.divide(middle, SCALE, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        private BigDecimal calculatePercentB(BigDecimal price, BigDecimal upper, BigDecimal lower) {
            BigDecimal numerator = price.subtract(lower);
            BigDecimal denominator = upper.subtract(lower);

            if (denominator.compareTo(BigDecimal.ZERO) == 0) {
                return new BigDecimal("0.5"); // Neutral when bands are collapsed
            }

            return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
        }

        public BigDecimal getUpperBand() {
            return upperBand;
        }

        public BigDecimal getMiddleBand() {
            return middleBand;
        }

        public BigDecimal getLowerBand() {
            return lowerBand;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public BigDecimal getBandWidth() {
            return bandWidth;
        }

        public BigDecimal getPercentB() {
            return percentB;
        }

        public BollingerSignal getSignal() {
            return signal;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format(
                    "BollingerBands{Upper=%.4f, Middle=%.4f, Lower=%.4f, Price=%.4f, %%B=%.2f, Width=%.2f%%, Signal=%s}",
                    upperBand, middleBand, lowerBand, currentPrice, percentB.multiply(new BigDecimal("100")), bandWidth,
                    signal);
        }
    }

    /**
     * Enum representing different Bollinger Bands signals
     */
    public enum BollingerSignal {
        STRONG_BUY, // Price touches or breaks below lower band with high volume
        BUY, // Price near lower band
        NEUTRAL, // Price within normal range
        SELL, // Price near upper band
        STRONG_SELL, // Price touches or breaks above upper band with high volume
        SQUEEZE, // Low volatility, bands contracting
        EXPANSION, // High volatility, bands expanding
        REVERSAL_UP, // Price bouncing off lower band
        REVERSAL_DOWN // Price bouncing off upper band
    }

    /**
     * Represents volatility information derived from Bollinger Bands
     */
    public static class VolatilityInfo {
        private final String volatilityLevel; // LOW, NORMAL, HIGH, EXTREME
        private final BigDecimal bandWidthPercentile;
        private final boolean isSqueeze;
        private final boolean isExpansion;
        private final String trend; // CONTRACTING, EXPANDING, STABLE

        public VolatilityInfo(String volatilityLevel, BigDecimal bandWidthPercentile,
                boolean isSqueeze, boolean isExpansion, String trend) {
            this.volatilityLevel = volatilityLevel;
            this.bandWidthPercentile = bandWidthPercentile;
            this.isSqueeze = isSqueeze;
            this.isExpansion = isExpansion;
            this.trend = trend;
        }

        public String getVolatilityLevel() {
            return volatilityLevel;
        }

        public BigDecimal getBandWidthPercentile() {
            return bandWidthPercentile;
        }

        public boolean isSqueeze() {
            return isSqueeze;
        }

        public boolean isExpansion() {
            return isExpansion;
        }

        public String getTrend() {
            return trend;
        }

        @Override
        public String toString() {
            return String.format("VolatilityInfo{Level=%s, Percentile=%.1f%%, Squeeze=%s, Expansion=%s, Trend=%s}",
                    volatilityLevel, bandWidthPercentile, isSqueeze, isExpansion, trend);
        }
    }

    /**
     * Complete analysis result containing current state and market insights
     */
    public static class BollingerBandsAnalysis {
        private final BollingerBandsResult current;
        private final List<BollingerBandsResult> history;
        private final VolatilityInfo volatilityInfo;
        private final MarketCondition marketCondition;
        private final String recommendation;
        private final BigDecimal confidence;
        private final List<String> insights;

        public BollingerBandsAnalysis(BollingerBandsResult current, List<BollingerBandsResult> history,
                VolatilityInfo volatilityInfo, MarketCondition marketCondition,
                String recommendation, BigDecimal confidence, List<String> insights) {
            this.current = current;
            this.history = new ArrayList<>(history);
            this.volatilityInfo = volatilityInfo;
            this.marketCondition = marketCondition;
            this.recommendation = recommendation;
            this.confidence = confidence;
            this.insights = new ArrayList<>(insights);
        }

        public BollingerBandsResult getCurrent() {
            return current;
        }

        public List<BollingerBandsResult> getHistory() {
            return new ArrayList<>(history);
        }

        public VolatilityInfo getVolatilityInfo() {
            return volatilityInfo;
        }

        public MarketCondition getMarketCondition() {
            return marketCondition;
        }

        public String getRecommendation() {
            return recommendation;
        }

        public BigDecimal getConfidence() {
            return confidence;
        }

        public List<String> getInsights() {
            return new ArrayList<>(insights);
        }

        @Override
        public String toString() {
            return String.format(
                    "BollingerBandsAnalysis{Current=%s, Volatility=%s, Market=%s, Recommendation='%s', Confidence=%.1f%%}",
                    current, volatilityInfo, marketCondition, recommendation, confidence);
        }
    }

    /**
     * Represents overall market condition based on Bollinger Bands analysis
     */
    public static class MarketCondition {
        private final String phase; // ACCUMULATION, TRENDING, DISTRIBUTION, RANGING
        private final String direction; // BULLISH, BEARISH, NEUTRAL
        private final int strength; // 1-5 scale
        private final boolean isOverbought;
        private final boolean isOversold;
        private final boolean isBreakout;

        public MarketCondition(String phase, String direction, int strength,
                boolean isOverbought, boolean isOversold, boolean isBreakout) {
            this.phase = phase;
            this.direction = direction;
            this.strength = strength;
            this.isOverbought = isOverbought;
            this.isOversold = isOversold;
            this.isBreakout = isBreakout;
        }

        public String getPhase() {
            return phase;
        }

        public String getDirection() {
            return direction;
        }

        public int getStrength() {
            return strength;
        }

        public boolean isOverbought() {
            return isOverbought;
        }

        public boolean isOversold() {
            return isOversold;
        }

        public boolean isBreakout() {
            return isBreakout;
        }

        @Override
        public String toString() {
            return String.format(
                    "MarketCondition{Phase=%s, Direction=%s, Strength=%d, Overbought=%s, Oversold=%s, Breakout=%s}",
                    phase, direction, strength, isOverbought, isOversold, isBreakout);
        }
    }

    /**
     * Main entry point for complete Bollinger Bands analysis
     * 
     * @param priceData                   List of price data points (should contain
     *                                    at least 30 data points for reliable
     *                                    analysis)
     * @param period                      Period for moving average calculation
     *                                    (default: 20)
     * @param standardDeviationMultiplier Multiplier for standard deviation bands
     *                                    (default: 2.0)
     * @return Complete analysis result
     */
    public static BollingerBandsAnalysis performCompleteAnalysis(List<PricePoint> priceData,
            int period, BigDecimal standardDeviationMultiplier) {
        if (priceData.size() < period + 10) {
            throw new IllegalArgumentException("Insufficient data points for analysis. Need at least " +
                    (period + 10) + " data points");
        }

        // Calculate Bollinger Bands for all available data
        List<BollingerBandsResult> results = calculateBollingerBands(priceData, period, standardDeviationMultiplier);

        if (results.isEmpty()) {
            throw new IllegalStateException("Unable to calculate Bollinger Bands");
        }

        BollingerBandsResult current = results.get(results.size() - 1);
        VolatilityInfo volatilityInfo = analyzeVolatility(results);
        MarketCondition marketCondition = analyzeMarketCondition(current, results);
        List<String> insights = generateInsights(current, volatilityInfo, marketCondition, results);
        String recommendation = generateRecommendation(current, volatilityInfo, marketCondition, insights);
        BigDecimal confidence = calculateConfidence(current, volatilityInfo, marketCondition, results);

        return new BollingerBandsAnalysis(current, results, volatilityInfo, marketCondition,
                recommendation, confidence, insights);
    }

    /**
     * Overloaded method with default parameters
     */
    public static BollingerBandsAnalysis performCompleteAnalysis(List<PricePoint> priceData) {
        return performCompleteAnalysis(priceData, DEFAULT_PERIOD, DEFAULT_STANDARD_DEVIATION_MULTIPLIER);
    }

    /**
     * Calculate Bollinger Bands for the entire dataset
     */
    private static List<BollingerBandsResult> calculateBollingerBands(List<PricePoint> priceData,
            int period, BigDecimal standardDeviationMultiplier) {
        List<BollingerBandsResult> results = new ArrayList<>();

        for (int i = period - 1; i < priceData.size(); i++) {
            List<PricePoint> subset = priceData.subList(i - period + 1, i + 1);

            BigDecimal movingAverage = calculateSimpleMovingAverage(subset);
            BigDecimal standardDeviation = calculateStandardDeviation(subset, movingAverage);

            BigDecimal upperBand = movingAverage.add(standardDeviation.multiply(standardDeviationMultiplier));
            BigDecimal lowerBand = movingAverage.subtract(standardDeviation.multiply(standardDeviationMultiplier));

            PricePoint currentPoint = priceData.get(i);
            BollingerSignal signal = determineSignal(currentPoint, upperBand, movingAverage, lowerBand, results);

            results.add(new BollingerBandsResult(upperBand, movingAverage, lowerBand,
                    currentPoint.getPrice(), signal, currentPoint.getTimestamp()));
        }

        return results;
    }

    /**
     * Calculate Simple Moving Average
     */
    private static BigDecimal calculateSimpleMovingAverage(List<PricePoint> priceData) {
        BigDecimal sum = priceData.stream()
                .map(PricePoint::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(priceData.size()), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Standard Deviation
     */
    private static BigDecimal calculateStandardDeviation(List<PricePoint> priceData, BigDecimal mean) {
        BigDecimal sumSquaredDifferences = BigDecimal.ZERO;

        for (PricePoint point : priceData) {
            BigDecimal difference = point.getPrice().subtract(mean);
            sumSquaredDifferences = sumSquaredDifferences.add(difference.multiply(difference));
        }

        BigDecimal variance = sumSquaredDifferences.divide(BigDecimal.valueOf(priceData.size()), SCALE,
                RoundingMode.HALF_UP);

        // Calculate square root using Newton's method
        return sqrt(variance);
    }

    /**
     * Calculate square root using Newton's method
     */
    private static BigDecimal sqrt(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal x = value;
        BigDecimal lastX = BigDecimal.ZERO;

        for (int i = 0; i < 50 && !x.equals(lastX); i++) {
            lastX = x;
            x = x.add(value.divide(x, SCALE, RoundingMode.HALF_UP)).divide(new BigDecimal("2"), SCALE,
                    RoundingMode.HALF_UP);
        }

        return x;
    }

    /**
     * Determine the current signal based on price position relative to bands
     */
    private static BollingerSignal determineSignal(PricePoint currentPoint, BigDecimal upperBand,
            BigDecimal middleBand, BigDecimal lowerBand,
            List<BollingerBandsResult> history) {
        BigDecimal price = currentPoint.getPrice();
        BigDecimal volume = currentPoint.getVolume();

        // Check for band touches/breaks
        boolean touchingUpperBand = price.compareTo(upperBand.multiply(new BigDecimal("0.98"))) >= 0;
        boolean touchingLowerBand = price.compareTo(lowerBand.multiply(new BigDecimal("1.02"))) <= 0;
        boolean breakingUpperBand = price.compareTo(upperBand) > 0;
        boolean breakingLowerBand = price.compareTo(lowerBand) < 0;

        // Analyze volume if available
        boolean highVolume = volume.compareTo(BigDecimal.ZERO) > 0; // Simplified - you might want to compare to average
                                                                    // volume

        // Check for squeeze/expansion
        if (!history.isEmpty()) {
            BollingerBandsResult previous = history.get(history.size() - 1);
            BigDecimal currentWidth = upperBand.subtract(lowerBand);
            BigDecimal previousWidth = previous.getUpperBand().subtract(previous.getLowerBand());

            boolean isSqueeze = currentWidth.compareTo(previousWidth.multiply(new BigDecimal("0.95"))) < 0;
            boolean isExpansion = currentWidth.compareTo(previousWidth.multiply(new BigDecimal("1.05"))) > 0;

            if (isSqueeze) {
                return BollingerSignal.SQUEEZE;
            } else if (isExpansion) {
                return BollingerSignal.EXPANSION;
            }
        }

        // Determine primary signals
        if (breakingLowerBand && highVolume) {
            return BollingerSignal.STRONG_BUY;
        } else if (breakingUpperBand && highVolume) {
            return BollingerSignal.STRONG_SELL;
        } else if (touchingLowerBand) {
            // Check if it's a bounce (reversal)
            if (!history.isEmpty()) {
                BollingerBandsResult previous = history.get(history.size() - 1);
                if (previous.getCurrentPrice().compareTo(lowerBand) < 0 && price.compareTo(lowerBand) > 0) {
                    return BollingerSignal.REVERSAL_UP;
                }
            }
            return BollingerSignal.BUY;
        } else if (touchingUpperBand) {
            // Check if it's a bounce (reversal)
            if (!history.isEmpty()) {
                BollingerBandsResult previous = history.get(history.size() - 1);
                if (previous.getCurrentPrice().compareTo(upperBand) > 0 && price.compareTo(upperBand) < 0) {
                    return BollingerSignal.REVERSAL_DOWN;
                }
            }
            return BollingerSignal.SELL;
        } else {
            return BollingerSignal.NEUTRAL;
        }
    }

    /**
     * Analyze volatility based on band width history
     */
    private static VolatilityInfo analyzeVolatility(List<BollingerBandsResult> results) {
        if (results.size() < 10) {
            return new VolatilityInfo("NORMAL", new BigDecimal("50"), false, false, "STABLE");
        }

        // Get recent band widths
        List<BigDecimal> recentWidths = new ArrayList<>();
        for (int i = Math.max(0, results.size() - 20); i < results.size(); i++) {
            recentWidths.add(results.get(i).getBandWidth());
        }

        BigDecimal currentWidth = results.get(results.size() - 1).getBandWidth();
        BigDecimal avgWidth = recentWidths.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(recentWidths.size()), SCALE, RoundingMode.HALF_UP);

        // Calculate percentile
        long belowCount = recentWidths.stream()
                .mapToLong(width -> width.compareTo(currentWidth) < 0 ? 1 : 0)
                .sum();
        BigDecimal percentile = BigDecimal.valueOf(belowCount)
                .divide(BigDecimal.valueOf(recentWidths.size()), SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        // Determine volatility level
        String volatilityLevel;
        if (percentile.compareTo(new BigDecimal("10")) < 0) {
            volatilityLevel = "LOW";
        } else if (percentile.compareTo(new BigDecimal("90")) > 0) {
            volatilityLevel = "EXTREME";
        } else if (percentile.compareTo(new BigDecimal("70")) > 0) {
            volatilityLevel = "HIGH";
        } else {
            volatilityLevel = "NORMAL";
        }

        // Check for squeeze/expansion
        boolean isSqueeze = "LOW".equals(volatilityLevel) &&
                currentWidth.compareTo(avgWidth.multiply(new BigDecimal("0.8"))) < 0;
        boolean isExpansion = currentWidth.compareTo(avgWidth.multiply(new BigDecimal("1.2"))) > 0;

        // Determine trend
        String trend = "STABLE";
        if (results.size() >= 5) {
            List<BollingerBandsResult> recent = results.subList(results.size() - 5, results.size());
            boolean increasing = true;
            boolean decreasing = true;

            for (int i = 1; i < recent.size(); i++) {
                if (recent.get(i).getBandWidth().compareTo(recent.get(i - 1).getBandWidth()) <= 0) {
                    increasing = false;
                }
                if (recent.get(i).getBandWidth().compareTo(recent.get(i - 1).getBandWidth()) >= 0) {
                    decreasing = false;
                }
            }

            if (increasing)
                trend = "EXPANDING";
            else if (decreasing)
                trend = "CONTRACTING";
        }

        return new VolatilityInfo(volatilityLevel, percentile, isSqueeze, isExpansion, trend);
    }

    /**
     * Analyze overall market condition
     */
    private static MarketCondition analyzeMarketCondition(BollingerBandsResult current,
            List<BollingerBandsResult> results) {
        String phase = "RANGING";
        String direction = "NEUTRAL";
        int strength = 1;
        boolean isOverbought = false;
        boolean isOversold = false;
        boolean isBreakout = false;

        BigDecimal percentB = current.getPercentB();

        // Determine overbought/oversold
        isOverbought = percentB.compareTo(new BigDecimal("0.8")) > 0;
        isOversold = percentB.compareTo(new BigDecimal("0.2")) < 0;

        // Check for breakouts
        isBreakout = percentB.compareTo(BigDecimal.ONE) > 0 || percentB.compareTo(BigDecimal.ZERO) < 0;

        // Analyze recent price action relative to middle band
        if (results.size() >= 10) {
            List<BollingerBandsResult> recent = results.subList(results.size() - 10, results.size());

            int aboveMiddle = 0;
            int belowMiddle = 0;

            for (BollingerBandsResult result : recent) {
                if (result.getCurrentPrice().compareTo(result.getMiddleBand()) > 0) {
                    aboveMiddle++;
                } else {
                    belowMiddle++;
                }
            }

            if (aboveMiddle > 7) {
                direction = "BULLISH";
                strength = Math.min(5, aboveMiddle - 5);
                phase = "TRENDING";
            } else if (belowMiddle > 7) {
                direction = "BEARISH";
                strength = Math.min(5, belowMiddle - 5);
                phase = "TRENDING";
            }
        }

        // Determine market phase based on volatility and price action
        if (current.getBandWidth().compareTo(new BigDecimal("2")) < 0) {
            phase = "ACCUMULATION";
        } else if (isBreakout) {
            phase = "DISTRIBUTION";
        }

        return new MarketCondition(phase, direction, strength, isOverbought, isOversold, isBreakout);
    }

    /**
     * Generate insights based on analysis
     */
    private static List<String> generateInsights(BollingerBandsResult current, VolatilityInfo volatilityInfo,
            MarketCondition marketCondition, List<BollingerBandsResult> history) {
        List<String> insights = new ArrayList<>();

        // Volatility insights
        if (volatilityInfo.isSqueeze()) {
            insights.add("Bollinger Band squeeze detected - expect increased volatility soon");
        }

        if (volatilityInfo.isExpansion()) {
            insights.add("Bollinger Bands expanding - high volatility period active");
        }

        // Position insights
        BigDecimal percentB = current.getPercentB();
        if (percentB.compareTo(new BigDecimal("0.9")) > 0) {
            insights.add("Price near upper band - potential resistance level");
        } else if (percentB.compareTo(new BigDecimal("0.1")) < 0) {
            insights.add("Price near lower band - potential support level");
        }

        // Breakout insights
        if (marketCondition.isBreakout()) {
            insights.add("Price breakout detected - monitor for continuation or false breakout");
        }

        // Trend insights
        if ("TRENDING".equals(marketCondition.getPhase())) {
            insights.add("Market in trending phase - " + marketCondition.getDirection().toLowerCase() + " bias");
        } else if ("ACCUMULATION".equals(marketCondition.getPhase())) {
            insights.add("Market in accumulation phase - low volatility, potential breakout building");
        }

        // Mean reversion insights
        if (current.getSignal() == BollingerSignal.REVERSAL_UP
                || current.getSignal() == BollingerSignal.REVERSAL_DOWN) {
            insights.add("Mean reversion signal detected - price bouncing off band");
        }

        return insights;
    }

    /**
     * Generate trading recommendation
     */
    private static String generateRecommendation(BollingerBandsResult current, VolatilityInfo volatilityInfo,
            MarketCondition marketCondition, List<String> insights) {
        StringBuilder recommendation = new StringBuilder();

        switch (current.getSignal()) {
            case STRONG_BUY:
                recommendation.append("STRONG BUY - Price breaking below lower band with volume. ");
                recommendation.append("Consider entering long position with tight stop loss.");
                break;
            case BUY:
                recommendation.append("BUY - Price near lower band. ");
                if (marketCondition.isOversold()) {
                    recommendation.append("Oversold condition supports potential bounce.");
                }
                break;
            case STRONG_SELL:
                recommendation.append("STRONG SELL - Price breaking above upper band with volume. ");
                recommendation.append("Consider exiting longs or entering short position.");
                break;
            case SELL:
                recommendation.append("SELL - Price near upper band. ");
                if (marketCondition.isOverbought()) {
                    recommendation.append("Overbought condition suggests potential pullback.");
                }
                break;
            case SQUEEZE:
                recommendation.append("PREPARE - Bollinger Band squeeze in progress. ");
                recommendation.append("Position for potential breakout in either direction.");
                break;
            case EXPANSION:
                recommendation.append("ACTIVE - High volatility period. ");
                recommendation.append("Trade with appropriate position sizing for increased risk.");
                break;
            case REVERSAL_UP:
                recommendation.append("BUY OPPORTUNITY - Price bouncing off lower band. ");
                recommendation.append("Mean reversion opportunity with defined risk.");
                break;
            case REVERSAL_DOWN:
                recommendation.append("SELL OPPORTUNITY - Price bouncing off upper band. ");
                recommendation.append("Mean reversion opportunity with defined risk.");
                break;
            case NEUTRAL:
            default:
                recommendation.append("WAIT - Price within normal range. ");
                if (volatilityInfo.isSqueeze()) {
                    recommendation.append("Monitor for breakout signals.");
                } else {
                    recommendation.append("Wait for clearer signals near band extremes.");
                }
                break;
        }

        return recommendation.toString();
    }

    /**
     * Calculate confidence level for the analysis (0-100%)
     */
    private static BigDecimal calculateConfidence(BollingerBandsResult current, VolatilityInfo volatilityInfo,
            MarketCondition marketCondition, List<BollingerBandsResult> history) {
        BigDecimal baseConfidence = new BigDecimal("50");

        // Increase confidence for strong signals
        if (current.getSignal() == BollingerSignal.STRONG_BUY ||
                current.getSignal() == BollingerSignal.STRONG_SELL) {
            baseConfidence = baseConfidence.add(new BigDecimal("25"));
        }

        // Increase confidence for reversal signals
        if (current.getSignal() == BollingerSignal.REVERSAL_UP ||
                current.getSignal() == BollingerSignal.REVERSAL_DOWN) {
            baseConfidence = baseConfidence.add(new BigDecimal("20"));
        }

        // Increase confidence for extreme readings
        BigDecimal percentB = current.getPercentB();
        if (percentB.compareTo(new BigDecimal("0.9")) > 0 || percentB.compareTo(new BigDecimal("0.1")) < 0) {
            baseConfidence = baseConfidence.add(new BigDecimal("15"));
        }

        // Increase confidence for clear trends
        if (marketCondition.getStrength() >= 4) {
            baseConfidence = baseConfidence.add(new BigDecimal("10"));
        }

        // Decrease confidence for neutral signals
        if (current.getSignal() == BollingerSignal.NEUTRAL) {
            baseConfidence = baseConfidence.subtract(new BigDecimal("15"));
        }

        // Decrease confidence during squeeze periods (uncertainty)
        if (volatilityInfo.isSqueeze()) {
            baseConfidence = baseConfidence.subtract(new BigDecimal("10"));
        }

        // Ensure confidence is within 0-100% range
        return baseConfidence.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * Helper method to create price point from trade data
     */
    public static PricePoint createPricePoint(BigDecimal price, LocalDateTime timestamp) {
        return new PricePoint(price, BigDecimal.ZERO, timestamp);
    }

    /**
     * Helper method to create price point with volume
     */
    public static PricePoint createPricePoint(BigDecimal price, BigDecimal volume, LocalDateTime timestamp) {
        return new PricePoint(price, volume, timestamp);
    }
}
