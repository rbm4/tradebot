package com.tradebot.rbm.component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.tradebot.rbm.entity.PriceDataEntity;
import com.tradebot.rbm.repository.PriceDataRepository;
import com.tradebot.rbm.utils.RecentTradeUtils;
import com.tradebot.rbm.utils.TechnicalAnalysisDemo;
import com.tradebot.rbm.utils.dto.stochasticOscilator.PriceBucket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecentTradesProcessor implements ApplicationRunner {
    private Thread processingThread;
    private final PriceDataRepository priceDataRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        processingThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000);
                    var newBucket = new PriceBucket(LocalDateTime.now());
                    var tradeInQuestion = RecentTradeUtils.recentTrades.poll();
                    while (tradeInQuestion != null) {
                        var tradeData = tradeInQuestion.getTrade();
                        var price = new BigDecimal(tradeData.getpLowerCase());
                        var qty = new BigDecimal(tradeData.getqLowerCase());
                        newBucket.addTrade(price, qty);
                        tradeInQuestion = RecentTradeUtils.recentTrades.poll();
                    }
                    priceDataRepository.save(new PriceDataEntity(newBucket));
                    var pageable = PageRequest.of(0, 20, Sort.by("id").descending());
                    var recenCandles = priceDataRepository.findAll(pageable);
                    TechnicalAnalysisDemo
                            .demonstrateStochasticAnalysis(recenCandles.map(PriceDataEntity::toPriceData).getContent());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error in processing trades", e);
                }
            }
        });
        processingThread.setName("RecentTradesProcessor");
        processingThread.setDaemon(true);
        processingThread.start();
    }

}
