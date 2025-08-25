package com.tradebot.rbm.websocket;

import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.binance.connector.client.impl.WebSocketApiClientImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebot.rbm.websocket.dto.AccountStatusResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountListenerWebsocketStream implements ApplicationRunner {

    public static AccountStatusResponse accountStatus;

    @Value("${binance.trading.symbol:BTCUSDT}")
    private String tradingSymbol;

    public final static AtomicBoolean shouldListenToAccount = new AtomicBoolean(true);
    private final WebSocketApiClientImpl accountWebsocketClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Async
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting Account WebSocket stream...");
        connect();
    }

    private void connect() {
        try {
            // Connect to WebSocket API with message handler
            accountWebsocketClient.connect(
                    // onOpen callback
                    response -> {
                        log.info("Account WebSocket API connected successfully");
                        startAccountRequests();
                    },
                    // onMessage callback
                    message -> {
                        log.info("Account WebSocket message received: {}", message);
                        handleAccountMessage(message);
                    },
                    // onClosing callback
                    (code, reason) -> {
                        log.warn("Account WebSocket connection closing: {} - {}", code, reason);
                    },
                    // onClosed callback
                    (code, reason) -> {
                        log.warn("Account WebSocket connection closed: {} - {}", code, reason);
                        if (shouldListenToAccount.get()) {
                            // Attempt to reconnect after delay
                            scheduleReconnect();
                        }
                    },
                    // onFailure callback
                    (throwable, response) -> {
                        log.error("Account WebSocket connection failed", throwable);
                        if (shouldListenToAccount.get()) {
                            scheduleReconnect();
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to connect to Account WebSocket API", e);
        }
    }

    private void startAccountRequests() {
        if (!shouldListenToAccount.get()) {
            return;
        }

        try {
            // Example: Get account status
            JSONObject accountParams = new JSONObject();
            accountParams.put("requestId", "account_status_" + System.currentTimeMillis());
            accountParams.put("omitZeroBalances", true);
            accountWebsocketClient.account().accountStatus(accountParams);

            log.info("Account status request sent");

            // Example: Get commission rates for the trading symbol
            // JSONObject commissionParams = new JSONObject();
            // commissionParams.put("requestId", "commission_rates_" +
            // System.currentTimeMillis());
            // accountWebsocketClient.account().accountCommissionRates(tradingSymbol.toUpperCase(),
            // commissionParams);

            log.info("Commission rates request sent for symbol: {}", tradingSymbol);

        } catch (Exception e) {
            log.error("Error sending account requests", e);
        }
    }

    private void handleAccountMessage(String message) {
        try {
            // Parse and handle different types of account messages
            JSONObject jsonMessage = new JSONObject(message);

            if (jsonMessage.has("id")) {
                String requestId = jsonMessage.getString("id");
                log.info("Response for request ID: {}", requestId);

                if (requestId.startsWith("account_status")) {
                    handleAccountStatusResponse(jsonMessage);
                } else if (requestId.startsWith("commission_rates")) {
                    handleCommissionRatesResponse(jsonMessage);
                }
            }

        } catch (Exception e) {
            log.error("Error handling account message: {}", message, e);
        }
    }

    private void handleAccountStatusResponse(JSONObject response) {
        try {
            // Convert JSONObject to String and then to DTO
            String jsonString = response.toString();
            AccountListenerWebsocketStream.accountStatus = objectMapper.readValue(jsonString,
                    AccountStatusResponse.class);

            // Access the structured data
            AccountStatusResponse.AccountResult result = accountStatus.getResult();

            log.info("Account Status - Can Trade: {}, Can Withdraw: {}, Can Deposit: {}",
                    result.isCanTrade(),
                    result.isCanWithdraw(),
                    result.isCanDeposit());

            log.info("Account Type: {}, UID: {}, Update Time: {}",
                    result.getAccountType(),
                    result.getUid(),
                    result.getUpdateTime());

            // Log commission rates
            AccountStatusResponse.CommissionRates commissionRates = result.getCommissionRates();
            log.info("Commission Rates - Maker: {}, Taker: {}, Buyer: {}, Seller: {}",
                    commissionRates.getMaker(),
                    commissionRates.getTaker(),
                    commissionRates.getBuyer(),
                    commissionRates.getSeller());

            // Log balances with actual amounts
            if (result.getBalances() != null && !result.getBalances().isEmpty()) {
                log.info("Account balances (non-zero only):");
                result.getBalances().forEach(balance -> {
                    double freeAmount = Double.parseDouble(balance.getFree());
                    double lockedAmount = Double.parseDouble(balance.getLocked());
                    if (freeAmount > 0 || lockedAmount > 0) {
                        log.info("  {} - Free: {}, Locked: {}",
                                balance.getAsset(),
                                balance.getFree(),
                                balance.getLocked());
                    }
                });
            }

            // Log permissions
            if (result.getPermissions() != null && !result.getPermissions().isEmpty()) {
                log.info("Account permissions: {}", String.join(", ", result.getPermissions()));
            }

            // Log rate limits
            if (accountStatus.getRateLimits() != null && !accountStatus.getRateLimits().isEmpty()) {
                accountStatus.getRateLimits().forEach(rateLimit -> {
                    log.info("Rate Limit - Type: {}, Interval: {} {}, Limit: {}, Current: {}",
                            rateLimit.getRateLimitType(),
                            rateLimit.getIntervalNum(),
                            rateLimit.getInterval(),
                            rateLimit.getLimit(),
                            rateLimit.getCount());
                });
            }

        } catch (Exception e) {
            log.error("Error handling account status response", e);
            // Fallback to the original JSON parsing if DTO fails
            handleAccountStatusResponseFallback(response);
        }
    }

    private void handleAccountStatusResponseFallback(JSONObject response) {
        try {
            if (response.has("result")) {
                JSONObject result = response.getJSONObject("result");
                log.info("Account Status (Fallback) - Can Trade: {}, Can Withdraw: {}, Can Deposit: {}",
                        result.getBoolean("canTrade"),
                        result.getBoolean("canWithdraw"),
                        result.getBoolean("canDeposit"));

                if (result.has("balances")) {
                    log.info("Account balances received, count: {}", result.getJSONArray("balances").length());
                }
            }
        } catch (Exception e) {
            log.error("Error in fallback account status response handler", e);
        }
    }

    private void handleCommissionRatesResponse(JSONObject response) {
        try {
            if (response.has("result")) {
                JSONObject result = response.getJSONObject("result");
                log.info("Commission Rates for {} - Maker: {}, Taker: {}, Buyer: {}, Seller: {}",
                        result.getString("symbol"),
                        result.getString("standardCommission.maker"),
                        result.getString("standardCommission.taker"),
                        result.getString("standardCommission.buyer"),
                        result.getString("standardCommission.seller"));
            }
        } catch (Exception e) {
            log.error("Error handling commission rates response", e);
        }
    }

    private void scheduleReconnect() {
        // Simple reconnect logic - you can enhance this with exponential backoff
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds before reconnecting
                if (shouldListenToAccount.get()) {
                    log.info("Attempting to reconnect Account WebSocket...");
                    connect();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void disconnect() {
        shouldListenToAccount.set(false);
        if (accountWebsocketClient != null) {
            accountWebsocketClient.close();
        }
        log.info("Account WebSocket disconnected");
    }
}
