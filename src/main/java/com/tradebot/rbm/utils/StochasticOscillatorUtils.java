package com.tradebot.rbm.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tradebot.rbm.utils.dto.stochasticOscilator.PriceData;
import com.tradebot.rbm.utils.dto.stochasticOscilator.StochasticAnalysis;
import com.tradebot.rbm.utils.dto.stochasticOscilator.StochasticResult;
import com.tradebot.rbm.utils.dto.stochasticOscilator.StochasticSignal;
import com.tradebot.rbm.utils.dto.stochasticOscilator.StochasticTrend;

/**
 * Utility class for calculating and analyzing Stochastic Oscillator
 * The Stochastic Oscillator is a momentum indicator that compares a closing
 * price
 * to its price range over a specific period of time.
 */
public class StochasticOscillatorUtils {

    private static final int DEFAULT_K_PERIOD = 9;
    private static final int DEFAULT_D_PERIOD = 3;
    private static final BigDecimal OVERBOUGHT_THRESHOLD = new BigDecimal("80");
    private static final BigDecimal OVERSOLD_THRESHOLD = new BigDecimal("20");
    private static final int SCALE = 8;

    /**
     * Main entry point for complete Stochastic Oscillator analysis
     * 
     * @param priceData List of price data points (should contain at least 20 data
     *                  points for reliable analysis)
     * @param kPeriod   Period for %K calculation (default: 14)
     * @param dPeriod   Period for %D calculation (default: 3)
     * @return Complete analysis result
     */
    public static StochasticAnalysis performCompleteAnalysis(List<PriceData> priceData,
            int kPeriod, int dPeriod) {
        if (priceData.size() < Math.max(kPeriod, dPeriod) + 5) {
            throw new IllegalArgumentException("Insufficient data points for analysis. Need at least " +
                    (Math.max(kPeriod, dPeriod) + 5) + " data points");
        }

        // Calculate stochastic values for all available data
        List<StochasticResult> results = calculateStochasticValues(priceData, kPeriod, dPeriod);

        if (results.isEmpty()) {
            throw new IllegalStateException("Unable to calculate stochastic values");
        }

        StochasticResult current = results.get(results.size() - 1);
        StochasticTrend trend = analyzeTrend(results);
        String recommendation = generateRecommendation(current, trend, results);
        BigDecimal confidence = calculateConfidence(current, trend, results);

        return new StochasticAnalysis(current, results, trend, recommendation, confidence);
    }

    /**
     * Overloaded method with default periods
     */
    public static StochasticAnalysis performCompleteAnalysis(List<PriceData> priceData) {
        return performCompleteAnalysis(priceData, DEFAULT_K_PERIOD, DEFAULT_D_PERIOD);
    }

    /**
     * Calculate %K and %D values for the entire dataset
     */
    private static List<StochasticResult> calculateStochasticValues(List<PriceData> priceData,
            int kPeriod, int dPeriod) {
        List<StochasticResult> results = new ArrayList<>();
        List<BigDecimal> kValues = new ArrayList<>();

        for (int i = kPeriod - 1; i < priceData.size(); i++) {
            // Get the subset for %K calculation
            List<PriceData> subset = priceData.subList(i - kPeriod + 1, i + 1);

            BigDecimal kPercent = calculateKPercent(subset, priceData.get(i).getClose());
            kValues.add(kPercent);

            // Calculate %D only if we have enough %K values
            if (kValues.size() >= dPeriod) {
                List<BigDecimal> dSubset = kValues.subList(kValues.size() - dPeriod, kValues.size());
                BigDecimal dPercent = calculateMovingAverage(dSubset);

                StochasticSignal signal = determineSignal(kPercent, dPercent, results);
                results.add(new StochasticResult(kPercent, dPercent, signal, priceData.get(i).getTimestamp()));
            }
        }

        return results;
    }

    /**
     * Calculate %K = 100 * (Close - Lowest Low) / (Highest High - Lowest Low)
     */
    private static BigDecimal calculateKPercent(List<PriceData> priceData, BigDecimal currentClose) {
        BigDecimal highestHigh = priceData.stream()
                .map(PriceData::getHigh)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal lowestLow = priceData.stream()
                .map(PriceData::getLow)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal range = highestHigh.subtract(lowestLow);

        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("50"); // Neutral value when no range
        }

        BigDecimal numerator = currentClose.subtract(lowestLow);
        return numerator.divide(range, SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Calculate moving average for %D
     */
    private static BigDecimal calculateMovingAverage(List<BigDecimal> values) {
        BigDecimal sum = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(values.size()), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Determine the current signal based on %K, %D, and historical data
     */
    private static StochasticSignal determineSignal(BigDecimal kPercent, BigDecimal dPercent,
            List<StochasticResult> history) {
        boolean isOverbought = kPercent.compareTo(OVERBOUGHT_THRESHOLD) > 0;
        boolean isOversold = kPercent.compareTo(OVERSOLD_THRESHOLD) < 0;

        // Check for crossovers if we have previous data
        if (!history.isEmpty()) {
            StochasticResult previous = history.get(history.size() - 1);
            boolean wasKBelowD = previous.getKPercent().compareTo(previous.getDPercent()) < 0;
            boolean isKAboveD = kPercent.compareTo(dPercent) > 0;

            // Bullish crossover in oversold territory
            if (wasKBelowD && isKAboveD && isOversold) {
                return StochasticSignal.STRONG_BUY;
            }

            // Bearish crossover in overbought territory
            if (!wasKBelowD && !isKAboveD && isOverbought) {
                return StochasticSignal.STRONG_SELL;
            }
        }

        // Standard signals
        if (isOversold) {
            return StochasticSignal.BUY;
        } else if (isOverbought) {
            return StochasticSignal.SELL;
        } else {
            return StochasticSignal.NEUTRAL;
        }
    }

    /**
     * Analyze trend based on recent stochastic results
     */
    private static StochasticTrend analyzeTrend(List<StochasticResult> results) {
        if (results.size() < 3) {
            return new StochasticTrend("NEUTRAL", 1, false, false, false);
        }

        StochasticResult current = results.get(results.size() - 1);
        List<StochasticResult> recent = results.subList(Math.max(0, results.size() - 5), results.size());

        // Determine trend direction
        String direction = "NEUTRAL";
        int upCount = 0;
        int downCount = 0;

        for (int i = 1; i < recent.size(); i++) {
            BigDecimal currentK = recent.get(i).getKPercent();
            BigDecimal previousK = recent.get(i - 1).getKPercent();

            if (currentK.compareTo(previousK) > 0) {
                upCount++;
            } else if (currentK.compareTo(previousK) < 0) {
                downCount++;
            }
        }

        if (upCount > downCount) {
            direction = "BULLISH";
        } else if (downCount > upCount) {
            direction = "BEARISH";
        }

        // Calculate strength (1-5 scale)
        int strength = Math.min(5, Math.max(1, Math.abs(upCount - downCount) + 1));

        // Check conditions
        boolean isOverbought = current.getKPercent().compareTo(OVERBOUGHT_THRESHOLD) > 0;
        boolean isOversold = current.getKPercent().compareTo(OVERSOLD_THRESHOLD) < 0;

        // Check for recent crossover
        boolean hasRecentCrossover = false;
        if (results.size() >= 2) {
            StochasticResult previous = results.get(results.size() - 2);
            boolean wasKBelowD = previous.getKPercent().compareTo(previous.getDPercent()) < 0;
            boolean isKAboveD = current.getKPercent().compareTo(current.getDPercent()) > 0;
            hasRecentCrossover = wasKBelowD != isKAboveD;
        }

        return new StochasticTrend(direction, strength, isOverbought, isOversold, hasRecentCrossover);
    }

    /**
     * Generate trading recommendation based on analysis
     */
    private static String generateRecommendation(StochasticResult current, StochasticTrend trend,
            List<StochasticResult> history) {
        StringBuilder recommendation = new StringBuilder();

        switch (current.getSignal()) {
            case STRONG_BUY:
                recommendation.append("STRONG BUY - Bullish crossover in oversold territory. ");
                recommendation.append("Consider entering long position.");
                break;
            case BUY:
                recommendation.append("BUY - Oversold condition detected. ");
                if (trend.getDirection().equals("BULLISH")) {
                    recommendation.append("Trend supports potential reversal.");
                } else {
                    recommendation.append("Wait for confirmation before entering.");
                }
                break;
            case STRONG_SELL:
                recommendation.append("STRONG SELL - Bearish crossover in overbought territory. ");
                recommendation.append("Consider exiting long positions or entering short.");
                break;
            case SELL:
                recommendation.append("SELL - Overbought condition detected. ");
                if (trend.getDirection().equals("BEARISH")) {
                    recommendation.append("Trend supports potential reversal.");
                } else {
                    recommendation.append("Monitor for reversal signals.");
                }
                break;
            case NEUTRAL:
            default:
                recommendation.append("NEUTRAL - No clear signal. ");
                if (trend.isHasRecentCrossover()) {
                    recommendation.append("Recent crossover detected, monitor closely.");
                } else {
                    recommendation.append("Wait for clearer signals.");
                }
                break;
        }

        return recommendation.toString();
    }

    /**
     * Calculate confidence level for the analysis (0-100%)
     */
    private static BigDecimal calculateConfidence(StochasticResult current, StochasticTrend trend,
            List<StochasticResult> history) {
        BigDecimal baseConfidence = new BigDecimal("50");

        // Increase confidence for strong signals
        if (current.getSignal() == StochasticSignal.STRONG_BUY ||
                current.getSignal() == StochasticSignal.STRONG_SELL) {
            baseConfidence = baseConfidence.add(new BigDecimal("30"));
        }

        // Increase confidence for clear trends
        if (trend.getStrength() >= 4) {
            baseConfidence = baseConfidence.add(new BigDecimal("15"));
        }

        // Increase confidence for extreme readings
        if (trend.isOverbought() || trend.isOversold()) {
            baseConfidence = baseConfidence.add(new BigDecimal("10"));
        }

        // Decrease confidence for neutral signals
        if (current.getSignal() == StochasticSignal.NEUTRAL) {
            baseConfidence = baseConfidence.subtract(new BigDecimal("20"));
        }

        // Ensure confidence is within 0-100% range
        return baseConfidence.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * Helper method to convert trade data to price data
     */
    public static PriceData createPriceDataFromTrade(BigDecimal price, LocalDateTime timestamp) {
        // For single trade data, use the price as high, low, and close
        return new PriceData(price, price, price, timestamp);
    }

    /**
     * Helper method to create price data with OHLC values
     */
    public static PriceData createPriceData(BigDecimal high, BigDecimal low, BigDecimal close,
            LocalDateTime timestamp) {
        return new PriceData(high, low, close, timestamp);
    }
}
