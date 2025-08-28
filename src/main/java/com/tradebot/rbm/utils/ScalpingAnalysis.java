package com.tradebot.rbm.utils;

import java.math.BigDecimal;

import com.tradebot.rbm.utils.dto.ScalpingAction;
import com.tradebot.rbm.utils.dto.TradeMomentum;

import lombok.Data;

@Data
public class ScalpingAnalysis {
    private final ScalpingAction action;
    private final TradeMomentum momentum;
    private final BigDecimal bidPrice;
    private final BigDecimal askPrice;
    private final BigDecimal lastTradePrice;
    private final BigDecimal baseBalance;
    private final BigDecimal quoteBalance;
    private final BigDecimal spread;

    public ScalpingAnalysis(ScalpingAction action, TradeMomentum momentum, BigDecimal bidPrice,
            BigDecimal askPrice, BigDecimal lastTradePrice, BigDecimal baseBalance,
            BigDecimal quoteBalance, BigDecimal spread) {
        this.action = action;
        this.momentum = momentum;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.lastTradePrice = lastTradePrice;
        this.baseBalance = baseBalance;
        this.quoteBalance = quoteBalance;
        this.spread = spread;
    }

    public static ScalpingAnalysis noTrade(String reason) {
        return new ScalpingAnalysis(
                new ScalpingAction("NONE", BigDecimal.ZERO, BigDecimal.ZERO, reason),
                null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public boolean shouldTrade() {
        return !"NONE".equals(action.getType());
    }

    public ScalpingAction getAction() {
        return action;
    }
}
