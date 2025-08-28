package com.tradebot.rbm.utils.dto.stochasticOscilator;

/**
 * Enum representing different Stochastic signals
 */
public enum StochasticSignal {
    STRONG_BUY, // Oversold and %K crosses above %D
    BUY, // Oversold condition
    NEUTRAL, // Normal range
    SELL, // Overbought condition
    STRONG_SELL, // Overbought and %K crosses below %D
    BULLISH_DIVERGENCE, // Price makes lower low but stochastic makes higher low
    BEARISH_DIVERGENCE // Price makes higher high but stochastic makes lower high
}
