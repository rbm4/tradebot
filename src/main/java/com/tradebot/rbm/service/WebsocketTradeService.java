package com.tradebot.rbm.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.spot.rest.model.DepthResponse;
import com.binance.connector.client.spot.rest.model.ExchangeInfoResponse;
import com.binance.connector.client.spot.rest.model.OrderOcoRequest;
import com.binance.connector.client.spot.rest.model.OrderOcoResponse;
import com.binance.connector.client.spot.rest.model.Side;
import com.binance.connector.client.spot.websocket.api.model.OrderPlaceRequest;
import com.binance.connector.client.spot.websocket.api.model.TimeInForce;
import com.binance.connector.client.spot.websocket.stream.model.BookTickerResponse;
import com.binance.connector.client.spot.websocket.stream.model.TradeResponse;
import com.tradebot.rbm.utils.DoubleLimitExample;
import com.tradebot.rbm.utils.RecentTradeUtils;
import com.tradebot.rbm.utils.ScalpingAnalysis;
import com.tradebot.rbm.utils.dto.PendingBuyOrderDTO;
import com.tradebot.rbm.utils.dto.ScalpingAction;
import com.tradebot.rbm.utils.dto.TradeData;
import com.tradebot.rbm.utils.dto.TradeMomentum;
import com.tradebot.rbm.websocket.AccountListenerWebsocketStream;

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
    private static final BigDecimal SCALP_MARGIN = new BigDecimal("0.04"); // 0.04$ margin for scalping
    private static final BigDecimal MIN_TRADE_AMOUNT = new BigDecimal("5.0"); // Minimum trade amount in USDT
    private static final BigDecimal MIN_TRADE_AMOUNT_QUOTE = new BigDecimal("0.008"); // Minimum trade amount in quote
                                                                                      // currency
    private static final BigDecimal MAX_POSITION_PERCENTAGE = new BigDecimal("1"); // Max 100% of balance per trade
    private static final long TRADE_ANALYSIS_WINDOW_SECONDS = 2400; // Analyze trades from last 240 seconds

    // Real-time data containers
    private final AtomicReference<BookTickerResponse> currentTicker = new AtomicReference<>();

    private final ExchangeInfoResponse exchangeInfoResponse;

    // Order tracking
    private final AtomicReference<PendingBuyOrderDTO> pendingBuyOrders = new AtomicReference<>();
    private final AtomicReference<OrderOcoResponse> pendingSellOrders = new AtomicReference<>();

    // Trading state
    private volatile boolean isActivelyTradingTicker = false;
    private volatile boolean isActivelyTrading = true;
    public static volatile LocalDateTime lastOrderTime = LocalDateTime.now().minus(1, ChronoUnit.MINUTES);

    /**
     * Updates the current ticker data from TickerWebsocketStream
     */
    public void updateTicker(BookTickerResponse ticker) {
        currentTicker.set(ticker);

        // Trigger scalping analysis when ticker updates
        if (isActivelyTradingTicker) {
            analyzeScalpingOpportunity();
        }
    }

    /**
     * Updates with new trade data from TradeWebsocketStream
     */
    public void updateTrade(TradeResponse trade) {
        RecentTradeUtils.lastTrade.set(trade);

        // Add to recent trades queue
        RecentTradeUtils.recentTrades.offer(new TradeData(trade));

        // Check if any pending buy orders might have been executed
        checkPendingOrderExecutions();

        // Trigger scalping analysis on new trade
        if (isActivelyTrading) {
            analyzeScalpingOpportunity();
        }
    }

    /**
     * Checks if incoming trade data matches any pending buy orders
     * This is a heuristic approach since we don't have direct order execution
     * callbacks
     */
    private void checkPendingOrderExecutions() {

        // Check each pending order to see if it could have been executed
        if (pendingBuyOrders.get() != null) {
            var pendingOrder = pendingBuyOrders.get();
            var buyPrice = pendingOrder.getBuyPrice();
            Integer limit = 5;
            ApiResponse<DepthResponse> response = orderService.depth(tradingSymbol, limit);
            var bid = new BigDecimal(response.getData().getBids().get(0).get(0));

            var tickerPrice = bid.add(SCALP_MARGIN.divide(BigDecimal.valueOf(2)));
            var sellPrice = buyPrice.add(SCALP_MARGIN.multiply(BigDecimal.valueOf(2))).max(tickerPrice);
            // Get account balances

            var baseAsset = extractBaseAsset(tradingSymbol);
            // check balance from account

            var quoteBalance = getAssetBalance(baseAsset);

            var canSell = quoteBalance.multiply(sellPrice).compareTo(MIN_TRADE_AMOUNT) > 0;
            if (canSell) {
                executeSellOrder(sellPrice, quoteBalance);
                pendingBuyOrders.set(null);
            }
        }
    }

    /**
     * Main scalping analysis method
     */
    private void analyzeScalpingOpportunity() {
        try {
            var ticker = currentTicker.get();
            var trade = RecentTradeUtils.lastTrade.get();
            var accountStatus = AccountListenerWebsocketStream.accountStatus;

            if (ticker == null || trade == null || accountStatus == null) {
                log.debug("Missing data for scalping analysis - Ticker: {}, Trade: {}, Account: {}",
                        ticker != null, trade != null, accountStatus != null);
                return;
            }

            // Cancel current buy order if is sitting too long
            if (ChronoUnit.SECONDS.between(lastOrderTime, LocalDateTime.now()) > 10 && pendingBuyOrders.get() != null) {
                log.debug("Cancelling Buy order");
                orderService.deleteBinanceOrder(tradingSymbol.toUpperCase(),
                        pendingBuyOrders.get().getBinanceOrderId());
                pendingBuyOrders.set(null);
                return;
            }

            ScalpingAnalysis analysis = performScalpingAnalysis(ticker, trade);

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
    private ScalpingAnalysis performScalpingAnalysis(BookTickerResponse ticker, TradeResponse trade) {

        var bidPrice = new BigDecimal(ticker.getbLowerCase());
        var askPrice = new BigDecimal(ticker.getaLowerCase());
        var lastTradePrice = new BigDecimal(trade.getpLowerCase());
        var spread = askPrice.subtract(bidPrice);

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

        BigDecimal baseBalance = getAssetBalance(baseAsset);
        BigDecimal quoteBalance = getAssetBalance(quoteAsset);

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

        for (TradeData tradeData : RecentTradeUtils.recentTrades) {
            if (tradeData.getTimestamp().isBefore(cutoffTime)) {
                continue; // Skip old trades
            }

            BigDecimal tradeVolume = new BigDecimal(tradeData.getTrade().getqLowerCase())
                    .multiply(new BigDecimal(tradeData.getTrade().getpLowerCase()));
            totalVolume = totalVolume.add(tradeVolume);

            // Determine if it's a buy or sell based on trade direction
            // Note: You might need to adjust this logic based on actual TradeResponse
            // structure
            boolean isBuyerMaker = tradeData.getTrade().getmLowerCase(); // Assuming this field exists

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
        BigDecimal buyOrderPrice = bidPrice.add(SCALP_MARGIN.negate()); // Slightly below bid
        BigDecimal sellOrderPrice = askPrice.add(SCALP_MARGIN); // Slightly above ask

        // Check if we have sufficient balance for buy order
        boolean canBuy = quoteBalance.compareTo(MIN_TRADE_AMOUNT) > 0;

        // Check if we have base asset to sell
        boolean canSell = baseBalance.multiply(lastTradePrice).compareTo(MIN_TRADE_AMOUNT) > 0;

        // Simple scalping logic based on momentum and position
        if ("BULLISH".equals(momentum.getDirection()) && canBuy) {
            BigDecimal quantity = calculateOptimalQuantity(quoteBalance, buyOrderPrice, true);
            return new ScalpingAction("BUY", buyOrderPrice, quantity, "Bullish momentum detected");
        }

        if ("BEARISH".equals(momentum.getDirection()) && canSell) {
            BigDecimal quantity = calculateOptimalQuantity(baseBalance, sellOrderPrice, false);
            return new ScalpingAction("SELL", sellOrderPrice, quantity, "Bearish momentum detected");
        }

        return new ScalpingAction("NONE", BigDecimal.ZERO, BigDecimal.ZERO, "No favorable conditions");
    }

    /**
     * Executes the scalping strategy based on analysis
     */
    private void executeScalpingStrategy(ScalpingAnalysis analysis) {
        ScalpingAction action = analysis.getAction();

        if ("NONE".equals(action.getType())) {
            log.debug("No scalping action: {}", action.getReason());
            return;
        }

        log.info("Executing scalping strategy - Action: {}, Price: {}, Quantity: {}, Reason: {}",
                action.getType(), action.getPrice(), action.getQuantity(), action.getReason());

        try {
            if ("BUY".equals(action.getType())) {
                executeBuyOrder(action.getPrice(), action.getQuantity());
            } else if ("SELL".equals(action.getType())) {
                executeSellOrder(action.getPrice(), action.getQuantity());
            }

            lastOrderTime = LocalDateTime.now();

        } catch (Exception e) {
            log.error("Error executing scalping order", e);
        }
    }

    /**
     * Executes a buy order and prepares for follow-up sell order
     */
    private void executeBuyOrder(BigDecimal price, BigDecimal quantity) {
        log.info("Placing BUY order - Symbol: {}, Price: {}, Quantity: {}", tradingSymbol, price, quantity);

        try {
            // Generate a unique order ID (you can replace this with actual order ID from
            // your order service)
            var orderId = "BUY_" + System.currentTimeMillis();

            // Calculate expected sell price with profit margin
            // var currentTickerData = currentTicker.get();
            Integer limit = 5;
            ApiResponse<DepthResponse> response = orderService.depth(tradingSymbol, limit);
            var ask = new BigDecimal(response.getData().getAsks().get(0).get(0));

            // Set sell price above current ask and with profit margin from buy price
            var askBasedPrice = ask.add(SCALP_MARGIN.negate());
            var expectedSellPrice = price.max(askBasedPrice);

            // Store the pending buy order
            var pendingOrder = new PendingBuyOrderDTO(orderId, tradingSymbol, price, quantity,
                    expectedSellPrice);
            pendingBuyOrders.set(pendingOrder);

            log.info("Buy order stored - ID: {}, Expected sell price: {}", orderId, expectedSellPrice);

            var orderDto = new OrderPlaceRequest();
            orderDto.setSymbol(tradingSymbol.toUpperCase());
            orderDto.setSide(com.binance.connector.client.spot.websocket.api.model.Side.BUY);
            orderDto.setType(com.binance.connector.client.spot.websocket.api.model.OrderType.LIMIT);
            orderDto.setPrice(price.doubleValue());
            orderDto.setQuantity(quantity.doubleValue());
            orderDto.setTimeInForce(TimeInForce.GTC);
            orderService.placeWsOrder(orderDto, pendingOrder);

            log.info("BUY order placed successfully - Monitoring for execution");

        } catch (Exception e) {
            log.error("Error placing buy order", e);
        }
    }

    /**
     * Executes a sell order
     */
    private void executeSellOrder(BigDecimal price, BigDecimal quantity) {
        log.info("Placing SELL order - Symbol: {}, Price: {}, Quantity: {}", tradingSymbol, price, quantity);
        var stopPrice = price.doubleValue() - (price.doubleValue() * 0.003);
        stopPrice = DoubleLimitExample.limitDecimal(stopPrice, 2);
        try {
            var sellOrderDto = new OrderOcoRequest();
            sellOrderDto.setSymbol(tradingSymbol.toUpperCase());
            sellOrderDto.setSide(Side.SELL);
            sellOrderDto.setPrice(price.doubleValue());
            sellOrderDto.setStopPrice(stopPrice);
            sellOrderDto.setQuantity(adjustLotSize(new BigDecimal(quantity.doubleValue())).doubleValue());
            var sellOrderResult = orderService.placeOcoOrder(sellOrderDto);
            pendingSellOrders.set(sellOrderResult);
        } catch (Exception e) {
            log.error("Error placing sell order", e);
        }
        // orderService.placeOcoOrder(sellOrderDto);
        // var orderDto = new OrderPlaceRequest();
        // orderDto.setSymbol(tradingSymbol.toUpperCase());
        // orderDto.setSide(com.binance.connector.client.spot.websocket.api.model.Side.SELL);
        // orderDto.setType(com.binance.connector.client.spot.websocket.api.model.OrderType.LIMIT);
        // orderDto.setPrice(price.doubleValue());
        // orderDto.setQuantity(adjustLotSize(new
        // BigDecimal(quantity.doubleValue())).doubleValue());
        // orderDto.setTimeInForce(TimeInForce.GTC);
        // orderService.placeWsOrder(orderDto);

        log.info("SELL order placed successfully");
    }

    /**
     * Calculates optimal quantity for order
     */
    private BigDecimal calculateOptimalQuantity(BigDecimal balance, BigDecimal price, boolean isBuy) {
        BigDecimal maxAmount = balance.multiply(MAX_POSITION_PERCENTAGE);

        if (isBuy) {
            // For buy orders, divide available quote balance by price
            if (tradingSymbol.toUpperCase().endsWith("BNBFDUSD")) {
                // Calculate how many BNB can be bought with maxAmount FDUSD at the given price
                maxAmount = maxAmount.divide(price, 8, RoundingMode.DOWN);
            }
            maxAmount = adjustLotSize(maxAmount);
            return BigDecimal.valueOf(DoubleLimitExample.limitDecimal(maxAmount.doubleValue(), 3));

        } else {
            // For sell orders, use the base balance directly
            return maxAmount;
        }
    }

    private BigDecimal adjustLotSize(BigDecimal maxAmount) {
        for (var symbol : exchangeInfoResponse.getSymbols()) {
            for (var filter : symbol.getFilters()) {
                if (filter.getFilterType().equals("LOT_SIZE")) {
                    var stepSize = new BigDecimal(filter.getStepSize());
                    // quantity % stepSize == 0
                    BigDecimal steps = maxAmount.divide(stepSize, 0, RoundingMode.DOWN);
                    maxAmount = steps.multiply(stepSize);
                    continue;
                }
            }
        }
        return maxAmount;
    }

    /**
     * Gets balance for a specific asset from account status
     */
    private BigDecimal getAssetBalance(String asset) {
        var acc = AccountListenerWebsocketStream.accountStatus.getResult();
        var quoteBalance = acc.getBalances().stream()
                .filter(balance -> asset.equals(balance.getAsset()))
                .map(balance -> new BigDecimal(balance.getFree()))
                .findFirst()
                .orElse(BigDecimal.ZERO);
        if (asset.equals("BNB") && quoteBalance.compareTo(MIN_TRADE_AMOUNT_QUOTE) < 0) {
            quoteBalance = adjustLotSize(quoteBalance);
            return BigDecimal.valueOf(DoubleLimitExample.limitDecimal(quoteBalance.doubleValue(), 3));
        }

        return BigDecimal.valueOf(DoubleLimitExample.limitDecimal(quoteBalance.doubleValue(), 5));
    }

    /**
     * Extracts base asset from trading symbol (e.g., "BTC" from "BTCUSDT")
     */
    private String extractBaseAsset(String symbol) {
        // Simple logic - you might need to adjust based on your symbol format
        var symbolCompare = symbol.toUpperCase();
        if (symbolCompare.endsWith("FDUSD")) {
            return symbolCompare.substring(0, symbolCompare.length() - 5);
        } else if (symbolCompare.endsWith("BTC") || symbolCompare.endsWith("DGB") || symbolCompare.endsWith("ETH")
                || symbolCompare.endsWith("BNB")) {
            return symbolCompare.substring(0, symbolCompare.length() - 3);
        }
        return symbolCompare.substring(0, symbolCompare.length() / 2); // Fallback
    }

    /**
     * Extracts quote asset from trading symbol (e.g., "USDT" from "BTCUSDT")
     */
    private String extractQuoteAsset(String symbol) {
        // Simple logic - you might need to adjust based on your symbol format
        var symbolCompare = symbol.toUpperCase();
        if (symbolCompare.endsWith("FDUSD")) {
            return "FDUSD";
        } else if (symbolCompare.endsWith("BTC")) {
            return "BTC";
        } else if (symbolCompare.endsWith("ETH")) {
            return "ETH";
        } else if (symbolCompare.endsWith("BNB")) {
            return "BNB";
        }
        return symbolCompare.substring(symbolCompare.length() / 2); // Fallback
    }

}
