package com.tradebot.rbm.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.binance.connector.client.spot.rest.model.TickerBookTickerResponse1;
import com.tradebot.rbm.entity.dto.TickerDto;
import com.tradebot.rbm.utils.dto.LiquidityAnalysis;
import com.tradebot.rbm.utils.dto.LiquidityAnalysis.LiquidityLevel;
import com.tradebot.rbm.utils.dto.MarketMomentum;
import com.tradebot.rbm.utils.dto.MarketMomentum.MomentumDirection;
import com.tradebot.rbm.utils.dto.ScalpingDecision;
import com.tradebot.rbm.utils.dto.ScalpingDecision.ScalpingSignal;
import com.tradebot.rbm.utils.dto.VolumeAnalysis;
import com.tradebot.rbm.utils.dto.VolumeAnalysis.MarketStrength;
import com.tradebot.rbm.utils.dto.VolumeAnalysis.VolumeSignal;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VolumeAnalysisUtils {

    // Configuration constants for volume analysis
    private static final BigDecimal HIGH_VOLUME_THRESHOLD = new BigDecimal("1.2"); // 120% of average
    private static final BigDecimal LOW_VOLUME_THRESHOLD = new BigDecimal("0.7"); // 70% of average
    private static final BigDecimal MAX_SPREAD_PERCENTAGE = new BigDecimal("0.1"); // 0.1% max spread
    private static final BigDecimal STRONG_PRICE_CHANGE = new BigDecimal("1.0"); // 1% price change
    private static final BigDecimal MODERATE_PRICE_CHANGE = new BigDecimal("0.4"); // 0.4% price change

    /**
     * Analyzes volume patterns between different timeframes
     * 
     * @param tickerBookTickerResponse1
     */
    public VolumeAnalysis analyzeVolume(TickerDto ticker6h, TickerDto ticker1h, TickerDto ticker24h,
            TickerBookTickerResponse1 book) {
        try {
            // Extract volume data
            BigDecimal volume6h = new BigDecimal(ticker6h.getTicker().getVolume());
            BigDecimal quoteVolume6h = new BigDecimal(ticker6h.getTicker().getQuoteVolume());
            BigDecimal volume1h = new BigDecimal(ticker1h.getTicker().getVolume());
            BigDecimal volume24h = new BigDecimal(ticker24h.getTicker().getVolume());

            // Calculate volume metrics
            BigDecimal avgVolumePerHour6h = volume6h.divide(new BigDecimal("6"), 8, RoundingMode.HALF_UP);
            BigDecimal volumeRatio = volume1h.divide(avgVolumePerHour6h, 8, RoundingMode.HALF_UP);

            // Price changes
            BigDecimal priceChange6h = new BigDecimal(ticker6h.getTicker().getPriceChangePercent());
            BigDecimal priceChange1h = new BigDecimal(ticker1h.getTicker().getPriceChangePercent());

            // Bid-ask spread
            BigDecimal bidPrice = new BigDecimal(book.getBidPrice());
            BigDecimal askPrice = new BigDecimal(book.getAskPrice());
            BigDecimal spread = askPrice.subtract(bidPrice);

            // Analysis
            boolean isVolumeIncreasing = volumeRatio.compareTo(HIGH_VOLUME_THRESHOLD) > 0;
            boolean isPriceVolumeAligned = isPriceVolumeAligned(priceChange1h, volumeRatio);
            MarketStrength marketStrength = calculateMarketStrength(priceChange6h, volume6h, volume24h, volumeRatio);
            BigDecimal liquidityScore = calculateLiquidityScore(quoteVolume6h,
                    new BigDecimal(ticker24h.getTicker().getQuoteVolume()), volume6h, volume24h);
            VolumeSignal signal = determineVolumeSignal(volumeRatio, isPriceVolumeAligned, marketStrength);

            return VolumeAnalysis.builder()
                    .volumeRatio(volumeRatio)
                    .volume6h(volume6h)
                    .volume1h(volume1h)
                    .avgVolumePerHour6h(avgVolumePerHour6h)
                    .isVolumeIncreasing(isVolumeIncreasing)
                    .isPriceVolumeAlignment(isPriceVolumeAligned)
                    .marketStrength(marketStrength)
                    .liquidityScore(liquidityScore)
                    .bidAskSpread(spread)
                    .signal(signal)
                    .build();

        } catch (Exception e) {
            log.error("Error analyzing volume", e);
            return createDefaultVolumeAnalysis();
        }
    }

    /**
     * Checks if price movement is supported by volume (crucial for scalping)
     */
    public boolean isPriceVolumeAligned(BigDecimal priceChange, BigDecimal volumeRatio) {
        // Strong bullish signal: Price up + High volume
        if (priceChange.compareTo(BigDecimal.ZERO) > 0 && volumeRatio.compareTo(HIGH_VOLUME_THRESHOLD) > 0) {
            log.debug("Strong bullish alignment: Price up {}, Volume ratio {}", priceChange, volumeRatio);
            return true;
        }

        // Weak signal: Price up + Low volume (potential fake breakout)
        if (priceChange.compareTo(BigDecimal.ZERO) > 0 && volumeRatio.compareTo(LOW_VOLUME_THRESHOLD) < 0) {
            log.debug("Weak signal detected: Price up {} but low volume {}", priceChange, volumeRatio);
            return false;
        }

        // Strong bearish signal: Price down + High volume (strong selling pressure)
        if (priceChange.compareTo(BigDecimal.ZERO) < 0 && volumeRatio.compareTo(HIGH_VOLUME_THRESHOLD) > 0) {
            log.debug("Strong bearish alignment: Price down {}, Volume ratio {}", priceChange, volumeRatio);
            return false; // Not good for buying
        }

        // Moderate alignment for other cases
        return volumeRatio.compareTo(new BigDecimal("0.8")) > 0;
    }

    /**
     * Calculates market strength based on price and volume relative changes
     * Uses relative volume analysis instead of fixed thresholds for different
     * crypto pairs
     */
    public MarketStrength calculateMarketStrength(BigDecimal priceChange, BigDecimal volume6h, BigDecimal volume24h,
            BigDecimal volumeRatio) {
        BigDecimal absPriceChange = priceChange.abs();

        // Calculate relative volume strength (6h vs 24h average)
        BigDecimal avgVolumePerHour24h = volume24h.divide(new BigDecimal("24"), 8, RoundingMode.HALF_UP);
        BigDecimal avgVolumePerHour6h = volume6h.divide(new BigDecimal("6"), 8, RoundingMode.HALF_UP);
        BigDecimal volume6hVs24hRatio = avgVolumePerHour6h.divide(avgVolumePerHour24h, 8, RoundingMode.HALF_UP);

        // Very strong: High price change + significantly above average volume (6h
        // period is much more active than 24h average)
        if (absPriceChange.compareTo(STRONG_PRICE_CHANGE) > 0 &&
                volume6hVs24hRatio.compareTo(new BigDecimal("2.0")) > 0 && // 6h volume is 2x higher than 24h average
                volumeRatio.compareTo(new BigDecimal("1.5")) > 0) { // Current hour is 1.5x higher than 6h average
            return MarketStrength.VERY_STRONG;
        }

        // Strong: Moderate price change + good relative volume increase
        if (absPriceChange.compareTo(MODERATE_PRICE_CHANGE) > 0 &&
                volume6hVs24hRatio.compareTo(new BigDecimal("1.5")) > 0 && // 6h volume is 1.5x higher than 24h average
                volumeRatio.compareTo(new BigDecimal("1.2")) > 0) { // Current hour is 1.2x higher than 6h average
            return MarketStrength.STRONG;
        }

        // Moderate: Some price change + moderate volume increase
        if (absPriceChange.compareTo(new BigDecimal("0.1")) > 0 &&
                volume6hVs24hRatio.compareTo(new BigDecimal("1.0")) > 0 && // 6h volume at least equal to 24h average
                volumeRatio.compareTo(new BigDecimal("0.8")) > 0) { // Current hour is at least 80% of 6h average
            return MarketStrength.MODERATE;
        }

        // Very weak: Significant volume decrease regardless of price
        if (volume6hVs24hRatio.compareTo(new BigDecimal("0.3")) < 0 || // 6h volume less than 30% of 24h average
                volumeRatio.compareTo(new BigDecimal("0.3")) < 0) { // Current hour less than 30% of 6h average
            return MarketStrength.VERY_WEAK;
        }

        // Weak: Low relative volume activity
        if (volume6hVs24hRatio.compareTo(new BigDecimal("0.7")) < 0 || // 6h volume less than 70% of 24h average
                volumeRatio.compareTo(new BigDecimal("0.7")) < 0) { // Current hour less than 70% of 6h average
            return MarketStrength.WEAK;
        }

        return MarketStrength.MODERATE;
    }

    /**
     * Calculates liquidity score for scalping suitability using relative volume
     * analysis
     */
    public BigDecimal calculateLiquidityScore(BigDecimal quoteVolume6h, BigDecimal quoteVolume24h,
            BigDecimal volume6h, BigDecimal volume24h) {
        BigDecimal score = BigDecimal.ZERO;

        // Calculate relative volume activity (6h vs 24h average)
        BigDecimal avgQuoteVolumePerHour24h = quoteVolume24h.divide(new BigDecimal("24"), 8, RoundingMode.HALF_UP);
        BigDecimal avgQuoteVolumePerHour6h = quoteVolume6h.divide(new BigDecimal("6"), 8, RoundingMode.HALF_UP);
        BigDecimal quoteVolumeRatio = avgQuoteVolumePerHour6h.divide(avgQuoteVolumePerHour24h, 8, RoundingMode.HALF_UP);

        BigDecimal avgVolumePerHour24h = volume24h.divide(new BigDecimal("24"), 8, RoundingMode.HALF_UP);
        BigDecimal avgVolumePerHour6h = volume6h.divide(new BigDecimal("6"), 8, RoundingMode.HALF_UP);
        BigDecimal baseVolumeRatio = avgVolumePerHour6h.divide(avgVolumePerHour24h, 8, RoundingMode.HALF_UP);

        // Quote volume score (0-40 points) - based on relative activity
        if (quoteVolumeRatio.compareTo(new BigDecimal("2.0")) > 0) { // 6h period 2x more active than 24h average
            score = score.add(new BigDecimal("40"));
        } else if (quoteVolumeRatio.compareTo(new BigDecimal("1.5")) > 0) { // 1.5x more active
            score = score.add(new BigDecimal("30"));
        } else if (quoteVolumeRatio.compareTo(new BigDecimal("1.0")) > 0) { // At least as active as average
            score = score.add(new BigDecimal("20"));
        } else if (quoteVolumeRatio.compareTo(new BigDecimal("0.5")) > 0) { // At least 50% of average
            score = score.add(new BigDecimal("10"));
        }

        // Base volume score (0-30 points) - based on relative activity
        if (baseVolumeRatio.compareTo(new BigDecimal("2.0")) > 0) { // 6h period 2x more active
            score = score.add(new BigDecimal("30"));
        } else if (baseVolumeRatio.compareTo(new BigDecimal("1.5")) > 0) { // 1.5x more active
            score = score.add(new BigDecimal("20"));
        } else if (baseVolumeRatio.compareTo(new BigDecimal("1.0")) > 0) { // At least as active as average
            score = score.add(new BigDecimal("15"));
        } else if (baseVolumeRatio.compareTo(new BigDecimal("0.5")) > 0) { // At least 50% of average
            score = score.add(new BigDecimal("10"));
        }

        // Consistency score (0-30 points) - how aligned quote and base volume are
        BigDecimal volumeAlignment = quoteVolumeRatio.subtract(baseVolumeRatio).abs();
        if (volumeAlignment.compareTo(new BigDecimal("0.2")) < 0) { // Very aligned
            score = score.add(new BigDecimal("30"));
        } else if (volumeAlignment.compareTo(new BigDecimal("0.5")) < 0) { // Moderately aligned
            score = score.add(new BigDecimal("20"));
        } else if (volumeAlignment.compareTo(new BigDecimal("1.0")) < 0) { // Somewhat aligned
            score = score.add(new BigDecimal("10"));
        }

        return score;
    }

    /**
     * Determines volume signal for trading decisions
     */
    public VolumeSignal determineVolumeSignal(BigDecimal volumeRatio, boolean isPriceVolumeAligned,
            MarketStrength marketStrength) {

        // Strong buy: High volume + alignment + strong market
        if (volumeRatio.compareTo(HIGH_VOLUME_THRESHOLD) > 0 &&
                isPriceVolumeAligned &&
                (marketStrength == MarketStrength.VERY_STRONG || marketStrength == MarketStrength.STRONG)) {
            return VolumeSignal.STRONG_BUY;
        }

        // Moderate buy: Good volume + alignment
        if (volumeRatio.compareTo(new BigDecimal("1.0")) > 0 &&
                isPriceVolumeAligned &&
                marketStrength != MarketStrength.VERY_WEAK) {
            return VolumeSignal.MODERATE_BUY;
        }

        // Avoid: Low volume or poor alignment
        if (volumeRatio.compareTo(LOW_VOLUME_THRESHOLD) < 0 || !isPriceVolumeAligned) {
            return VolumeSignal.AVOID;
        }

        // Strong avoid: Very low volume or very weak market
        if (volumeRatio.compareTo(new BigDecimal("0.3")) < 0 || marketStrength == MarketStrength.VERY_WEAK) {
            return VolumeSignal.STRONG_AVOID;
        }

        return VolumeSignal.NEUTRAL;
    }

    /**
     * Analyzes liquidity for scalping suitability
     */
    public LiquidityAnalysis analyzeLiquidity(TickerDto ticker, TickerBookTickerResponse1 book) {
        try {
            BigDecimal quoteVolume24h = new BigDecimal(ticker.getTicker().getQuoteVolume());
            BigDecimal totalTrades = new BigDecimal(ticker.getTicker().getCount());
            BigDecimal bidPrice = new BigDecimal(book.getBidPrice());
            BigDecimal askPrice = new BigDecimal(book.getAskPrice());

            // Calculate metrics
            BigDecimal avgTradeSize = quoteVolume24h.divide(totalTrades, 8, RoundingMode.HALF_UP);
            BigDecimal bidAskSpread = askPrice.subtract(bidPrice);
            BigDecimal spreadPercentage = bidAskSpread.divide(bidPrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            BigDecimal liquidityScore = calculateLiquidityScore(quoteVolume24h,
                    quoteVolume24h, // Using 24h as baseline for comparison
                    new BigDecimal(ticker.getTicker().getVolume()),
                    new BigDecimal(ticker.getTicker().getVolume())); // Using same volume for consistency

            // Determine liquidity level
            LiquidityLevel liquidityLevel = determineLiquidityLevel(quoteVolume24h, avgTradeSize, spreadPercentage,
                    liquidityScore);
            boolean hasGoodLiquidity = liquidityLevel == LiquidityLevel.EXCELLENT
                    || liquidityLevel == LiquidityLevel.GOOD;
            boolean isSuitableForScalping = hasGoodLiquidity && spreadPercentage.compareTo(MAX_SPREAD_PERCENTAGE) < 0;

            return LiquidityAnalysis.builder()
                    .quoteVolume24h(quoteVolume24h)
                    .avgTradeSize(avgTradeSize)
                    .bidAskSpread(bidAskSpread)
                    .spreadPercentage(spreadPercentage)
                    .liquidityScore(liquidityScore)
                    .hasGoodLiquidity(hasGoodLiquidity)
                    .isSuitableForScalping(isSuitableForScalping)
                    .liquidityLevel(liquidityLevel)
                    .totalTrades(totalTrades)
                    .marketDepthScore(liquidityScore) // Simplified market depth score
                    .build();

        } catch (Exception e) {
            log.error("Error analyzing liquidity", e);
            return createDefaultLiquidityAnalysis();
        }
    }

    /**
     * Determines liquidity level based on relative metrics instead of fixed
     * thresholds
     */
    public LiquidityLevel determineLiquidityLevel(BigDecimal quoteVolume24h, BigDecimal avgTradeSize,
            BigDecimal spreadPercentage, BigDecimal volumeConsistency) {

        // Calculate relative trade frequency (higher is better for scalping)
        BigDecimal totalTrades = quoteVolume24h.divide(avgTradeSize, 2, RoundingMode.HALF_UP);
        BigDecimal tradesPerHour = totalTrades.divide(new BigDecimal("24"), 2, RoundingMode.HALF_UP);

        // Excellent liquidity: Very tight spreads + high trade frequency + consistent
        // volume
        if (spreadPercentage.compareTo(new BigDecimal("0.05")) < 0 && // Very tight spread
                tradesPerHour.compareTo(new BigDecimal("100")) > 0 && // High frequency (>100 trades/hour)
                volumeConsistency.compareTo(new BigDecimal("80")) > 0) { // High volume consistency score
            return LiquidityLevel.EXCELLENT;
        }

        // Good liquidity: Reasonable spreads + moderate trade frequency + good volume
        if (spreadPercentage.compareTo(MAX_SPREAD_PERCENTAGE) < 0 && // Acceptable spread
                tradesPerHour.compareTo(new BigDecimal("50")) > 0 && // Moderate frequency (>50 trades/hour)
                volumeConsistency.compareTo(new BigDecimal("60")) > 0) { // Good volume consistency
            return LiquidityLevel.GOOD;
        }

        // Moderate liquidity: Acceptable spreads + some trading activity
        if (spreadPercentage.compareTo(new BigDecimal("0.2")) < 0 && // Reasonable spread
                tradesPerHour.compareTo(new BigDecimal("20")) > 0 && // Some frequency (>20 trades/hour)
                volumeConsistency.compareTo(new BigDecimal("40")) > 0) { // Moderate volume consistency
            return LiquidityLevel.MODERATE;
        }

        // Poor liquidity: Wide spreads or low activity
        if (spreadPercentage.compareTo(new BigDecimal("0.5")) < 0 && // Wide but manageable spread
                tradesPerHour.compareTo(new BigDecimal("5")) > 0) { // Low frequency (>5 trades/hour)
            return LiquidityLevel.POOR;
        }

        return LiquidityLevel.VERY_POOR;
    }

    /**
     * Analyzes market momentum across timeframes
     */
    public MarketMomentum analyzeMomentum(TickerDto ticker1h, TickerDto ticker6h, TickerDto ticker24h) {
        try {
            BigDecimal priceChange1h = new BigDecimal(ticker1h.getTicker().getPriceChangePercent());
            BigDecimal priceChange6h = new BigDecimal(ticker6h.getTicker().getPriceChangePercent());
            BigDecimal priceChange24h = new BigDecimal(ticker24h.getTicker().getPriceChangePercent());

            MomentumDirection shortTerm = determineMomentumDirection(priceChange1h);
            MomentumDirection mediumTerm = determineMomentumDirection(priceChange6h);
            MomentumDirection longTerm = determineMomentumDirection(priceChange24h);

            boolean isTrendAligned = isTrendAligned(shortTerm, mediumTerm, longTerm);
            boolean isSuitableForScalping = isSuitableForScalping(shortTerm, mediumTerm, longTerm);
            BigDecimal momentumScore = calculateMomentumScore(priceChange1h, priceChange6h, priceChange24h);

            return MarketMomentum.builder()
                    .priceChange1h(priceChange1h)
                    .priceChange6h(priceChange6h)
                    .priceChange24h(priceChange24h)
                    .shortTermTrend(shortTerm)
                    .mediumTermTrend(mediumTerm)
                    .longTermTrend(longTerm)
                    .isTrendAligned(isTrendAligned)
                    .isSuitableForScalping(isSuitableForScalping)
                    .momentumScore(momentumScore)
                    .build();

        } catch (Exception e) {
            log.error("Error analyzing momentum", e);
            return createDefaultMomentum();
        }
    }

    /**
     * Determines momentum direction based on price change
     */
    public MomentumDirection determineMomentumDirection(BigDecimal priceChange) {
        if (priceChange.compareTo(STRONG_PRICE_CHANGE) > 0) {
            return MomentumDirection.STRONG_BULLISH;
        } else if (priceChange.compareTo(MODERATE_PRICE_CHANGE) > 0) {
            return MomentumDirection.BULLISH;
        } else if (priceChange.compareTo(MODERATE_PRICE_CHANGE.negate()) < 0) {
            return MomentumDirection.BEARISH;
        } else if (priceChange.compareTo(STRONG_PRICE_CHANGE.negate()) < 0) {
            return MomentumDirection.STRONG_BEARISH;
        } else {
            return MomentumDirection.NEUTRAL;
        }
    }

    /**
     * Checks if trends are aligned across timeframes
     */
    public boolean isTrendAligned(MomentumDirection shortTerm, MomentumDirection mediumTerm,
            MomentumDirection longTerm) {
        // All bullish
        if ((shortTerm == MomentumDirection.BULLISH || shortTerm == MomentumDirection.STRONG_BULLISH) &&
                (mediumTerm == MomentumDirection.BULLISH || mediumTerm == MomentumDirection.STRONG_BULLISH) &&
                (longTerm == MomentumDirection.BULLISH || longTerm == MomentumDirection.STRONG_BULLISH)) {
            return true;
        }

        // All bearish
        if ((shortTerm == MomentumDirection.BEARISH || shortTerm == MomentumDirection.STRONG_BEARISH) &&
                (mediumTerm == MomentumDirection.BEARISH || mediumTerm == MomentumDirection.STRONG_BEARISH) &&
                (longTerm == MomentumDirection.BEARISH || longTerm == MomentumDirection.STRONG_BEARISH)) {
            return true;
        }

        return false;
    }

    /**
     * Determines if market conditions are suitable for scalping
     */
    public boolean isSuitableForScalping(MomentumDirection shortTerm, MomentumDirection mediumTerm,
            MomentumDirection longTerm) {
        // Avoid strong downtrends
        if (longTerm == MomentumDirection.STRONG_BEARISH || mediumTerm == MomentumDirection.STRONG_BEARISH) {
            return false;
        }

        // Good for scalping: Short-term bullish with neutral/bullish medium/long term
        if ((shortTerm == MomentumDirection.BULLISH || shortTerm == MomentumDirection.STRONG_BULLISH) &&
                mediumTerm != MomentumDirection.STRONG_BEARISH &&
                longTerm != MomentumDirection.STRONG_BEARISH) {
            return true;
        }

        return false;
    }

    /**
     * Calculates momentum score for decision making
     */
    public BigDecimal calculateMomentumScore(BigDecimal change1h, BigDecimal change6h, BigDecimal change24h) {
        BigDecimal score = BigDecimal.ZERO;

        // Weight recent changes more heavily
        score = score.add(change1h.multiply(new BigDecimal("0.5")));
        score = score.add(change6h.multiply(new BigDecimal("0.3")));
        score = score.add(change24h.multiply(new BigDecimal("0.2")));

        return score;
    }

    /**
     * Comprehensive analysis for scalping decisions
     */
    public ScalpingDecision makeScalpingDecision(VolumeAnalysis volumeAnalysis, MarketMomentum momentum,
            LiquidityAnalysis liquidity) {
        try {
            // Determine overall signal
            ScalpingSignal signal = determineScalpingSignal(volumeAnalysis, momentum, liquidity);
            BigDecimal confidence = calculateConfidence(volumeAnalysis, momentum, liquidity);
            String reason = generateDecisionReason(volumeAnalysis, momentum, liquidity, signal);

            boolean shouldBuy = (signal == ScalpingSignal.STRONG_BUY || signal == ScalpingSignal.MODERATE_BUY);
            boolean shouldSell = false; // TODO: Implement sell logic
            boolean shouldAvoid = (signal == ScalpingSignal.AVOID || signal == ScalpingSignal.EMERGENCY_EXIT);

            BigDecimal suggestedTradeSize = calculateSuggestedTradeSize(liquidity, confidence);
            BigDecimal suggestedProfitMargin = calculateSuggestedProfitMargin(momentum, confidence);
            BigDecimal riskScore = calculateRiskScore(volumeAnalysis, momentum, liquidity);

            return ScalpingDecision.builder()
                    .shouldBuy(shouldBuy)
                    .shouldSell(shouldSell)
                    .shouldAvoid(shouldAvoid)
                    .signal(signal)
                    .confidence(confidence)
                    .reason(reason)
                    .volumeAnalysis(volumeAnalysis)
                    .marketMomentum(momentum)
                    .liquidityAnalysis(liquidity)
                    .suggestedTradeSize(suggestedTradeSize)
                    .suggestedProfitMargin(suggestedProfitMargin)
                    .riskScore(riskScore)
                    .build();

        } catch (Exception e) {
            log.error("Error making scalping decision", e);
            return createDefaultScalpingDecision();
        }
    }

    /**
     * Determines overall scalping signal
     */
    private ScalpingSignal determineScalpingSignal(VolumeAnalysis volume, MarketMomentum momentum,
            LiquidityAnalysis liquidity) {
        // Emergency exit conditions
        if (!liquidity.isHasGoodLiquidity() || momentum.getLongTermTrend() == MomentumDirection.STRONG_BEARISH) {
            return ScalpingSignal.EMERGENCY_EXIT;
        }

        // Avoid conditions
        if (volume.getSignal() == VolumeSignal.AVOID || volume.getSignal() == VolumeSignal.STRONG_AVOID ||
                !momentum.isSuitableForScalping() || !liquidity.isSuitableForScalping()) {
            return ScalpingSignal.AVOID;
        }

        // Strong buy conditions
        if (volume.getSignal() == VolumeSignal.STRONG_BUY &&
                momentum.isTrendAligned() &&
                momentum.getShortTermTrend() == MomentumDirection.STRONG_BULLISH &&
                liquidity.getLiquidityLevel() == LiquidityLevel.EXCELLENT) {
            return ScalpingSignal.STRONG_BUY;
        }

        // Moderate buy conditions
        if (volume.getSignal() == VolumeSignal.MODERATE_BUY &&
                momentum.isSuitableForScalping() &&
                liquidity.isHasGoodLiquidity()) {
            return ScalpingSignal.MODERATE_BUY;
        }

        // Weak buy conditions
        if (volume.getSignal() == VolumeSignal.MODERATE_BUY &&
                momentum.getShortTermTrend() == MomentumDirection.BULLISH) {
            return ScalpingSignal.WEAK_BUY;
        }

        return ScalpingSignal.HOLD;
    }

    /**
     * Calculates confidence level for the decision
     */
    private BigDecimal calculateConfidence(VolumeAnalysis volume, MarketMomentum momentum,
            LiquidityAnalysis liquidity) {
        BigDecimal confidence = BigDecimal.ZERO;

        // Volume confidence (0-40)
        if (volume.getSignal() == VolumeSignal.STRONG_BUY)
            confidence = confidence.add(new BigDecimal("40"));
        else if (volume.getSignal() == VolumeSignal.MODERATE_BUY)
            confidence = confidence.add(new BigDecimal("25"));
        else if (volume.getSignal() == VolumeSignal.NEUTRAL)
            confidence = confidence.add(new BigDecimal("15"));

        // Momentum confidence (0-30)
        if (momentum.isTrendAligned())
            confidence = confidence.add(new BigDecimal("20"));
        if (momentum.isSuitableForScalping())
            confidence = confidence.add(new BigDecimal("10"));

        // Liquidity confidence (0-30)
        if (liquidity.getLiquidityLevel() == LiquidityLevel.EXCELLENT)
            confidence = confidence.add(new BigDecimal("30"));
        else if (liquidity.getLiquidityLevel() == LiquidityLevel.GOOD)
            confidence = confidence.add(new BigDecimal("20"));
        else if (liquidity.getLiquidityLevel() == LiquidityLevel.MODERATE)
            confidence = confidence.add(new BigDecimal("10"));

        return confidence;
    }

    /**
     * Generates human-readable reason for the decision
     */
    private String generateDecisionReason(VolumeAnalysis volume, MarketMomentum momentum,
            LiquidityAnalysis liquidity, ScalpingSignal signal) {
        StringBuilder reason = new StringBuilder();

        reason.append("Signal: ").append(signal).append(". ");
        reason.append("Volume: ").append(volume.getSignal()).append(" (ratio: ")
                .append(volume.getVolumeRatio().setScale(2, RoundingMode.HALF_UP)).append("). ");
        reason.append("Momentum: ").append(momentum.getShortTermTrend()).append("/")
                .append(momentum.getMediumTermTrend()).append("/").append(momentum.getLongTermTrend()).append(". ");
        reason.append("Liquidity: ").append(liquidity.getLiquidityLevel()).append(".");

        return reason.toString();
    }

    /**
     * Calculates suggested trade size based on confidence and liquidity
     */
    private BigDecimal calculateSuggestedTradeSize(LiquidityAnalysis liquidity, BigDecimal confidence) {
        BigDecimal baseSize = new BigDecimal("10"); // Base $10 USDT

        // Adjust based on confidence
        BigDecimal confidenceMultiplier = confidence.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);

        // Adjust based on liquidity
        BigDecimal liquidityMultiplier = new BigDecimal("1");
        if (liquidity.getLiquidityLevel() == LiquidityLevel.EXCELLENT) {
            liquidityMultiplier = new BigDecimal("3");
        } else if (liquidity.getLiquidityLevel() == LiquidityLevel.GOOD) {
            liquidityMultiplier = new BigDecimal("2");
        }

        return baseSize.multiply(confidenceMultiplier).multiply(liquidityMultiplier);
    }

    /**
     * Calculates suggested profit margin based on momentum and confidence
     */
    private BigDecimal calculateSuggestedProfitMargin(MarketMomentum momentum, BigDecimal confidence) {
        BigDecimal baseProfitMargin = new BigDecimal("0.3"); // 0.3%

        // Increase margin for strong momentum
        if (momentum.getShortTermTrend() == MomentumDirection.STRONG_BULLISH) {
            baseProfitMargin = baseProfitMargin.multiply(new BigDecimal("1.5"));
        }

        // Adjust based on confidence
        if (confidence.compareTo(new BigDecimal("70")) > 0) {
            baseProfitMargin = baseProfitMargin.multiply(new BigDecimal("1.2"));
        }

        return baseProfitMargin;
    }

    /**
     * Calculates risk score for the trade
     */
    private BigDecimal calculateRiskScore(VolumeAnalysis volume, MarketMomentum momentum, LiquidityAnalysis liquidity) {
        BigDecimal risk = BigDecimal.ZERO;

        // Volume risk
        if (volume.getVolumeRatio().compareTo(LOW_VOLUME_THRESHOLD) < 0) {
            risk = risk.add(new BigDecimal("30"));
        }

        // Momentum risk
        if (momentum.getLongTermTrend() == MomentumDirection.BEARISH ||
                momentum.getLongTermTrend() == MomentumDirection.STRONG_BEARISH) {
            risk = risk.add(new BigDecimal("40"));
        }

        // Liquidity risk
        if (liquidity.getLiquidityLevel() == LiquidityLevel.POOR ||
                liquidity.getLiquidityLevel() == LiquidityLevel.VERY_POOR) {
            risk = risk.add(new BigDecimal("30"));
        }

        return risk;
    }

    // Default objects for error cases
    private VolumeAnalysis createDefaultVolumeAnalysis() {
        return VolumeAnalysis.builder()
                .volumeRatio(BigDecimal.ZERO)
                .isVolumeIncreasing(false)
                .isPriceVolumeAlignment(false)
                .marketStrength(MarketStrength.VERY_WEAK)
                .liquidityScore(BigDecimal.ZERO)
                .signal(VolumeSignal.STRONG_AVOID)
                .build();
    }

    private LiquidityAnalysis createDefaultLiquidityAnalysis() {
        return LiquidityAnalysis.builder()
                .hasGoodLiquidity(false)
                .isSuitableForScalping(false)
                .liquidityLevel(LiquidityLevel.VERY_POOR)
                .liquidityScore(BigDecimal.ZERO)
                .build();
    }

    private MarketMomentum createDefaultMomentum() {
        return MarketMomentum.builder()
                .shortTermTrend(MomentumDirection.NEUTRAL)
                .mediumTermTrend(MomentumDirection.NEUTRAL)
                .longTermTrend(MomentumDirection.NEUTRAL)
                .isTrendAligned(false)
                .isSuitableForScalping(false)
                .momentumScore(BigDecimal.ZERO)
                .build();
    }

    private ScalpingDecision createDefaultScalpingDecision() {
        return ScalpingDecision.builder()
                .shouldBuy(false)
                .shouldSell(false)
                .shouldAvoid(true)
                .signal(ScalpingSignal.AVOID)
                .confidence(BigDecimal.ZERO)
                .reason("Error in analysis")
                .riskScore(new BigDecimal("100"))
                .build();
    }
}
