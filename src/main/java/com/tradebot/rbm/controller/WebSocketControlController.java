package com.tradebot.rbm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tradebot.rbm.entity.dto.StreamStatusDTO;
import com.tradebot.rbm.websocket.AccountListenerWebsocketStream;
import com.tradebot.rbm.websocket.TickerWebsocketStream;
import com.tradebot.rbm.websocket.TradeWebsocketStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/websocket")
@RequiredArgsConstructor
@Slf4j
public class WebSocketControlController {

    /**
     * Control trade stream listener
     * POST /api/websocket/trade?enabled=true/false
     */
    @PostMapping("/trade")
    public ResponseEntity<String> controlTradeStream(@RequestParam boolean enabled) {
        TradeWebsocketStream.shouldListenToTrades.set(enabled);

        String status = enabled ? "enabled" : "disabled";
        String message = "Trade stream listener has been " + status;

        log.info("Trade stream listener {}", status);

        return ResponseEntity.ok(message);
    }

    /**
     * Control ticker stream listener
     * POST /api/websocket/ticker?enabled=true/false
     */
    @PostMapping("/ticker")
    public ResponseEntity<String> controlTickerStream(@RequestParam boolean enabled) {
        TradeWebsocketStream.shouldListenToTrades.set(enabled);

        String status = enabled ? "enabled" : "disabled";
        String message = "Ticker stream listener has been " + status;

        log.info("Ticker stream listener {}", status);

        return ResponseEntity.ok(message);
    }

    /**
     * Control all streams at once
     * POST /api/websocket/all?enabled=true/false
     */
    @PostMapping("/all")
    public ResponseEntity<String> controlAllStreams(@RequestParam boolean enabled) {
        TradeWebsocketStream.shouldListenToTrades.set(enabled);
        TickerWebsocketStream.shouldListenToTrades.set(enabled);
        AccountListenerWebsocketStream.shouldListenToAccount.set(enabled);

        String status = enabled ? "enabled" : "disabled";
        String message = "All stream listeners have been " + status;

        log.info("All stream listeners {}", status);

        return ResponseEntity.ok(message);
    }

    /**
     * Control account stream listener
     * POST /api/websocket/account?enabled=true/false
     */
    @PostMapping("/account")
    public ResponseEntity<String> controlAccountStream(@RequestParam boolean enabled) {
        AccountListenerWebsocketStream.shouldListenToAccount.set(enabled);

        String status = enabled ? "enabled" : "disabled";
        String message = "Account stream listener has been " + status;

        log.info("Account stream listener {}", status);

        return ResponseEntity.ok(message);
    }

    /**
     * Get current status of all streams
     * GET /api/websocket/status
     */
    @GetMapping("/status")
    public ResponseEntity<StreamStatusDTO> getStreamStatus() {
        StreamStatusDTO status = StreamStatusDTO.builder()
                .tradeStreamEnabled(TradeWebsocketStream.shouldListenToTrades.get())
                .tickerStreamEnabled(TickerWebsocketStream.shouldListenToTrades.get())
                .accountStreamEnabled(AccountListenerWebsocketStream.shouldListenToAccount.get())
                .accountStatusResponse(AccountListenerWebsocketStream.accountStatus)
                .build();

        return ResponseEntity.ok(status);
    }

    /**
     * Enable trade stream (convenience method)
     * POST /api/websocket/trade/enable
     */
    @PostMapping("/trade/enable")
    public ResponseEntity<String> enableTradeStream() {
        return controlTradeStream(true);
    }

    /**
     * Disable trade stream (convenience method)
     * POST /api/websocket/trade/disable
     */
    @PostMapping("/trade/disable")
    public ResponseEntity<String> disableTradeStream() {
        return controlTradeStream(false);
    }

    /**
     * Enable ticker stream (convenience method)
     * POST /api/websocket/ticker/enable
     */
    @PostMapping("/ticker/enable")
    public ResponseEntity<String> enableTickerStream() {
        return controlTickerStream(true);
    }

    /**
     * Disable ticker stream (convenience method)
     * POST /api/websocket/ticker/disable
     */
    @PostMapping("/ticker/disable")
    public ResponseEntity<String> disableTickerStream() {
        return controlTickerStream(false);
    }

    /**
     * Enable account stream (convenience method)
     * POST /api/websocket/account/enable
     */
    @PostMapping("/account/enable")
    public ResponseEntity<String> enableAccountStream() {
        return controlAccountStream(true);
    }

    /**
     * Disable account stream (convenience method)
     * POST /api/websocket/account/disable
     */
    @PostMapping("/account/disable")
    public ResponseEntity<String> disableAccountStream() {
        return controlAccountStream(false);
    }

}
