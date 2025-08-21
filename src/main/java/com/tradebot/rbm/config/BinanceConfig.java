package com.tradebot.rbm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.common.configuration.SignatureConfiguration;
import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.client.spot.rest.SpotRestApiUtil;
import com.binance.connector.client.spot.rest.api.SpotRestApi;

@Configuration
public class BinanceConfig {
    @Value("${binance.secret}")
    private String secret;
    @Value("${binance.key}")
    private String key;

    @Bean
    public SpotClient binanceSpotClient() {
        return new SpotClientImpl(key, secret);
    }

    @Bean
    public SpotRestApi binanceSpotRestClient() {
        return new SpotRestApi(getConfig());
    }

    public ClientConfiguration getConfig() {
        ClientConfiguration clientConfiguration = SpotRestApiUtil.getClientConfiguration();
        SignatureConfiguration signatureConfiguration = new SignatureConfiguration();
        signatureConfiguration.setApiKey(key);
        signatureConfiguration.setSecretKey(secret);
        clientConfiguration.setSignatureConfiguration(signatureConfiguration);
        return clientConfiguration;
    }

}
