package com.tradebot.rbm.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tradebot.rbm.service.TradeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/trading")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TradingController {

    private final TradeService tradeService;

    /**
     * Execute algorithmic trading for a specific symbol
     * 
     * @param symbol Trading pair symbol (e.g., "BTCUSDT")
     * @return Trading result
     */
    @PostMapping("/execute/{symbol}")
    public ResponseEntity<String> executeTrade(@PathVariable String symbol) {
        try {
            log.info("Executing trade for symbol: {}", symbol);
            String result = tradeService.trade(symbol);

            if (result.startsWith("ERROR") || result.startsWith("FAILED")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing trade for {}: {}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("TRADE_EXECUTION_ERROR: " + e.getMessage());
        }
    }

    /**
     * Emergency stop - cancels all orders for a symbol
     * 
     * @param symbol Trading pair symbol
     * @return Emergency stop result
     */
    @PostMapping("/emergency-stop/{symbol}")
    public ResponseEntity<String> emergencyStop(@PathVariable String symbol) {
        try {
            log.warn("Emergency stop requested for symbol: {}", symbol);
            String result = tradeService.emergencyStop(symbol);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during emergency stop for {}: {}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("EMERGENCY_STOP_ERROR: " + e.getMessage());
        }
    }

}
