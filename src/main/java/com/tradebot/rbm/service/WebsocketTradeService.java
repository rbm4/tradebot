package com.tradebot.rbm.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.binance.connector.client.spot.websocket.stream.model.BookTickerResponse;
import com.binance.connector.client.spot.websocket.stream.model.TradeResponse;
import com.tradebot.rbm.websocket.AccountListenerWebsocketStream;
import com.tradebot.rbm.websocket.dto.AccountStatusResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebsocketTradeService {

    private final OrderService orderService;

    @Value("${binance.trading.symbol:BTCUSDT}")
    private String tradingSymbol;

    // Scalping configuration
    private static final BigDecimal MIN_SPREAD_THRESHOLD = new BigDecimal("0.0001"); // Minimum spread to consider
    private static final BigDecimal SCALP_MARGIN = new BigDecimal("0.0005"); // 0.05% margin for scalping
    private static final BigDecimal MIN_TRADE_AMOUNT = new BigDecimal("10.0"); // Minimum trade amount in USDT
    private static final BigDecimal MAX_POSITION_PERCENTAGE = new BigDecimal("0.1"); // Max 10% of balance per trade
    private static final int MAX_RECENT_TRADES = 50; // Keep last 50 trades for analysis
    private static final long TRADE_ANALYSIS_WINDOW_SECONDS = 30; // Analyze trades from last 30 seconds

    // Real-time data containers
    private final AtomicReference<BookTickerResponse> currentTicker = new AtomicReference<>();
    private final AtomicReference<TradeResponse> lastTrade = new AtomicReference<>();
    private final ConcurrentLinkedQueue<TradeData> recentTrades = new ConcurrentLinkedQueue<>();

    // Trading state
    private volatile boolean isActivelyTrading = false;
    private volatile LocalDateTime lastOrderTime = LocalDateTime.now().minus(1, ChronoUnit.MINUTES);

    /**
     * Internal class to store trade data with timestamp
     */
    private static class TradeData {
        final TradeResponse trade;
        final LocalDateTime timestamp;

        TradeData(TradeResponse trade) {
            this.trade = trade;
            this.timestamp = LocalDateTime.now();
        }
    }

    /**
     * Updates the current ticker data from TickerWebsocketStream
     */
    public void updateTicker(BookTickerResponse ticker) {
        currentTicker.set(ticker);
        log.debug("Ticker updated - Bid: {}, Ask: {}, Symbol: {}, Bid qty: {}, Ask qty: {}",
                ticker.getbLowerCase(), ticker.getaLowerCase(), ticker.getsLowerCase(), ticker.getB(), ticker.getA());

        // Trigger scalping analysis when ticker updates
        if (isActivelyTrading) {
            analyzeScalpingOpportunity();
        }
    }

    /**
     * Updates with new trade data from TradeWebsocketStream
     */
    public void updateTrade(TradeResponse trade) {
        lastTrade.set(trade);

        // Add to recent trades queue
        recentTrades.offer(new TradeData(trade));

        // Maintain queue size
        while (recentTrades.size() > MAX_RECENT_TRADES) {
            recentTrades.poll();
        }

        log.debug("Trade updated - Price: {}, Quantity: {}, Time: {}",
                trade.getpLowerCase(), trade.getqLowerCase(), trade.getT());

        // Trigger scalping analysis on new trade
        if (isActivelyTrading) {
            analyzeScalpingOpportunity();
        }
    }

    /**
     * Starts the scalping strategy
     */
    public void startScalping() {
        isActivelyTrading = true;
        log.info("Scalping strategy started for symbol: {}", tradingSymbol);
    }

    /**
     * Stops the scalping strategy
     */
    public void stopScalping() {
        isActivelyTrading = false;
        log.info("Scalping strategy stopped for symbol: {}", tradingSymbol);
    }

    /**
     * Main scalping analysis method
     */
    private void analyzeScalpingOpportunity() {
        try {
            BookTickerResponse ticker = currentTicker.get();
            TradeResponse trade = lastTrade.get();
            AccountStatusResponse accountStatus = AccountListenerWebsocketStream.accountStatus;

            if (ticker == null || trade == null || accountStatus == null) {
                log.debug("Missing data for scalping analysis - Ticker: {}, Trade: {}, Account: {}",
                        ticker != null, trade != null, accountStatus != null);
                return;
            }

            // Prevent too frequent trading
            if (ChronoUnit.SECONDS.between(lastOrderTime, LocalDateTime.now()) < 10) {
                log.debug("Cooling down - last order was {} seconds ago",
                        ChronoUnit.SECONDS.between(lastOrderTime, LocalDateTime.now()));
                return;
            }

            ScalpingAnalysis analysis = performScalpingAnalysis(ticker, trade, accountStatus);

            if (analysis.shouldTrade()) {
                executeScalpingStrategy(analysis);
            }

        } catch (Exception e) {
            log.error("Error in scalping analysis", e);
        }
    }

    /**
     * Performs comprehensive scalping analysis
     */
    private ScalpingAnalysis performScalpingAnalysis(BookTickerResponse ticker, TradeResponse trade,
            AccountStatusResponse accountStatus) {

        BigDecimal bidPrice = new BigDecimal(ticker.getbLowerCase());
        BigDecimal askPrice = new BigDecimal(ticker.getaLowerCase());
        BigDecimal lastTradePrice = new BigDecimal(trade.getpLowerCase());
        BigDecimal spread = askPrice.subtract(bidPrice);

        log.debug("Market Analysis - Bid: {}, Ask: {}, Last Trade: {}, Spread: {}",
                bidPrice, askPrice, lastTradePrice, spread);

        // Check if spread is sufficient for scalping
        if (spread.compareTo(MIN_SPREAD_THRESHOLD) < 0) {
            return ScalpingAnalysis.noTrade("Spread too small: " + spread);
        }

        // Analyze recent trade momentum
        TradeMomentum momentum = analyzeRecentTradeMomentum();

        // Get account balances
        String baseAsset = extractBaseAsset(tradingSymbol);
        String quoteAsset = extractQuoteAsset(tradingSymbol);

        BigDecimal baseBalance = getAssetBalance(accountStatus, baseAsset);
        BigDecimal quoteBalance = getAssetBalance(accountStatus, quoteAsset);

        log.debug("Account balances - {}: {}, {}: {}", baseAsset, baseBalance, quoteAsset, quoteBalance);

        // Determine scalping action
        ScalpingAction action = determineScalpingAction(momentum, bidPrice, askPrice, lastTradePrice,
                baseBalance, quoteBalance, spread);

        return new ScalpingAnalysis(action, momentum, bidPrice, askPrice, lastTradePrice,
                baseBalance, quoteBalance, spread);
    }

    /**
     * Analyzes recent trade momentum to determine market direction
     */
    private TradeMomentum analyzeRecentTradeMomentum() {
        LocalDateTime cutoffTime = LocalDateTime.now().minus(TRADE_ANALYSIS_WINDOW_SECONDS, ChronoUnit.SECONDS);

        long buyCount = 0;
        long sellCount = 0;
        BigDecimal buyVolume = BigDecimal.ZERO;
        BigDecimal sellVolume = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;

        for (TradeData tradeData : recentTrades) {
            if (tradeData.timestamp.isBefore(cutoffTime)) {
                continue; // Skip old trades
            }

            BigDecimal tradeVolume = new BigDecimal(tradeData.trade.getqLowerCase())
                    .multiply(new BigDecimal(tradeData.trade.getpLowerCase()));
            totalVolume = totalVolume.add(tradeVolume);

            // Determine if it's a buy or sell based on trade direction
            // Note: You might need to adjust this logic based on actual TradeResponse
            // structure
            boolean isBuyerMaker = tradeData.trade.getmLowerCase(); // Assuming this field exists

            if (isBuyerMaker) {
                sellCount++; // If buyer is maker, it's actually a sell
                sellVolume = sellVolume.add(tradeVolume);
            } else {
                buyCount++; // If buyer is taker, it's a buy
                buyVolume = buyVolume.add(tradeVolume);
            }
        }

        String direction = "NEUTRAL";
        if (buyVolume.compareTo(sellVolume) > 0) {
            direction = "BULLISH";
        } else if (sellVolume.compareTo(buyVolume) > 0) {
            direction = "BEARISH";
        }

        log.debug("Trade momentum - Direction: {}, Buy trades: {}, Sell trades: {}, Buy volume: {}, Sell volume: {}",
                direction, buyCount, sellCount, buyVolume, sellVolume);

        return new TradeMomentum(direction, buyCount, sellCount, buyVolume, sellVolume, totalVolume);
    }

    /**
     * Determines the scalping action based on analysis
     */
    private ScalpingAction determineScalpingAction(TradeMomentum momentum, BigDecimal bidPrice, BigDecimal askPrice,
            BigDecimal lastTradePrice, BigDecimal baseBalance,
            BigDecimal quoteBalance, BigDecimal spread) {

        // Calculate potential order prices
        BigDecimal buyOrderPrice = bidPrice.add(bidPrice.multiply(SCALP_MARGIN.negate())); // Slightly below bid
        BigDecimal sellOrderPrice = askPrice.add(askPrice.multiply(SCALP_MARGIN)); // Slightly above ask

        // Check if we have sufficient balance for buy order
        boolean canBuy = quoteBalance.compareTo(MIN_TRADE_AMOUNT) > 0;

        // Check if we have base asset to sell
        boolean canSell = baseBalance.multiply(lastTradePrice).compareTo(MIN_TRADE_AMOUNT) > 0;

        // Simple scalping logic based on momentum and position
        if ("BULLISH".equals(momentum.direction) && canBuy) {
            BigDecimal quantity = calculateOptimalQuantity(quoteBalance, buyOrderPrice, true);
            return new ScalpingAction("BUY", buyOrderPrice, quantity, "Bullish momentum detected");
        }

        if ("BEARISH".equals(momentum.direction) && canSell) {
            BigDecimal quantity = calculateOptimalQuantity(baseBalance, sellOrderPrice, false);
            return new ScalpingAction("SELL", sellOrderPrice, quantity, "Bearish momentum detected");
        }

        // Market making strategy - place both buy and sell orders
        if ("NEUTRAL".equals(momentum.direction) && canBuy
                && spread.compareTo(MIN_SPREAD_THRESHOLD.multiply(BigDecimal.valueOf(2))) > 0) {
            BigDecimal buyQuantity = calculateOptimalQuantity(
                    quoteBalance.divide(BigDecimal.valueOf(2), RoundingMode.DOWN), buyOrderPrice, true);
            return new ScalpingAction("BUY", buyOrderPrice, buyQuantity, "Market making - neutral momentum");
        }

        return new ScalpingAction("NONE", BigDecimal.ZERO, BigDecimal.ZERO, "No favorable conditions");
    }

    /**
     * Executes the scalping strategy based on analysis
     */
    private void executeScalpingStrategy(ScalpingAnalysis analysis) {
        ScalpingAction action = analysis.getAction();

        if ("NONE".equals(action.type)) {
            log.debug("No scalping action: {}", action.reason);
            return;
        }

        log.info("Executing scalping strategy - Action: {}, Price: {}, Quantity: {}, Reason: {}",
                action.type, action.price, action.quantity, action.reason);

        try {
            if ("BUY".equals(action.type)) {
                executeBuyOrder(action.price, action.quantity);
            } else if ("SELL".equals(action.type)) {
                executeSellOrder(action.price, action.quantity);
            }

            lastOrderTime = LocalDateTime.now();

        } catch (Exception e) {
            log.error("Error executing scalping order", e);
        }
    }

    /**
     * Executes a buy order
     */
    private void executeBuyOrder(BigDecimal price, BigDecimal quantity) {
        log.info("Placing BUY order - Symbol: {}, Price: {}, Quantity: {}", tradingSymbol, price, quantity);

        // TODO: Integrate with your OrderService to place actual order
        // Example:
        // PlaceOrderDto orderDto = new PlaceOrderDto();
        // orderDto.setSymbol(tradingSymbol);
        // orderDto.setSide(Side.BUY);
        // orderDto.setType(OrderType.LIMIT);
        // orderDto.setPrice(price);
        // orderDto.setQuantity(quantity);
        // orderDto.setTimeInForce(TimeInForce.GTC);
        // orderService.placeOrder(orderDto);

        log.info("BUY order placed successfully");
    }

    /**
     * Executes a sell order
     */
    private void executeSellOrder(BigDecimal price, BigDecimal quantity) {
        log.info("Placing SELL order - Symbol: {}, Price: {}, Quantity: {}", tradingSymbol, price, quantity);

        // TODO: Integrate with your OrderService to place actual order
        // Similar to buy order but with Side.SELL

        log.info("SELL order placed successfully");
    }

    /**
     * Calculates optimal quantity for order
     */
    private BigDecimal calculateOptimalQuantity(BigDecimal balance, BigDecimal price, boolean isBuy) {
        BigDecimal maxAmount = balance.multiply(MAX_POSITION_PERCENTAGE);

        if (isBuy) {
            // For buy orders, divide available quote balance by price
            return maxAmount.divide(price, 8, RoundingMode.DOWN);
        } else {
            // For sell orders, use the base balance directly
            return maxAmount;
        }
    }

    /**
     * Gets balance for a specific asset from account status
     */
    private BigDecimal getAssetBalance(AccountStatusResponse accountStatus, String asset) {
        return accountStatus.getResult().getBalances().stream()
                .filter(balance -> asset.equals(balance.getAsset()))
                .map(balance -> new BigDecimal(balance.getFree()))
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Extracts base asset from trading symbol (e.g., "BTC" from "BTCUSDT")
     */
    private String extractBaseAsset(String symbol) {
        // Simple logic - you might need to adjust based on your symbol format
        if (symbol.endsWith("USDT")) {
            return symbol.substring(0, symbol.length() - 4);
        } else if (symbol.endsWith("BTC") || symbol.endsWith("ETH") || symbol.endsWith("BNB")) {
            return symbol.substring(0, symbol.length() - 3);
        }
        return symbol.substring(0, symbol.length() / 2); // Fallback
    }

    /**
     * Extracts quote asset from trading symbol (e.g., "USDT" from "BTCUSDT")
     */
    private String extractQuoteAsset(String symbol) {
        // Simple logic - you might need to adjust based on your symbol format
        if (symbol.endsWith("USDT")) {
            return "USDT";
        } else if (symbol.endsWith("BTC")) {
            return "BTC";
        } else if (symbol.endsWith("ETH")) {
            return "ETH";
        } else if (symbol.endsWith("BNB")) {
            return "BNB";
        }
        return symbol.substring(symbol.length() / 2); // Fallback
    }

    /**
     * Gets current trading status
     */
    public boolean isActivelyTrading() {
        return isActivelyTrading;
    }

    /**
     * Gets recent trades count for monitoring
     */
    public int getRecentTradesCount() {
        return recentTrades.size();
    }

    // Inner classes for analysis results

    private static class ScalpingAnalysis {
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
            return !"NONE".equals(action.type);
        }

        public ScalpingAction getAction() {
            return action;
        }
    }

    private static class ScalpingAction {
        private final String type;
        private final BigDecimal price;
        private final BigDecimal quantity;
        private final String reason;

        public ScalpingAction(String type, BigDecimal price, BigDecimal quantity, String reason) {
            this.type = type;
            this.price = price;
            this.quantity = quantity;
            this.reason = reason;
        }
    }

    private static class TradeMomentum {
        private final String direction;
        private final long buyCount;
        private final long sellCount;
        private final BigDecimal buyVolume;
        private final BigDecimal sellVolume;
        private final BigDecimal totalVolume;

        public TradeMomentum(String direction, long buyCount, long sellCount,
                BigDecimal buyVolume, BigDecimal sellVolume, BigDecimal totalVolume) {
            this.direction = direction;
            this.buyCount = buyCount;
            this.sellCount = sellCount;
            this.buyVolume = buyVolume;
            this.sellVolume = sellVolume;
            this.totalVolume = totalVolume;
        }
    }
}
