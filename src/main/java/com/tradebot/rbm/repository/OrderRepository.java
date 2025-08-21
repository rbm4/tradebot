package com.tradebot.rbm.repository;

import com.tradebot.rbm.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByBinanceOrderId(String binanceOrderId);

    List<OrderEntity> findBySymbol(String symbol);

    List<OrderEntity> findBySymbolAndSide(String symbol, OrderEntity.OrderSide side);

    List<OrderEntity> findByStatus(OrderEntity.OrderStatus status);

    List<OrderEntity> findBySymbolAndStatus(String symbol, OrderEntity.OrderStatus status);

    @Query("SELECT o FROM OrderEntity o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<OrderEntity> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM OrderEntity o WHERE o.symbol = :symbol AND o.createdAt BETWEEN :startDate AND :endDate")
    List<OrderEntity> findOrdersBySymbolAndDateRange(@Param("symbol") String symbol,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = :status")
    Long countByStatus(@Param("status") OrderEntity.OrderStatus status);

    @Query("SELECT o FROM OrderEntity o WHERE o.symbol = :symbol ORDER BY o.createdAt DESC")
    List<OrderEntity> findLatestOrdersBySymbol(@Param("symbol") String symbol);

    List<OrderEntity> findTop10ByOrderByCreatedAtDesc();
}
