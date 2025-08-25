package com.tradebot.rbm.websocket.dto;

import java.util.List;

import lombok.Data;

@Data
public class AccountStatusResponse {
    private String id;
    private int status;
    private AccountResult result;
    private List<RateLimit> rateLimits;

    @Data
    public static class AccountResult {
        private int makerCommission;
        private int takerCommission;
        private int buyerCommission;
        private int sellerCommission;
        private CommissionRates commissionRates;
        private boolean canTrade;
        private boolean canWithdraw;
        private boolean canDeposit;
        private boolean brokered;
        private boolean requireSelfTradePrevention;
        private boolean preventSor;
        private long updateTime;
        private String accountType;
        private List<Balance> balances;
        private List<String> permissions;
        private long uid;
    }

    @Data
    public static class CommissionRates {
        private String maker;
        private String taker;
        private String buyer;
        private String seller;
    }

    @Data
    public static class Balance {
        private String asset;
        private String free;
        private String locked;
    }

    @Data
    public static class RateLimit {
        private String rateLimitType;
        private String interval;
        private int intervalNum;
        private int limit;
        private int count;
    }
}
