package com.tradebot.rbm.service;

import org.springframework.stereotype.Service;

import com.binance.connector.client.spot.rest.model.TickerBookTickerResponse1;
import com.binance.connector.client.spot.rest.model.WindowSize;
import com.tradebot.rbm.adapter.BinanceAdapter;
import com.tradebot.rbm.entity.dto.TickerDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotService {
    private final BinanceAdapter binanceAdapter;

    public TickerDto ticker(String symbol, WindowSize windowSize) {
        return binanceAdapter.ticker(symbol, windowSize);
    }

    public TickerBookTickerResponse1 getBookTicker(String symbol) {
        var book = binanceAdapter.tickerBookTicker(symbol);
        return book.getData().getTickerBookTickerResponse1();
    }

}
