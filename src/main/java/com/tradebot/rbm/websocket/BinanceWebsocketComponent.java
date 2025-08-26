package com.tradebot.rbm.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.binance.connector.client.common.configuration.SignatureConfiguration;
import com.binance.connector.client.common.websocket.configuration.WebSocketClientConfiguration;
import com.binance.connector.client.impl.WebSocketApiClientImpl;
import com.binance.connector.client.spot.websocket.stream.SpotWebSocketStreamsUtil;
import com.binance.connector.client.spot.websocket.stream.api.SpotWebSocketStreams;
import com.binance.connector.client.utils.signaturegenerator.HmacSignatureGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BinanceWebsocketComponent {
    private final SignatureConfiguration signatureConfig;

    @Bean
    public SpotWebSocketStreams initSpotStream() {
        WebSocketClientConfiguration clientConfiguration = SpotWebSocketStreamsUtil.getClientConfiguration();
        clientConfiguration.setUsePool(true);
        clientConfiguration.setSignatureConfiguration(signatureConfig);

        return new SpotWebSocketStreams(clientConfiguration);
    }

    @Bean
    public WebSocketApiClientImpl initAccountWebsocketStream() {
        // Create signature generator from the signature configuration
        HmacSignatureGenerator signatureGenerator = new HmacSignatureGenerator(signatureConfig.getSecretKey());

        // Create WebSocket API client with API key and signature generator
        WebSocketApiClientImpl webSocketApiClient = new WebSocketApiClientImpl(
                signatureConfig.getApiKey(),
                signatureGenerator);

        log.info("Account WebSocket API client initialized");

        return webSocketApiClient;
    }

}
