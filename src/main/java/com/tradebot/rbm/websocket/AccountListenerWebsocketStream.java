package com.tradebot.rbm.websocket;

import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebot.rbm.websocket.dto.AccountStatusResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountListenerWebsocketStream {

    public static AccountStatusResponse accountStatus;

    @Value("${binance.trading.symbol:BTCUSDT}")
    private String tradingSymbol;

    public final static AtomicBoolean shouldListenToAccount = new AtomicBoolean(true);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void handleAccountStatusResponse(JSONObject response) {
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

    private static void handleAccountStatusResponseFallback(JSONObject response) {
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

    public static void handleCommissionRatesResponse(JSONObject response) {
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

}
