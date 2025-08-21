package com.tradebot.rbm.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.binance.connector.client.spot.rest.model.WindowSize;
import com.tradebot.rbm.entity.dto.TickerDto;
import com.tradebot.rbm.service.SpotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/spot")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SpotController {
    private final SpotService spotService;

    @GetMapping(path = "/ticker/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TickerDto> ticker(@PathVariable("symbol") String symbol) {
        return ResponseEntity.ok(spotService.ticker(symbol, WindowSize.WINDOW_SIZE_10h));
    }

}
