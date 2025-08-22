package com.tradebot.rbm.entity.dto;

import com.binance.connector.client.spot.rest.model.TickerResponse1;

import lombok.Data;

@Data
public class TickerDto {
    private TickerResponse1 ticker;

    public TickerDto(TickerResponse1 ticker) {
        this.ticker = ticker;
    }

}
