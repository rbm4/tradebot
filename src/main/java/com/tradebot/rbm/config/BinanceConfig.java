package com.tradebot.rbm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.common.configuration.SignatureConfiguration;
import com.binance.connector.client.common.websocket.configuration.WebSocketClientConfiguration;
import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.client.spot.rest.SpotRestApiUtil;
import com.binance.connector.client.spot.rest.api.SpotRestApi;
import com.binance.connector.client.spot.rest.model.ExchangeInfoResponse;
import com.binance.connector.client.spot.rest.model.Permissions;
import com.binance.connector.client.spot.rest.model.Symbols;
import com.binance.connector.client.spot.websocket.api.SpotWebSocketApiUtil;
import com.binance.connector.client.spot.websocket.api.api.SpotWebSocketApi;

@Configuration
public class BinanceConfig {
    @Value("${binance.secret}")
    private String secret;
    @Value("${binance.key}")
    private String key;
    @Value("${binance.trading.symbol:BTCUSDT}")
    private String tradingSymbol;
    @Value("${binance.spotWsKey}")
    private String spotWsKey;
    @Value("${binance.spotWsLoc}")
    private String spotWsLoc;

    @Bean
    public SpotClient binanceSpotClient() {
        return new SpotClientImpl(key, secret);
    }

    @Bean
    public SpotRestApi binanceSpotRestClient() {
        return new SpotRestApi(getConfig());
    }

    @Bean
    public ExchangeInfoResponse tradingSymbol() {
        var symbol = tradingSymbol.toUpperCase();
        Symbols symbols = null;
        Permissions permissions = null;
        Boolean showPermissionSets = true;
        ApiResponse<ExchangeInfoResponse> response = binanceSpotRestClient().exchangeInfo(
                symbol, symbols, permissions, showPermissionSets, null);
        return response.getData();
    }

    @Bean
    public SpotWebSocketApi spotWebSocketApi() {
        return new SpotWebSocketApi(getWsConfig());
    }

    @Bean
    public SignatureConfiguration signatureConfiguration() {
        SignatureConfiguration signatureConfiguration = new SignatureConfiguration();
        signatureConfiguration.setApiKey(key);
        signatureConfiguration.setSecretKey(secret);
        return signatureConfiguration;
    }

    public WebSocketClientConfiguration getWsConfig() {
        WebSocketClientConfiguration clientConfiguration = SpotWebSocketApiUtil.getClientConfiguration();
        // if you want the connection to be auto logged on:
        // https://developers.binance.com/docs/binance-spot-api-docs/websocket-api/authentication-requests
        clientConfiguration.setAutoLogon(true);
        var signatureConfiguration = new SignatureConfiguration();
        signatureConfiguration.setApiKey(spotWsKey);
        signatureConfiguration.setPrivateKey(spotWsLoc);
        clientConfiguration.setSignatureConfiguration(signatureConfiguration);
        return clientConfiguration;
    }

    public ClientConfiguration getConfig() {
        ClientConfiguration clientConfiguration = SpotRestApiUtil.getClientConfiguration();
        SignatureConfiguration signatureConfiguration = getSignatureConfiguration();
        clientConfiguration.setSignatureConfiguration(signatureConfiguration);
        return clientConfiguration;
    }

    private SignatureConfiguration getSignatureConfiguration() {
        SignatureConfiguration signatureConfiguration = new SignatureConfiguration();
        signatureConfiguration.setApiKey(key);
        signatureConfiguration.setSecretKey(secret);
        return signatureConfiguration;
    }

}
