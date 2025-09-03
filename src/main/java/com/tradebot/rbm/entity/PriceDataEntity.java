package com.tradebot.rbm.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tradebot.rbm.utils.dto.stochasticOscilator.PriceBucket;
import com.tradebot.rbm.utils.dto.stochasticOscilator.PriceData;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "priceData", schema = "tradebot")
@NoArgsConstructor
@AllArgsConstructor
public class PriceDataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private BigDecimal open;
    @NotNull
    private BigDecimal high;
    @NotNull
    private BigDecimal low;
    @NotNull
    private BigDecimal close;
    @NotNull
    private BigDecimal volume;
    @NotNull
    private int tradeCount;
    @NotNull
    private LocalDateTime timestamp;

    public PriceDataEntity(PriceBucket bucket) {
        this.open = bucket.getOpen();
        this.high = bucket.getHigh();
        this.low = bucket.getLow();
        this.close = bucket.getClose();
        this.volume = bucket.getVolume();
        this.tradeCount = bucket.getTradeCount();
        this.timestamp = bucket.getTimestamp();
    }

    public static PriceData toPriceData(PriceDataEntity entity) {
        return PriceData.builder()
                .close(entity.getClose())
                .high(entity.getHigh())
                .low(entity.getLow())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
