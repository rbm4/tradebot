package com.tradebot.rbm.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.binance.connector.client.spot.rest.model.GetOpenOrdersResponse;
import com.tradebot.rbm.adapter.BinanceAdapter;
import com.tradebot.rbm.entity.OrderEntity;
import com.tradebot.rbm.entity.dto.PlaceOrderDto;
import com.tradebot.rbm.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final BinanceAdapter binanceAdapter;

    public OrderEntity createOrder(OrderEntity order) {
        log.info("Creating new order for symbol: {}, side: {}, quantity: {}",
                order.getSymbol(), order.getSide(), order.getQuantity());

        OrderEntity savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());
        return savedOrder;
    }

    public OrderEntity createMarketOrder(String symbol, OrderEntity.OrderSide side, BigDecimal quantity) {
        OrderEntity order = new OrderEntity();
        order.setSymbol(symbol);
        order.setSide(side);
        order.setType(OrderEntity.OrderType.MARKET);
        order.setQuantity(quantity.toString());
        order.setStatus(OrderEntity.OrderStatus.NEW);

        return createOrder(order);
    }

    public OrderEntity createLimitOrder(String symbol, OrderEntity.OrderSide side,
            BigDecimal quantity, BigDecimal price) {
        OrderEntity order = new OrderEntity();
        order.setSymbol(symbol);
        order.setSide(side);
        order.setType(OrderEntity.OrderType.LIMIT);
        order.setQuantity(quantity.toString());
        order.setPrice(price.toString());
        order.setStatus(OrderEntity.OrderStatus.NEW);

        return createOrder(order);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getAllOrders() {
        log.info("Fetching all orders");
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<OrderEntity> getOrderById(Long id) {
        log.info("Fetching order with ID: {}", id);
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<OrderEntity> getOrderByBinanceOrderId(String binanceOrderId) {
        log.info("Fetching order with Binance Order ID: {}", binanceOrderId);
        return orderRepository.findByBinanceOrderId(binanceOrderId);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getOrdersBySymbol(String symbol) {
        log.info("Fetching orders for symbol: {}", symbol);
        return orderRepository.findBySymbol(symbol);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getOrdersByStatus(OrderEntity.OrderStatus status) {
        log.info("Fetching orders with status: {}", status);
        return orderRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getOrdersBySymbolAndStatus(String symbol, OrderEntity.OrderStatus status) {
        log.info("Fetching orders for symbol: {} with status: {}", symbol, status);
        return orderRepository.findBySymbolAndStatus(symbol, status);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching orders between {} and {}", startDate, endDate);
        return orderRepository.findOrdersByDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getLatestOrders() {
        log.info("Fetching latest 10 orders");
        return orderRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public OrderEntity updateOrderStatus(Long orderId, OrderEntity.OrderStatus status) {
        log.info("Updating order {} status to {}", orderId, status);

        Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            OrderEntity order = orderOpt.get();
            order.setStatus(status);
            OrderEntity updatedOrder = orderRepository.save(order);
            log.info("Order status updated successfully");
            return updatedOrder;
        } else {
            log.warn("Order with ID {} not found", orderId);
            throw new RuntimeException("Order not found with ID: " + orderId);
        }
    }

    public OrderEntity updateOrderExecution(Long orderId, BigDecimal executedQuantity,
            BigDecimal executedPrice, BigDecimal commission,
            String commissionAsset) {
        log.info("Updating order {} execution details", orderId);

        Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            OrderEntity order = orderOpt.get();
            // order.setExecutedQuantity(executedQuantity);
            // order.setExecutedPrice(executedPrice);
            // order.setCommission(commission);
            order.setCommissionAsset(commissionAsset);

            // Update status based on execution
            if (executedQuantity.compareTo(new BigDecimal(order.getQuantity())) >= 0) {
                order.setStatus(OrderEntity.OrderStatus.FILLED);
            } else if (executedQuantity.compareTo(BigDecimal.ZERO) > 0) {
                order.setStatus(OrderEntity.OrderStatus.PARTIALLY_FILLED);
            }

            OrderEntity updatedOrder = orderRepository.save(order);
            log.info("Order execution details updated successfully");
            return updatedOrder;
        } else {
            log.warn("Order with ID {} not found", orderId);
            throw new RuntimeException("Order not found with ID: " + orderId);
        }
    }

    public void deleteOrder(Long orderId) {
        log.info("Deleting order with ID: {}", orderId);
        if (orderRepository.existsById(orderId)) {
            orderRepository.deleteById(orderId);
            log.info("Order deleted successfully");
        } else {
            log.warn("Order with ID {} not found", orderId);
            throw new RuntimeException("Order not found with ID: " + orderId);
        }
    }

    @Transactional(readOnly = true)
    public Long countOrdersByStatus(OrderEntity.OrderStatus status) {
        log.info("Counting orders with status: {}", status);
        return orderRepository.countByStatus(status);
    }

    public GetOpenOrdersResponse getOpenOrders(String symbol) {
        return binanceAdapter.openOrders(symbol);
    }

    public void deleteBinanceOrder(String symbol, Long id) {
        binanceAdapter.cancelOrder(symbol, id);
    }

    public void placeOrder(PlaceOrderDto order) {
        var resultingOrder = binanceAdapter.placeOrder(order);
        log.info("Order placed successfully: {}", resultingOrder.toJson());
        // Save the order details to the database
        // orderRepository.save(new OrderEntity(resultingOrder));
    }
}
