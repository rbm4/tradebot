package com.tradebot.rbm.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.binance.connector.client.spot.rest.model.GetAccountResponse;
import com.tradebot.rbm.service.WalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class WalletController {
    private final WalletService walletService;

    @GetMapping(path = "/accountInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GetAccountResponse> accountInfo() {
        return ResponseEntity.ok(walletService.accountInfo());
    }
}
