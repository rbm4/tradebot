package com.tradebot.rbm.service;

import org.springframework.stereotype.Service;

import com.binance.connector.client.spot.rest.model.GetAccountResponse;
import com.tradebot.rbm.adapter.BinanceAdapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
    private final BinanceAdapter binanceAdapter;

    public GetAccountResponse accountInfo() {
        var accountInfo = binanceAdapter.accountInfo();
        return accountInfo;
    }

}
