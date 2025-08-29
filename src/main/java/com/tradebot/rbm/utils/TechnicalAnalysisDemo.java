package com.tradebot.rbm.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.binance.connector.client.spot.websocket.stream.model.TradeResponse;
import com.tradebot.rbm.utils.BollingerBandsUtils.BollingerBandsAnalysis;
import com.tradebot.rbm.utils.BollingerBandsUtils.PricePoint;
import com.tradebot.rbm.utils.dto.stochasticOscilator.PriceData;
import com.tradebot.rbm.utils.dto.stochasticOscilator.StochasticAnalysis;

/**
 * Demo class showing how to use the technical analysis utilities
 * with your existing trade data from WebsocketTradeService
 */
public class TechnicalAnalysisDemo {

    /**
     * Example of how to use Stochastic Oscillator analysis with your trade data
     * This method shows integration with your existing
     * ConcurrentLinkedQueue<TradeData>
     */
    public static void demonstrateStochasticAnalysis(List<PriceData> priceDataList) {
        // Example: Convert your existing trade data to price data for analysis

        try {
            // Perform complete Stochastic Oscillator analysis
            StochasticAnalysis stochasticAnalysis = StochasticOscillatorUtils.performCompleteAnalysis(priceDataList);

            System.out.println("=== STOCHASTIC OSCILLATOR ANALYSIS ===");
            System.out.println("Current Result: " + stochasticAnalysis.getCurrent());
            System.out.println("Trend: " + stochasticAnalysis.getTrend());
            System.out.println("Recommendation: " + stochasticAnalysis.getRecommendation());
            System.out.println("Confidence: " + stochasticAnalysis.getConfidence() + "%");

            // Example of making trading decisions based on the analysis
            makeStochasticTradingDecision(stochasticAnalysis);

        } catch (Exception e) {
            System.err.println("Error in Stochastic analysis: " + e.getMessage());
        }
    }

    /**
     * Example of how to use Bollinger Bands analysis with your trade data
     */
    public static void demonstrateBollingerBandsAnalysis() {
        // Example: Convert your existing trade data to price points for analysis
        List<PricePoint> pricePointsList = createSamplePricePoints();

        try {
            // Perform complete Bollinger Bands analysis
            BollingerBandsAnalysis bollingerAnalysis = BollingerBandsUtils.performCompleteAnalysis(pricePointsList);

            System.out.println("\n=== BOLLINGER BANDS ANALYSIS ===");
            System.out.println("Current Result: " + bollingerAnalysis.getCurrent());
            System.out.println("Volatility Info: " + bollingerAnalysis.getVolatilityInfo());
            System.out.println("Market Condition: " + bollingerAnalysis.getMarketCondition());
            System.out.println("Recommendation: " + bollingerAnalysis.getRecommendation());
            System.out.println("Confidence: " + bollingerAnalysis.getConfidence() + "%");
            System.out.println("Insights:");
            bollingerAnalysis.getInsights().forEach(insight -> System.out.println("  - " + insight));

            // Example of making trading decisions based on the analysis
            makeBollingerTradingDecision(bollingerAnalysis);

        } catch (Exception e) {
            System.err.println("Error in Bollinger Bands analysis: " + e.getMessage());
        }
    }

    /**
     * Combined analysis using both indicators for more informed decisions
     */
    public static CombinedAnalysisResult performCombinedAnalysis(List<TradeResponse> recentTrades) {
        // Convert TradeResponse to analysis-ready data
        List<PriceData> stochasticData = convertTradesToPriceData(recentTrades);
        List<PricePoint> bollingerData = convertTradesToPricePoints(recentTrades);

        try {
            StochasticAnalysis stochasticAnalysis = StochasticOscillatorUtils.performCompleteAnalysis(stochasticData);
            BollingerBandsAnalysis bollingerAnalysis = BollingerBandsUtils.performCompleteAnalysis(bollingerData);

            return new CombinedAnalysisResult(stochasticAnalysis, bollingerAnalysis);

        } catch (Exception e) {
            System.err.println("Error in combined analysis: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convert your TradeResponse objects to PriceData for Stochastic analysis
     */
    private static List<PriceData> convertTradesToPriceData(List<TradeResponse> trades) {
        List<PriceData> priceDataList = new ArrayList<>();

        for (TradeResponse trade : trades) {
            BigDecimal price = new BigDecimal(trade.getpLowerCase());
            LocalDateTime timestamp = LocalDateTime.now(); // You might want to convert trade timestamp

            // For single trades, use the price as high, low, and close
            PriceData priceData = StochasticOscillatorUtils.createPriceDataFromTrade(price, timestamp);
            priceDataList.add(priceData);
        }

        return priceDataList;
    }

    /**
     * Convert your TradeResponse objects to PricePoint for Bollinger Bands analysis
     */
    private static List<PricePoint> convertTradesToPricePoints(List<TradeResponse> trades) {
        List<PricePoint> pricePointsList = new ArrayList<>();

        for (TradeResponse trade : trades) {
            BigDecimal price = new BigDecimal(trade.getpLowerCase());
            BigDecimal quantity = new BigDecimal(trade.getqLowerCase());
            LocalDateTime timestamp = LocalDateTime.now(); // You might want to convert trade timestamp

            PricePoint pricePoint = BollingerBandsUtils.createPricePoint(price, quantity, timestamp);
            pricePointsList.add(pricePoint);
        }

        return pricePointsList;
    }

    /**
     * Example trading decision logic based on Stochastic analysis
     */
    private static void makeStochasticTradingDecision(StochasticAnalysis analysis) {
        System.out.println("\n--- Stochastic Trading Decision ---");

        switch (analysis.getCurrent().getSignal()) {
            case STRONG_BUY:
                System.out.println("ACTION: Consider STRONG BUY position");
                System.out.println("REASON: Bullish crossover in oversold territory");
                break;
            case BUY:
                System.out.println("ACTION: Consider BUY position");
                System.out.println("REASON: Oversold condition detected");
                break;
            case STRONG_SELL:
                System.out.println("ACTION: Consider STRONG SELL position");
                System.out.println("REASON: Bearish crossover in overbought territory");
                break;
            case SELL:
                System.out.println("ACTION: Consider SELL position");
                System.out.println("REASON: Overbought condition detected");
                break;
            case NEUTRAL:
            default:
                System.out.println("ACTION: WAIT");
                System.out.println("REASON: No clear signal, wait for better entry");
                break;
        }
    }

    /**
     * Example trading decision logic based on Bollinger Bands analysis
     */
    private static void makeBollingerTradingDecision(BollingerBandsAnalysis analysis) {
        System.out.println("\n--- Bollinger Bands Trading Decision ---");

        switch (analysis.getCurrent().getSignal()) {
            case STRONG_BUY:
                System.out.println("ACTION: Consider STRONG BUY position");
                System.out.println("REASON: Price breaking below lower band with volume");
                break;
            case REVERSAL_UP:
                System.out.println("ACTION: Consider BUY position");
                System.out.println("REASON: Mean reversion opportunity - price bouncing off lower band");
                break;
            case STRONG_SELL:
                System.out.println("ACTION: Consider STRONG SELL position");
                System.out.println("REASON: Price breaking above upper band with volume");
                break;
            case REVERSAL_DOWN:
                System.out.println("ACTION: Consider SELL position");
                System.out.println("REASON: Mean reversion opportunity - price bouncing off upper band");
                break;
            case SQUEEZE:
                System.out.println("ACTION: PREPARE for breakout");
                System.out.println("REASON: Bollinger Band squeeze - volatility expansion expected");
                break;
            case EXPANSION:
                System.out.println("ACTION: Use smaller position sizes");
                System.out.println("REASON: High volatility period - increased risk");
                break;
            case NEUTRAL:
            default:
                System.out.println("ACTION: WAIT");
                System.out.println("REASON: Price within normal range, wait for band extreme signals");
                break;
        }
    }

    /**
     * Create sample price data for demonstration (you would replace this with your
     * actual data)
     */
    private static List<PriceData> createSamplePriceData() {
        List<PriceData> priceData = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusHours(24);

        // Sample BTCUSDT-like prices for demonstration
        BigDecimal[] samplePrices = {
                new BigDecimal("45000"), new BigDecimal("45100"), new BigDecimal("44900"), new BigDecimal("45200"),
                new BigDecimal("45150"), new BigDecimal("45300"), new BigDecimal("45250"), new BigDecimal("45400"),
                new BigDecimal("45350"), new BigDecimal("45500"), new BigDecimal("45450"), new BigDecimal("45600"),
                new BigDecimal("45550"), new BigDecimal("45700"), new BigDecimal("45650"), new BigDecimal("45800"),
                new BigDecimal("45750"), new BigDecimal("45900"), new BigDecimal("45850"), new BigDecimal("46000"),
                new BigDecimal("45950"), new BigDecimal("46100"), new BigDecimal("46050"), new BigDecimal("46200"),
                new BigDecimal("46150"), new BigDecimal("46300"), new BigDecimal("46250"), new BigDecimal("46400"),
                new BigDecimal("46350"), new BigDecimal("46500")
        };

        for (int i = 0; i < samplePrices.length; i++) {
            BigDecimal price = samplePrices[i];
            // Create simple OHLC where high = price + 50, low = price - 50, close = price
            BigDecimal high = price.add(new BigDecimal("50"));
            BigDecimal low = price.subtract(new BigDecimal("50"));
            LocalDateTime timestamp = baseTime.plusMinutes(i * 30);

            priceData.add(StochasticOscillatorUtils.createPriceData(high, low, price, timestamp));
        }

        return priceData;
    }

    /**
     * Create sample price points for demonstration (you would replace this with
     * your actual data)
     */
    private static List<PricePoint> createSamplePricePoints() {
        List<PricePoint> pricePoints = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusHours(24);

        // Sample BTCUSDT-like prices for demonstration
        BigDecimal[] samplePrices = {
                new BigDecimal("45000"), new BigDecimal("45100"), new BigDecimal("44900"), new BigDecimal("45200"),
                new BigDecimal("45150"), new BigDecimal("45300"), new BigDecimal("45250"), new BigDecimal("45400"),
                new BigDecimal("45350"), new BigDecimal("45500"), new BigDecimal("45450"), new BigDecimal("45600"),
                new BigDecimal("45550"), new BigDecimal("45700"), new BigDecimal("45650"), new BigDecimal("45800"),
                new BigDecimal("45750"), new BigDecimal("45900"), new BigDecimal("45850"), new BigDecimal("46000"),
                new BigDecimal("45950"), new BigDecimal("46100"), new BigDecimal("46050"), new BigDecimal("46200"),
                new BigDecimal("46150"), new BigDecimal("46300"), new BigDecimal("46250"), new BigDecimal("46400"),
                new BigDecimal("46350"), new BigDecimal("46500")
        };

        for (int i = 0; i < samplePrices.length; i++) {
            BigDecimal price = samplePrices[i];
            BigDecimal volume = new BigDecimal("1.5"); // Sample volume
            LocalDateTime timestamp = baseTime.plusMinutes(i * 30);

            pricePoints.add(BollingerBandsUtils.createPricePoint(price, volume, timestamp));
        }

        return pricePoints;
    }

    /**
     * Combined analysis result containing both Stochastic and Bollinger Bands
     * analysis
     */
    public static class CombinedAnalysisResult {
        private final StochasticAnalysis stochasticAnalysis;
        private final BollingerBandsAnalysis bollingerAnalysis;

        public CombinedAnalysisResult(StochasticAnalysis stochasticAnalysis, BollingerBandsAnalysis bollingerAnalysis) {
            this.stochasticAnalysis = stochasticAnalysis;
            this.bollingerAnalysis = bollingerAnalysis;
        }

        public StochasticAnalysis getStochasticAnalysis() {
            return stochasticAnalysis;
        }

        public BollingerBandsAnalysis getBollingerAnalysis() {
            return bollingerAnalysis;
        }

        /**
         * Generate combined recommendation based on both analyses
         */
        public String getCombinedRecommendation() {
            StringBuilder recommendation = new StringBuilder();

            boolean stochasticBullish = stochasticAnalysis.getCurrent().getSignal().name().contains("BUY");
            boolean stochasticBearish = stochasticAnalysis.getCurrent().getSignal().name().contains("SELL");
            boolean bollingerBullish = bollingerAnalysis.getCurrent().getSignal().name().contains("BUY") ||
                    bollingerAnalysis.getCurrent().getSignal() == BollingerBandsUtils.BollingerSignal.REVERSAL_UP;
            boolean bollingerBearish = bollingerAnalysis.getCurrent().getSignal().name().contains("SELL") ||
                    bollingerAnalysis.getCurrent().getSignal() == BollingerBandsUtils.BollingerSignal.REVERSAL_DOWN;

            if (stochasticBullish && bollingerBullish) {
                recommendation.append("STRONG BUY SIGNAL - Both indicators confirm bullish bias");
            } else if (stochasticBearish && bollingerBearish) {
                recommendation.append("STRONG SELL SIGNAL - Both indicators confirm bearish bias");
            } else if (stochasticBullish && !bollingerBearish) {
                recommendation.append("MODERATE BUY - Stochastic bullish, Bollinger neutral/supportive");
            } else if (stochasticBearish && !bollingerBullish) {
                recommendation.append("MODERATE SELL - Stochastic bearish, Bollinger neutral/supportive");
            } else if (stochasticBullish && bollingerBearish) {
                recommendation.append(
                        "CONFLICTING SIGNALS - Stochastic bullish but Bollinger bearish. Wait for confirmation.");
            } else if (stochasticBearish && bollingerBullish) {
                recommendation.append(
                        "CONFLICTING SIGNALS - Stochastic bearish but Bollinger bullish. Wait for confirmation.");
            } else {
                recommendation.append("NEUTRAL - Both indicators show neutral signals. Wait for clearer direction.");
            }

            return recommendation.toString();
        }

        /**
         * Calculate combined confidence based on agreement between indicators
         */
        public BigDecimal getCombinedConfidence() {
            BigDecimal stochasticConfidence = stochasticAnalysis.getConfidence();
            BigDecimal bollingerConfidence = bollingerAnalysis.getConfidence();

            // Base combined confidence on average
            BigDecimal avgConfidence = stochasticConfidence.add(bollingerConfidence)
                    .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);

            // Boost confidence if both agree, reduce if they conflict
            String stochasticSignal = stochasticAnalysis.getCurrent().getSignal().name();
            String bollingerSignal = bollingerAnalysis.getCurrent().getSignal().name();

            boolean bothBullish = stochasticSignal.contains("BUY") &&
                    (bollingerSignal.contains("BUY") || bollingerSignal.contains("REVERSAL_UP"));
            boolean bothBearish = stochasticSignal.contains("SELL") &&
                    (bollingerSignal.contains("SELL") || bollingerSignal.contains("REVERSAL_DOWN"));
            boolean conflicting = (stochasticSignal.contains("BUY") && bollingerSignal.contains("SELL")) ||
                    (stochasticSignal.contains("SELL") && bollingerSignal.contains("BUY"));

            if (bothBullish || bothBearish) {
                avgConfidence = avgConfidence.add(new BigDecimal("15")); // Boost for agreement
            } else if (conflicting) {
                avgConfidence = avgConfidence.subtract(new BigDecimal("20")); // Reduce for conflict
            }

            return avgConfidence.max(BigDecimal.ZERO).min(new BigDecimal("100"));
        }

        @Override
        public String toString() {
            return String.format("CombinedAnalysis{Recommendation='%s', Confidence=%.1f%%}",
                    getCombinedRecommendation(), getCombinedConfidence());
        }
    }

    /**
     * Main method for testing the utilities
     */
    public static void main(String[] args) {
        System.out.println("=== TECHNICAL ANALYSIS UTILITIES DEMO ===\n");

        // demonstrateStochasticAnalysis();
        demonstrateBollingerBandsAnalysis();

        System.out.println("\n=== COMBINED ANALYSIS DEMO ===");
        // Example with empty trades list - you would pass your actual recent trades
        List<TradeResponse> sampleTrades = new ArrayList<>(); // Replace with actual data
        if (!sampleTrades.isEmpty()) {
            CombinedAnalysisResult combinedResult = performCombinedAnalysis(sampleTrades);
            if (combinedResult != null) {
                System.out.println("Combined Result: " + combinedResult);
            }
        } else {
            System.out.println("No trade data available for combined analysis demo");
        }
    }
}
