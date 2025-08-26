package com.tradebot.rbm.entity.dto;

import com.tradebot.rbm.websocket.dto.AccountStatusResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StreamStatusDTO {
    private boolean tradeStreamEnabled;
    private boolean tickerStreamEnabled;
    private boolean accountStreamEnabled;
    private AccountStatusResponse accountStatusResponse;

}
