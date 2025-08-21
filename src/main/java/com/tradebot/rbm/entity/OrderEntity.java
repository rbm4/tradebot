package com.tradebot.rbm.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.binance.connector.client.spot.rest.model.NewOrderResponse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders", schema = "tradebot")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "binance_order_id", unique = true)
    private String binanceOrderId;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "side", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderSide side;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderType type;

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    private String quantity;

    @Column(name = "price", precision = 20, scale = 8)
    private String price;

    @Column(name = "stop_price", precision = 20, scale = 8)
    private BigDecimal stopPrice;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "executed_quantity", precision = 20, scale = 8)
    private String executedQuantity;

    @Column(name = "executed_price", precision = 20, scale = 8)
    private String executedPrice;

    @Column(name = "commission", precision = 20, scale = 8)
    private String commission;

    @Column(name = "commission_asset")
    private String commissionAsset;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "binance_created_time")
    private LocalDateTime binanceCreatedTime;

    @Column(name = "binance_updated_time")
    private LocalDateTime binanceUpdatedTime;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public OrderEntity(NewOrderResponse resultingOrder) {
        this.binanceOrderId = resultingOrder.getOrderId() + "";
        this.symbol = resultingOrder.getSymbol();
        this.side = OrderSide.valueOf(resultingOrder.getSide().toUpperCase());
        this.type = OrderType.valueOf(resultingOrder.getType().toUpperCase());
        this.quantity = resultingOrder.getOrigQty();
        this.price = resultingOrder.getPrice();
        // this.stopPrice = resultingOrder.getStopPrice();
        this.status = OrderStatus.valueOf(resultingOrder.getStatus().toUpperCase());
        this.executedQuantity = resultingOrder.getExecutedQty();
        // this.executedPrice = resultingOrder.getExecutedPrice();
        // this.commission = resultingOrder.getCommission();
        // this.commissionAsset = resultingOrder.getCommissionAsset();
        this.binanceCreatedTime = LocalDateTime.now();
        this.binanceUpdatedTime = LocalDateTime.now();
    }

    public enum OrderSide {
        BUY, SELL
    }

    public enum OrderType {
        MARKET, LIMIT, STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT, TAKE_PROFIT_LIMIT
    }

    public enum OrderStatus {
        NEW, PARTIALLY_FILLED, FILLED, CANCELED, PENDING_CANCEL, REJECTED, EXPIRED
    }
}
