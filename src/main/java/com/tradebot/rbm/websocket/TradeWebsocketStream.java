package com.tradebot.rbm.websocket;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.binance.connector.client.common.ApiException;
import com.binance.connector.client.common.websocket.service.StreamBlockingQueueWrapper;
import com.binance.connector.client.spot.websocket.stream.api.SpotWebSocketStreams;
import com.binance.connector.client.spot.websocket.stream.model.TradeRequest;
import com.binance.connector.client.spot.websocket.stream.model.TradeResponse;
import com.tradebot.rbm.service.WebsocketTradeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeWebsocketStream implements ApplicationRunner {

    @Value("${binance.trading.symbol:BTCUSDT}")
    private String tradingSymbol;

    public final static AtomicBoolean shouldListenToTrades = new AtomicBoolean(true);
    private final SpotWebSocketStreams spotWebSocketStreams;
    private final WebsocketTradeService websocketTradeService;

    @Override
    @Async
    public void run(ApplicationArguments args) {
        log.info("Starting Binance WebSocket connections for symbol: {}...", tradingSymbol);
        connect();
    }

    private void connect() {
        try {
            tradeListener();
        } catch (Exception e) {
            log.error("Failed to connect to WebSocket streams", e);
        }
    }

    private void tradeListener() throws ApiException, InterruptedException {
        TradeRequest tradeRequest = new TradeRequest();
        tradeRequest.symbol(tradingSymbol);
        StreamBlockingQueueWrapper<TradeResponse> response = spotWebSocketStreams.trade(tradeRequest);
        while (shouldListenToTrades.get()) {
            var tickerData = response.take();

            websocketTradeService.updateTrade(tickerData);
        }
    }
}
