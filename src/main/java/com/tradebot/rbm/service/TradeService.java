package com.tradebot.rbm.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.binance.connector.client.spot.rest.model.GetAccountResponse;
import com.binance.connector.client.spot.rest.model.GetOpenOrdersResponse;
import com.binance.connector.client.spot.rest.model.OrderType;
import com.binance.connector.client.spot.rest.model.Side;
import com.binance.connector.client.spot.rest.model.WindowSize;
import com.tradebot.rbm.entity.dto.PlaceOrderDto;
import com.tradebot.rbm.entity.dto.ScalpingDTO;
import com.tradebot.rbm.entity.dto.TickerDto;
import com.tradebot.rbm.utils.VolumeAnalysisUtils;
import com.tradebot.rbm.utils.dto.LiquidityAnalysis;
import com.tradebot.rbm.utils.dto.MarketMomentum;
import com.tradebot.rbm.utils.dto.ScalpingDecision;
import com.tradebot.rbm.utils.dto.VolumeAnalysis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TradeService {

    private final WalletService walletService;
    private final SpotService spotService;
    private final OrderService orderService;
    private final VolumeAnalysisUtils volumeUtils;

    // Trading configuration - could be moved to application.yaml later
    private static final BigDecimal PROFIT_MARGIN_PERCENTAGE = new BigDecimal("0.3"); // 0.3%
    private static final BigDecimal MIN_TRADE_AMOUNT_USDT = new BigDecimal("10.0"); // Minimum $10 USDT trade
    private static final BigDecimal MAX_TRADE_PERCENTAGE = new BigDecimal("20.0"); // Max 20% of available balance
    private static final BigDecimal PRICE_DROP_THRESHOLD = new BigDecimal("1.0"); // 1% price drop to trigger buy
    private static final int QUANTITY_PRECISION = 6; // Decimal places for quantity
    private static final int PRICE_PRECISION = 8; // Decimal places for price

    /**
     * Main trading method - executes the algorithmic trading strategy
     * 
     * @param symbol Trading pair symbol (e.g., "BTCUSDT")
     * @return Trading result status
     */
    public String trade(String symbol) {
        try {
            log.info("Starting trade analysis for symbol: {}", symbol);

            // Step 1: Get account balance information
            GetAccountResponse accountInfo = walletService.accountInfo();
            if (accountInfo == null) {
                log.error("Failed to retrieve account information");
                return "FAILED: Unable to get account info";
            }

            // Step 2: Get market ticker information
            TickerDto tickerInfo6h = spotService.ticker(symbol, WindowSize.WINDOW_SIZE_6h);
            TickerDto tickerInfo1h = spotService.ticker(symbol, WindowSize.WINDOW_SIZE_1h);
            TickerDto tickerInfo24h = spotService.ticker(symbol, WindowSize.WINDOW_SIZE_1d);

            // Step 3: Check existing open orders
            GetOpenOrdersResponse openOrders = orderService.getOpenOrders(symbol);
            var scalpingDTO = ScalpingDTO.builder()
                    .symbol(symbol)
                    .tickerInfo1h(tickerInfo1h)
                    .tickerInfo24h(tickerInfo24h)
                    .tickerInfo6h(tickerInfo6h)
                    .accountInfo(accountInfo)
                    .openOrders(openOrders)
                    .build();
            // Step 4: Analyze market conditions and execute trading strategy
            return executeScalpingStrategy(scalpingDTO);

        } catch (Exception e) {
            log.error("Error during trade execution for {}: {}", symbol, e.getMessage(), e);
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Executes the scalping trading strategy
     */
    private String executeScalpingStrategy(ScalpingDTO scalpingDTO) {
        log.info("---------------------------- Starting scalping ----------------------------");
        String baseAsset = extractBaseAsset(scalpingDTO.getSymbol()); // e.g., "BTC" from "BTCUSDT"
        String quoteAsset = extractQuoteAsset(scalpingDTO.getSymbol()); // e.g., "USDT" from "BTCUSDT"

        // Get current market price
        BigDecimal currentPrice = new BigDecimal(scalpingDTO.getTickerInfo1h().getBookTicker().getBidPrice());
        BigDecimal askPrice = new BigDecimal(scalpingDTO.getTickerInfo1h().getBookTicker().getAskPrice());
        BigDecimal bidPrice = new BigDecimal(scalpingDTO.getTickerInfo1h().getBookTicker().getBidPrice());

        log.info("Current market - Bid: {}, Ask: {}, Symbol: {}", bidPrice, askPrice, scalpingDTO.getSymbol());

        // Get balances
        BigDecimal baseBalance = getAssetBalance(scalpingDTO.getAccountInfo(), baseAsset);
        BigDecimal quoteBalance = getAssetBalance(scalpingDTO.getAccountInfo(), quoteAsset);

        log.info("Account balances - {}: {}, {}: {}", baseAsset, baseBalance, quoteAsset, quoteBalance);

        // Check if we have existing open orders
        // TODO: Fix GetOpenOrdersResponse structure - need to check actual method names
        /*
         * if (openOrders.getOrders() != null && !openOrders.getOrders().isEmpty()) {
         * log.info("Found {} open orders for {}", openOrders.getOrders().size(),
         * symbol);
         * return handleExistingOrders(symbol, openOrders, currentPrice, baseBalance);
         * }
         */

        // For now, just log that we're checking for open orders
        log.info("Checking for existing open orders for {}", scalpingDTO.getSymbol());

        // Check if we should place a buy order
        if (shouldPlaceBuyOrder(scalpingDTO, quoteBalance)) {
            // return placeBuyOrder(scalpingDTO.getSymbol(), currentPrice, quoteBalance);
        }

        // Check if we should place a sell order (if we have base asset)
        if (baseBalance.compareTo(BigDecimal.ZERO) > 0) {
            return placeSellOrder(scalpingDTO.getSymbol(), currentPrice, baseBalance);
        }

        log.info("No trading action taken for {}", scalpingDTO.getSymbol());
        return "NO_ACTION: Market conditions not favorable";
    }

    /**
     * Analyzes market conditions to determine if a buy order should be placed
     */
    private boolean shouldPlaceBuyOrder(ScalpingDTO scalpingDTO, BigDecimal quoteBalance) {
        // Check if we have enough balance to trade
        if (quoteBalance.compareTo(MIN_TRADE_AMOUNT_USDT) < 0) {
            log.info("Insufficient balance for trading. Available: {}, Required: {}",
                    quoteBalance, MIN_TRADE_AMOUNT_USDT);
            return false;
        }
        var ticker1h = scalpingDTO.getTickerInfo1h();
        var ticker6h = scalpingDTO.getTickerInfo6h();
        var ticker24h = scalpingDTO.getTickerInfo24h();

        // Get price change percentage
        BigDecimal priceChangePercent = new BigDecimal(ticker6h.getTicker().getPriceChangePercent());
        log.info("Price change percent: {}%", priceChangePercent);

        // TODO: Implement more sophisticated market analysis
        // - RSI (Relative Strength Index) calculation
        // - Moving averages analysis
        // - Volume analysis
        // - Support/resistance levels

        // Simple strategy: Buy if price increased during ticker window
        boolean shouldBuy = priceChangePercent.compareTo(PRICE_DROP_THRESHOLD.negate()) == 1;

        // Additional checks
        // Comprehensive analysis
        VolumeAnalysis volumeAnalysis = volumeUtils.analyzeVolume(ticker6h, ticker1h, ticker24h);
        LiquidityAnalysis liquidityAnalysis = volumeUtils.analyzeLiquidity(ticker24h);
        MarketMomentum momentum = volumeUtils.analyzeMomentum(ticker1h, ticker6h, ticker24h);

        // TODO: Add volume-based filters
        // boolean volumeCheck = volume.compareTo(avgVolume.multiply(new
        // BigDecimal("0.8"))) > 0;

        log.info("Buy signal analysis - Price variance: {}%, Should buy: {}", priceChangePercent, shouldBuy);

        // Get final decision
        ScalpingDecision decision = volumeUtils.makeScalpingDecision(volumeAnalysis, momentum, liquidityAnalysis);

        log.info("Trading Decision: {} (Confidence: {}%) - {}",
                decision.getSignal(), decision.getConfidence(), decision.getReason());

        return decision.isShouldBuy();
    }

    /**
     * Places a buy order
     */
    private String placeBuyOrder(String symbol, BigDecimal currentPrice, BigDecimal availableBalance) {
        try {
            // Calculate trade amount (use a percentage of available balance)
            BigDecimal tradeAmount = availableBalance
                    .multiply(MAX_TRADE_PERCENTAGE)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            // Ensure minimum trade amount
            if (tradeAmount.compareTo(MIN_TRADE_AMOUNT_USDT) < 0) {
                tradeAmount = MIN_TRADE_AMOUNT_USDT;
            }

            // Calculate quantity to buy
            BigDecimal quantity = tradeAmount.divide(currentPrice, QUANTITY_PRECISION, RoundingMode.DOWN);

            log.info("Placing BUY order - Symbol: {}, Quantity: {}, Price: {}, Total: {}",
                    symbol, quantity, currentPrice, tradeAmount);

            PlaceOrderDto buyOrder = new PlaceOrderDto(
                    symbol,
                    Side.BUY.getValue(),
                    OrderType.MARKET.getValue(), // Using market order for immediate execution
                    currentPrice.doubleValue(),
                    quantity.doubleValue());

            orderService.placeOrder(buyOrder);

            log.info("BUY order placed successfully for {}", symbol);
            return "BUY_ORDER_PLACED: " + quantity + " " + symbol + " at " + currentPrice;

        } catch (Exception e) {
            log.error("Failed to place buy order for {}: {}", symbol, e.getMessage(), e);
            return "BUY_ORDER_FAILED: " + e.getMessage();
        }
    }

    /**
     * Places a sell order with profit margin
     */
    private String placeSellOrder(String symbol, BigDecimal currentPrice, BigDecimal availableQuantity) {
        try {
            // Calculate sell price with profit margin
            BigDecimal profitMultiplier = BigDecimal.ONE.add(PROFIT_MARGIN_PERCENTAGE.divide(new BigDecimal("100")));
            BigDecimal sellPrice = currentPrice.multiply(profitMultiplier)
                    .setScale(PRICE_PRECISION, RoundingMode.UP);

            log.info("Placing SELL order - Symbol: {}, Quantity: {}, Price: {}, Profit margin: {}%",
                    symbol, availableQuantity, sellPrice, PROFIT_MARGIN_PERCENTAGE);

            PlaceOrderDto sellOrder = new PlaceOrderDto(
                    symbol,
                    Side.SELL.getValue(),
                    OrderType.LIMIT.getValue(), // Using limit order to ensure profit margin
                    sellPrice.doubleValue(),
                    availableQuantity.doubleValue());

            orderService.placeOrder(sellOrder);

            log.info("SELL order placed successfully for {}", symbol);
            return "SELL_ORDER_PLACED: " + availableQuantity + " " + symbol + " at " + sellPrice;

        } catch (Exception e) {
            log.error("Failed to place sell order for {}: {}", symbol, e.getMessage(), e);
            return "SELL_ORDER_FAILED: " + e.getMessage();
        }
    }

    /**
     * Handles existing open orders
     */
    private String handleExistingOrders(String symbol, GetOpenOrdersResponse openOrders,
            BigDecimal currentPrice, BigDecimal baseBalance) {
        // TODO: Implement order management logic after fixing GetOpenOrdersResponse
        // structure
        // - Check if orders should be cancelled (price moved too far)
        // - Adjust stop-loss orders
        // - Update trailing stops

        // TODO: Fix GetOpenOrdersResponse method names
        /*
         * log.info("Managing {} existing orders for {}", openOrders.getOrders().size(),
         * symbol);
         * 
         * // For now, just monitor existing orders
         * openOrders.getOrders().forEach(order -> {
         * log.
         * info("Existing order - ID: {}, Side: {}, Type: {}, Price: {}, Quantity: {}",
         * order.getOrderId(), order.getSide(), order.getType(),
         * order.getPrice(), order.getOrigQty());
         * });
         * 
         * return "MONITORING_ORDERS: " + openOrders.getOrders().size() +
         * " active orders";
         */

        log.info("Checking existing orders for {}", symbol);
        return "MONITORING_ORDERS: Need to fix GetOpenOrdersResponse structure";
    }

    /**
     * Gets balance for a specific asset
     */
    private BigDecimal getAssetBalance(GetAccountResponse accountInfo, String asset) {
        if (accountInfo.getBalances() == null) {
            return BigDecimal.ZERO;
        }

        return accountInfo.getBalances().stream()
                .filter(balance -> asset.equals(balance.getAsset()))
                .findFirst()
                .map(balance -> new BigDecimal(balance.getFree()))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Extracts base asset from trading pair (e.g., "BTC" from "BTCUSDT")
     */
    private String extractBaseAsset(String symbol) {
        // TODO: Implement proper symbol parsing
        // This is a simplified version - should be more robust
        if (symbol.endsWith("USDT")) {
            return symbol.substring(0, symbol.length() - 4);
        } else if (symbol.endsWith("BTC")) {
            return symbol.substring(0, symbol.length() - 3);
        } else if (symbol.endsWith("ETH")) {
            return symbol.substring(0, symbol.length() - 3);
        }
        return symbol.substring(0, symbol.length() / 2); // Fallback
    }

    /**
     * Extracts quote asset from trading pair (e.g., "USDT" from "BTCUSDT")
     */
    private String extractQuoteAsset(String symbol) {
        // TODO: Implement proper symbol parsing
        if (symbol.endsWith("USDT")) {
            return "USDT";
        } else if (symbol.endsWith("BTC")) {
            return "BTC";
        } else if (symbol.endsWith("ETH")) {
            return "ETH";
        } else if (symbol.endsWith("BRL")) {
            return "BRL";
        }
        return "USDT"; // Default fallback
    }

    /**
     * Emergency stop - cancels all open orders for a symbol
     */
    public String emergencyStop(String symbol) {
        try {
            log.warn("EMERGENCY STOP triggered for {}", symbol);

            // TODO: Implement order cancellation when GetOpenOrdersResponse structure is
            // fixed
            // GetOpenOrdersResponse openOrders = orderService.getOpenOrders(symbol);

            log.info("Emergency stop requested for {} - need to implement proper order cancellation", symbol);
            return "EMERGENCY_STOP_REQUESTED: Need to fix GetOpenOrdersResponse structure for " + symbol;

        } catch (Exception e) {
            log.error("Emergency stop failed for {}: {}", symbol, e.getMessage(), e);
            return "EMERGENCY_STOP_FAILED: " + e.getMessage();
        }
    }
}
