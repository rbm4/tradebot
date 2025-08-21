package com.tradebot.rbm.entity.dto;

import com.binance.connector.client.spot.rest.model.OrderType;
import com.binance.connector.client.spot.rest.model.Side;

import lombok.Data;

@Data
public class PlaceOrderDto {
    private String ticker;
    private Side side;
    private OrderType type;
    private double price;
    private double amount;

    public PlaceOrderDto() {
    }

    public PlaceOrderDto(String ticker, String side, String type, double price, double amount) {
        this.ticker = ticker;
        this.side = Side.fromValue(side);
        this.type = OrderType.fromValue(type);
        this.price = price;
        this.amount = amount;
    }

}
