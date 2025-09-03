package com.tradebot.rbm.adapter;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.spot.rest.api.SpotRestApi;
import com.binance.connector.client.spot.rest.model.DepthResponse;
import com.binance.connector.client.spot.rest.model.GetAccountResponse;
import com.binance.connector.client.spot.rest.model.GetOpenOrdersResponse;
import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.NewOrderResponse;
import com.binance.connector.client.spot.rest.model.OrderOcoRequest;
import com.binance.connector.client.spot.rest.model.OrderOcoResponse;
import com.binance.connector.client.spot.rest.model.OrderType;
import com.binance.connector.client.spot.rest.model.Symbols;
import com.binance.connector.client.spot.rest.model.TickerBookTickerResponse;
import com.binance.connector.client.spot.rest.model.TickerType;
import com.binance.connector.client.spot.rest.model.TimeInForce;
import com.binance.connector.client.spot.rest.model.WindowSize;
import com.binance.connector.client.spot.websocket.api.api.SpotWebSocketApi;
import com.binance.connector.client.spot.websocket.api.model.OrderPlaceRequest;
import com.binance.connector.client.spot.websocket.api.model.OrderPlaceResponse;
import com.tradebot.rbm.entity.dto.PlaceOrderDto;
import com.tradebot.rbm.entity.dto.TickerDto;
import com.tradebot.rbm.utils.dto.PendingBuyOrderDTO;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BinanceAdapter {
    private final SpotClient spot;
    private final SpotRestApi spotRestApi;
    private final SpotWebSocketApi spotWebSocketApi;

    public BinanceAdapter(SpotClient spotC, SpotRestApi spotRestApi, SpotWebSocketApi spotWebSocketApi) {
        this.spot = spotC;
        this.spotRestApi = spotRestApi;
        this.spotWebSocketApi = spotWebSocketApi;
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
        req.setQuantity(order.getAmount());
        req.timeInForce(TimeInForce.GTC);
        if (order.getType() == OrderType.STOP_LOSS_LIMIT || order.getType() == OrderType.STOP_LOSS) {

            req.setStopPrice(order.getStop());
        }

        var response = spotRestApi.newOrder(req);
        return response.getData();
    }

    public void placeWsOrder(OrderPlaceRequest order, PendingBuyOrderDTO pendingOrder) {
        CompletableFuture<OrderPlaceResponse> future = spotWebSocketApi.orderPlace(order);
        future.handle(
                (response, error) -> {
                    if (response.getError() != null) {
                        System.err.println(response.getError());
                    }
                    System.out.println(response);
                    pendingOrder.setBinanceOrderId(response.getResult().getOrderId());
                    return response;
                });

    }

    public OrderOcoResponse placeOcoOrder(OrderOcoRequest order) {
        var response = spotRestApi.orderOco(order);
        return response.getData();
    }

    public ApiResponse<TickerBookTickerResponse> tickerBookTicker(String symbol) {
        return spotRestApi.tickerBookTicker(symbol, null);
    }

    public ApiResponse<DepthResponse> depth(String symbol, Integer limit) {
        return spotRestApi.depth(symbol, limit);
    }
}
