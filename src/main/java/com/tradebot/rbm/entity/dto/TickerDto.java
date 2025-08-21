package com.tradebot.rbm.entity.dto;

import com.binance.connector.client.spot.rest.model.TickerBookTickerResponse1;
import com.binance.connector.client.spot.rest.model.TickerResponse1;

import lombok.Data;

@Data
public class TickerDto {
    private TickerResponse1 ticker;
    private TickerBookTickerResponse1 bookTicker;

    public TickerDto(TickerResponse1 ticker, TickerBookTickerResponse1 bookTicker) {
        this.ticker = ticker;
        this.bookTicker = bookTicker;
    }

}
