package com.tradebot.rbm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tradebot.rbm.entity.PriceDataEntity;

public interface PriceDataRepository extends JpaRepository<PriceDataEntity, Long> {

}
