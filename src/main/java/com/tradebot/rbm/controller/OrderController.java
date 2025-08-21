package com.tradebot.rbm.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.binance.connector.client.spot.rest.model.GetOpenOrdersResponse;
import com.tradebot.rbm.entity.OrderEntity;
import com.tradebot.rbm.entity.dto.PlaceOrderDto;
import com.tradebot.rbm.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/openOrders/{symbol}")
    public ResponseEntity<GetOpenOrdersResponse> getOpenOrders(@PathVariable String symbol) {
        return ResponseEntity.ok(orderService.getOpenOrders(symbol));
    }

    @DeleteMapping("/deleteOrder/{symbol}/{id}")
    public ResponseEntity<Void> deleteOrderBinance(@PathVariable String symbol, @PathVariable Long id) {
        orderService.deleteBinanceOrder(symbol, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/placeOrder")
    public ResponseEntity<Void> placeOrder(@RequestBody PlaceOrderDto order) {
        orderService.placeOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping
    public ResponseEntity<OrderEntity> createOrder(@RequestBody OrderEntity order) {
        try {
            OrderEntity createdOrder = orderService.createOrder(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/market")
    public ResponseEntity<OrderEntity> createMarketOrder(
            @RequestParam String symbol,
            @RequestParam OrderEntity.OrderSide side,
            @RequestParam BigDecimal quantity) {
        try {
            OrderEntity order = orderService.createMarketOrder(symbol, side, quantity);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (Exception e) {
            log.error("Error creating market order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/limit")
    public ResponseEntity<OrderEntity> createLimitOrder(
            @RequestParam String symbol,
            @RequestParam OrderEntity.OrderSide side,
            @RequestParam BigDecimal quantity,
            @RequestParam BigDecimal price) {
        try {
            OrderEntity order = orderService.createLimitOrder(symbol, side, quantity, price);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (Exception e) {
            log.error("Error creating limit order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderEntity>> getAllOrders() {
        try {
            List<OrderEntity> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching all orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderEntity> getOrderById(@PathVariable Long id) {
        try {
            Optional<OrderEntity> order = orderService.getOrderById(id);
            return order.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching order by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/binance/{binanceOrderId}")
    public ResponseEntity<OrderEntity> getOrderByBinanceOrderId(@PathVariable String binanceOrderId) {
        try {
            Optional<OrderEntity> order = orderService.getOrderByBinanceOrderId(binanceOrderId);
            return order.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching order by Binance Order ID {}: {}", binanceOrderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<OrderEntity>> getOrdersBySymbol(@PathVariable String symbol) {
        try {
            List<OrderEntity> orders = orderService.getOrdersBySymbol(symbol);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching orders by symbol {}: {}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderEntity>> getOrdersByStatus(@PathVariable OrderEntity.OrderStatus status) {
        try {
            List<OrderEntity> orders = orderService.getOrdersByStatus(status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching orders by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/symbol/{symbol}/status/{status}")
    public ResponseEntity<List<OrderEntity>> getOrdersBySymbolAndStatus(
            @PathVariable String symbol,
            @PathVariable OrderEntity.OrderStatus status) {
        try {
            List<OrderEntity> orders = orderService.getOrdersBySymbolAndStatus(symbol, status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching orders by symbol {} and status {}: {}", symbol, status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<OrderEntity>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<OrderEntity> orders = orderService.getOrdersByDateRange(startDate, endDate);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching orders by date range: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<List<OrderEntity>> getLatestOrders() {
        try {
            List<OrderEntity> orders = orderService.getLatestOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching latest orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderEntity> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderEntity.OrderStatus status) {
        try {
            OrderEntity updatedOrder = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/execution")
    public ResponseEntity<OrderEntity> updateOrderExecution(
            @PathVariable Long id,
            @RequestParam BigDecimal executedQuantity,
            @RequestParam BigDecimal executedPrice,
            @RequestParam(required = false) BigDecimal commission,
            @RequestParam(required = false) String commissionAsset) {
        try {
            OrderEntity updatedOrder = orderService.updateOrderExecution(
                    id, executedQuantity, executedPrice, commission, commissionAsset);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            log.error("Error updating order execution: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating order execution: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting order: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/count/status/{status}")
    public ResponseEntity<Long> countOrdersByStatus(@PathVariable OrderEntity.OrderStatus status) {
        try {
            Long count = orderService.countOrdersByStatus(status);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error counting orders by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
