package com.tradebot.rbm.utils;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import com.binance.connector.client.spot.websocket.stream.model.TradeResponse;
import com.tradebot.rbm.utils.dto.TradeData;

public class RecentTradeUtils {
    public static final ConcurrentLinkedQueue<TradeData> recentTrades = new ConcurrentLinkedQueue<>();
    public static final AtomicReference<TradeResponse> lastTrade = new AtomicReference<>();

}
