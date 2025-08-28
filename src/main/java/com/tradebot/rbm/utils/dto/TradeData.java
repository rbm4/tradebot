package com.tradebot.rbm.utils.dto;

import java.time.LocalDateTime;

import com.binance.connector.client.spot.websocket.stream.model.TradeResponse;

import lombok.Data;

/**
 * Internal class to store trade data with timestamp
 */
@Data
public class TradeData {
    final TradeResponse trade;
    final LocalDateTime timestamp;

    public TradeData(TradeResponse trade) {
        this.trade = trade;
        this.timestamp = LocalDateTime.now();
    }
}
