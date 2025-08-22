package com.tradebot.rbm.adapter;

import org.springframework.stereotype.Component;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.spot.rest.api.SpotRestApi;
import com.binance.connector.client.spot.rest.model.GetAccountResponse;
import com.binance.connector.client.spot.rest.model.GetOpenOrdersResponse;
import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.NewOrderResponse;
import com.binance.connector.client.spot.rest.model.Symbols;
import com.binance.connector.client.spot.rest.model.TickerBookTickerResponse;
import com.binance.connector.client.spot.rest.model.TickerType;
import com.binance.connector.client.spot.rest.model.WindowSize;
import com.tradebot.rbm.entity.dto.PlaceOrderDto;
import com.tradebot.rbm.entity.dto.TickerDto;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BinanceAdapter {
    private final SpotClient spot;
    private final SpotRestApi spotRestApi;

    public BinanceAdapter(SpotClient spotC, SpotRestApi spotRestApi) {
        this.spot = spotC;
        this.spotRestApi = spotRestApi;
    }

    public TickerDto ticker(String pair, WindowSize windowSize) {
        var symbol = pair;
        Symbols symbols = null;
        var type = TickerType.FULL;
        var ticker = spotRestApi.ticker(symbol, symbols, windowSize, type);
        if (ticker == null || ticker.getData() == null || ticker.getData().getTickerResponse1() == null
                || Double.parseDouble(ticker.getData().getTickerResponse1().getVolume()) == 0) {
            log.error("Failed to retrieve ticker information for {}", symbol);
            throw new RuntimeException("FAILED: Unable to get proper ticker info");
        }

        return new TickerDto(ticker.getData().getTickerResponse1());
    }

    public GetAccountResponse accountInfo() {
        var accInfo = spotRestApi.getAccount(true, null);
        return accInfo.getData();
    }

    public GetOpenOrdersResponse openOrders(String symbol) {
        var orders = spotRestApi.getOpenOrders(symbol, null);

        return orders.getData();
    }

    public void cancelOrder(String symbol, Long id) {
        spotRestApi.deleteOrder(symbol, id, null, null, null, null);
    }

    public NewOrderResponse placeOrder(PlaceOrderDto order) {
        var req = new NewOrderRequest();
        req.setSymbol(order.getTicker());
        req.setSide(order.getSide());
        req.setType(order.getType());
        req.setPrice(order.getPrice());
        var response = spotRestApi.newOrder(req);
        return response.getData();
    }

    public ApiResponse<TickerBookTickerResponse> tickerBookTicker(String symbol) {
        return spotRestApi.tickerBookTicker(symbol, null);
    }
}
