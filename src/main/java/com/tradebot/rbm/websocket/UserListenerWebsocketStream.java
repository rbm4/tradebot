package com.tradebot.rbm.websocket;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.binance.connector.client.impl.WebSocketApiClientImpl;
import com.binance.connector.client.impl.websocketapi.WebSocketApiUserDataStream;
import com.binance.connector.client.spot.websocket.stream.api.SpotWebSocketStreams;
import com.tradebot.rbm.service.WebsocketTradeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserListenerWebsocketStream implements ApplicationRunner {

    @Value("${binance.trading.symbol:BTCUSDT}")
    private String tradingSymbol;

    public static final AtomicBoolean shouldListenToUserData = new AtomicBoolean(true);
    private final WebSocketApiClientImpl userDataWebsocketClient;
    private final WebsocketTradeService websocketTradeService;
    private final SpotWebSocketStreams spotWebSocketStreams;

    // Store the listen key for pinging and closing the stream
    private final AtomicReference<String> currentListenKey = new AtomicReference<>();
    private volatile boolean isStreamActive = true;

    protected boolean userDataStreamSubscribed;

    @Override
    @Async
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting User Data WebSocket stream...");
        connect();
    }

    private void connect() {
        try {
            // Connect to WebSocket API with message handler
            userDataWebsocketClient.connect(
                    // onOpen callback
                    response -> {
                        log.info("User Data WebSocket API connected successfully");
                        startUserDataStream();
                    },
                    // onMessage callback
                    message -> {
                        log.debug("User Data WebSocket message received: {}", message);
                        handleUserDataMessage(message);
                    },
                    // onClosing callback
                    (code, reason) -> {
                        log.warn("User Data WebSocket connection closing: {} - {}", code, reason);
                        isStreamActive = false;
                    },
                    // onClosed callback
                    (code, reason) -> {
                        log.warn("User Data WebSocket connection closed: {} - {}", code, reason);
                        isStreamActive = false;
                        if (shouldListenToUserData.get()) {
                            scheduleReconnect();
                        }
                    },
                    // onFailure callback
                    (throwable, response) -> {
                        log.error("User Data WebSocket connection failed", throwable);
                        isStreamActive = false;
                        if (shouldListenToUserData.get()) {
                            scheduleReconnect();
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to connect to User Data WebSocket API", e);
        }
    }

    private void startUserDataStream() {
        if (!shouldListenToUserData.get()) {
            return;
        }

        try {
            // Get the WebSocketApiUserDataStream module
            WebSocketApiUserDataStream userDataStream = (WebSocketApiUserDataStream) userDataWebsocketClient
                    .userDataStream();

            // Create parameters for starting the user data stream
            JSONObject startParams = new JSONObject();
            startParams.put("requestId", "start_user_data_" + System.currentTimeMillis());

            // Start the user data stream to get a listen key
            userDataStream.userDataStreamStart(startParams);

            log.info("User data stream start request sent");

            // Start the ping scheduler to keep the stream alive
            startPingScheduler();

            JSONObject accountParams = new JSONObject();
            accountParams.put("requestId", "account_status_" + System.currentTimeMillis());
            accountParams.put("omitZeroBalances", true);
            userDataWebsocketClient.account().accountStatus(accountParams);

        } catch (Exception e) {
            log.error("Error starting user data stream", e);
        }
    }

    private void handleUserDataMessage(String message) {
        try {
            JSONObject jsonMessage = new JSONObject(message);

            // Handle response to userDataStream.start request
            if (jsonMessage.has("id") && jsonMessage.has("result")) {
                String requestId = jsonMessage.getString("id");

                if (requestId.startsWith("start_user_data")) {
                    handleUserDataStreamStartResponse(jsonMessage);
                } else if (requestId.startsWith("ping_user_data")) {
                    handleUserDataStreamPingResponse(jsonMessage);
                } else if (requestId.startsWith("account_status")) {
                    AccountListenerWebsocketStream.handleAccountStatusResponse(jsonMessage);
                } else if (requestId.startsWith("commission_rates")) {
                    AccountListenerWebsocketStream.handleCommissionRatesResponse(jsonMessage);
                }
                return;
            }

            // Handle user data stream events
            if (jsonMessage.has("stream") && jsonMessage.has("data")) {
                handleUserDataStreamEvent(jsonMessage);
                return;
            }

            // Handle direct stream data (without stream wrapper)
            if (jsonMessage.has("e")) {
                handleUserDataEvent(jsonMessage);
            }

        } catch (Exception e) {
            log.error("Error handling user data message: {}", message, e);
        }
    }

    private void handleUserDataStreamStartResponse(JSONObject response) {
        try {
            if (response.has("result")) {
                JSONObject result = response.getJSONObject("result");
                if (result.has("listenKey")) {
                    String listenKey = result.getString("listenKey");
                    currentListenKey.set(listenKey);
                    isStreamActive = true;

                    log.info("User data stream started successfully with listen key: {}",
                            listenKey.substring(0, Math.min(8, listenKey.length())) + "...");

                    // Now we can subscribe to the user data stream using the listen key
                    // This would typically be done through a separate WebSocket stream connection
                    // For now, we'll just log that we have the listen key
                    // Open a new WebSocket connection to listen to the user data stream
                    subscribeToUserDataStream(listenKey);
                }
            }
        } catch (Exception e) {
            log.error("Error handling user data stream start response", e);
        }
    }

    private void subscribeToUserDataStream(String listenKey) {
        try {
            log.info("Subscribing to user data stream with listen key: {}...",
                    listenKey.substring(0, Math.min(8, listenKey.length())));

            // Subscribe to the user data stream using SpotWebSocketStreams
            String userDataStreamUrl = "wss://stream.binance.com:9443/ws/" + listenKey;

            // Use Jetty WebSocket client for direct connection
            connectToUserDataStreamDirectly(userDataStreamUrl);

            log.info("Successfully created user data stream connection");

        } catch (Exception e) {
            log.error("Failed to create user data stream connection", e);
        }
    }

    private void connectToUserDataStreamDirectly(String streamUrl) {
        try {
            // Create WebSocket client configuration
            org.eclipse.jetty.websocket.client.WebSocketClient client = new org.eclipse.jetty.websocket.client.WebSocketClient();

            client.start();

            // Create WebSocket listener
            var listener = new WebSocketListener() {

                @Override
                public void onWebSocketConnect(org.eclipse.jetty.websocket.api.Session session) {
                    log.info("User data stream WebSocket connected");
                    userDataStreamSubscribed = true;
                }

                @Override
                public void onWebSocketText(String message) {
                    log.debug("User data stream message received: {}", message);
                    handleUserDataStreamMessage(message);
                }

                @Override
                public void onWebSocketClose(int statusCode, String reason) {
                    log.warn("User data stream WebSocket closed: {} - {}", statusCode, reason);
                    userDataStreamSubscribed = false;

                    if (shouldListenToUserData.get() && isStreamActive) {
                        scheduleUserDataStreamReconnect();
                    }
                }

                @Override
                public void onWebSocketError(Throwable cause) {
                    log.error("User data stream WebSocket error", cause);
                    userDataStreamSubscribed = false;

                    if (shouldListenToUserData.get() && isStreamActive) {
                        scheduleUserDataStreamReconnect();
                    }
                }
            };

            // Connect to the user data stream
            URI streamUri = URI.create(streamUrl);
            client.connect(listener, streamUri);

        } catch (Exception e) {
            log.error("Error creating direct user data stream connection", e);
            throw new RuntimeException(e);
        }
    }

    private void scheduleUserDataStreamReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Wait 3 seconds before reconnecting
                String listenKey = currentListenKey.get();
                if (shouldListenToUserData.get() && isStreamActive &&
                        listenKey != null && !userDataStreamSubscribed) {
                    log.info("Attempting to reconnect user data stream...");
                    subscribeToUserDataStream(listenKey);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void handleUserDataStreamMessage(String message) {
        try {
            JSONObject eventData = new JSONObject(message);

            // These events come directly from the stream
            if (eventData.has("e")) {
                handleUserDataEvent(eventData);
            } else {
                log.debug("Received non-event message from user data stream: {}", message);
            }

        } catch (Exception e) {
            log.error("Error handling user data stream message: {}", message, e);
        }
    }

    private void handleUserDataStreamPingResponse(JSONObject response) {
        try {
            if (response.has("result")) {
                log.debug("User data stream ping successful");
            }
        } catch (Exception e) {
            log.error("Error handling user data stream ping response", e);
        }
    }

    private void handleUserDataStreamEvent(JSONObject streamMessage) {
        try {
            String streamName = streamMessage.getString("stream");
            JSONObject data = streamMessage.getJSONObject("data");

            log.debug("User data stream event received from stream: {}", streamName);
            handleUserDataEvent(data);

        } catch (Exception e) {
            log.error("Error handling user data stream event", e);
        }
    }

    private void handleUserDataEvent(JSONObject eventData) {
        try {
            String eventType = eventData.getString("e");

            switch (eventType) {
                case "outboundAccountPosition":
                    handleAccountPositionUpdate(eventData);
                    break;
                case "balanceUpdate":
                    handleBalanceUpdate(eventData);
                    break;
                case "executionReport":
                    handleOrderExecutionReport(eventData);
                    break;
                case "listStatus":
                    handleOrderListStatus(eventData);
                    break;
                default:
                    log.debug("Unhandled user data event type: {}", eventType);
                    log.debug("Event data: {}", eventData.toString());
            }

        } catch (Exception e) {
            log.error("Error handling user data event", e);
        }
    }

    private void handleAccountPositionUpdate(JSONObject eventData) {
        try {
            long eventTime = eventData.getLong("E");
            long lastUpdateTime = eventData.getLong("u");

            log.info("Account position update - Event time: {}, Last update: {}", eventTime, lastUpdateTime);

            if (eventData.has("B")) {
                var balances = eventData.getJSONArray("B");

                log.info("Balance updates received for {} assets:", balances.length());

                for (int i = 0; i < balances.length(); i++) {
                    JSONObject balance = balances.getJSONObject(i);
                    String asset = balance.getString("a");
                    String free = balance.getString("f");
                    String locked = balance.getString("l");

                    double freeAmount = Double.parseDouble(free);
                    double lockedAmount = Double.parseDouble(locked);

                    if (freeAmount > 0 || lockedAmount > 0) {
                        log.info("  {} - Free: {}, Locked: {}", asset, free, locked);
                    }
                    AccountListenerWebsocketStream.accountStatus.getResult().getBalances().forEach((acBalance) -> {
                        if (acBalance.getAsset().toUpperCase().equals(asset.toUpperCase())) {
                            acBalance.setFree(free);
                            acBalance.setLocked(locked);
                        }
                    });
                }
            }

        } catch (Exception e) {
            log.error("Error handling account position update", e);
        }
    }

    private void handleBalanceUpdate(JSONObject eventData) {
        try {
            long eventTime = eventData.getLong("E");
            String asset = eventData.getString("a");
            String balanceDelta = eventData.getString("d");
            long clearTime = eventData.getLong("T");

            log.info("Balance update - Asset: {}, Delta: {}, Event time: {}, Clear time: {}",
                    asset, balanceDelta, eventTime, clearTime);

            // Notify the WebsocketTradeService about balance changes
            // This can be useful for tracking if orders were executed
            double delta = Double.parseDouble(balanceDelta);
            if (Math.abs(delta) > 0.000001) { // Only log significant changes
                log.info("Significant balance change detected for {}: {}", asset, balanceDelta);
            }

        } catch (Exception e) {
            log.error("Error handling balance update", e);
        }
    }

    private void handleOrderExecutionReport(JSONObject eventData) {
        try {
            String symbol = eventData.getString("s");
            String clientOrderId = eventData.getString("c");
            String side = eventData.getString("S");
            String orderType = eventData.getString("o");
            String orderStatus = eventData.getString("X");
            String executionType = eventData.getString("x");

            log.info("Order execution report - Symbol: {}, Side: {}, Type: {}, Status: {}, Execution: {}",
                    symbol, side, orderType, orderStatus, executionType);

            // Check if this is our trading symbol
            if (symbol.equals(tradingSymbol)) {
                String orderId = String.valueOf(eventData.getLong("i"));

                // Handle different execution types
                switch (executionType) {
                    case "NEW":
                        log.info("New order placed - Order ID: {}, Client Order ID: {}", orderId, clientOrderId);
                        break;
                    case "TRADE":
                        handleTradeExecution(eventData, orderId);
                        break;
                    case "CANCELED":
                        log.info("Order canceled - Order ID: {}, Client Order ID: {}", orderId, clientOrderId);
                        break;
                    case "REJECTED":
                        log.warn("Order rejected - Order ID: {}, Client Order ID: {}", orderId, clientOrderId);
                        break;
                    case "EXPIRED":
                        log.info("Order expired - Order ID: {}, Client Order ID: {}", orderId, clientOrderId);
                        break;
                }
            }

        } catch (Exception e) {
            log.error("Error handling order execution report", e);
        }
    }

    private void handleTradeExecution(JSONObject eventData, String orderId) {
        try {
            String side = eventData.getString("S");
            String lastExecutedPrice = eventData.getString("L");
            String lastExecutedQuantity = eventData.getString("l");
            String cumulativeFilledQuantity = eventData.getString("z");

            log.info("Trade executed - Order ID: {}, Side: {}, Price: {}, Quantity: {}, Cumulative: {}",
                    orderId, side, lastExecutedPrice, lastExecutedQuantity, cumulativeFilledQuantity);

            // If this is a BUY order execution, notify the WebsocketTradeService
            if ("BUY".equals(side)) {
                try {
                    websocketTradeService.onBuyOrderExecuted(
                            orderId,
                            new java.math.BigDecimal(lastExecutedPrice),
                            new java.math.BigDecimal(lastExecutedQuantity));

                    log.info("Notified WebsocketTradeService of buy order execution: {}", orderId);

                } catch (Exception e) {
                    log.error("Error notifying WebsocketTradeService of order execution", e);
                }
            }

        } catch (Exception e) {
            log.error("Error handling trade execution", e);
        }
    }

    private void handleOrderListStatus(JSONObject eventData) {
        try {
            String orderListId = String.valueOf(eventData.getLong("g"));
            String contingencyType = eventData.getString("c");
            String listStatusType = eventData.getString("l");
            String listOrderStatus = eventData.getString("L");

            log.info("Order list status - ID: {}, Type: {}, Status: {}, Order Status: {}",
                    orderListId, contingencyType, listStatusType, listOrderStatus);

        } catch (Exception e) {
            log.error("Error handling order list status", e);
        }
    }

    private void startPingScheduler() {
        // Schedule periodic pings to keep the user data stream alive
        Thread pingThread = new Thread(() -> {
            while (shouldListenToUserData.get() && isStreamActive) {
                try {
                    Thread.sleep(30000); // Ping every 30 seconds (recommended by Binance)

                    String listenKey = currentListenKey.get();
                    if (listenKey != null && isStreamActive) {
                        pingUserDataStream(listenKey);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error in ping scheduler", e);
                }
            }
        });

        pingThread.setName("UserDataStreamPing");
        pingThread.setDaemon(true);
        pingThread.start();

        log.info("User data stream ping scheduler started");
    }

    private void pingUserDataStream(String listenKey) {
        try {
            WebSocketApiUserDataStream userDataStream = (WebSocketApiUserDataStream) userDataWebsocketClient
                    .userDataStream();

            JSONObject pingParams = new JSONObject();
            pingParams.put("requestId", "ping_user_data_" + System.currentTimeMillis());

            userDataStream.userDataStreamPing(listenKey, pingParams);

            log.debug("User data stream ping sent for listen key: {}...",
                    listenKey.substring(0, Math.min(8, listenKey.length())));

        } catch (Exception e) {
            log.error("Error pinging user data stream", e);
        }
    }

    private void scheduleReconnect() {
        // Simple reconnect logic - you can enhance this with exponential backoff
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds before reconnecting
                if (shouldListenToUserData.get()) {
                    log.info("Attempting to reconnect User Data WebSocket...");
                    connect();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void disconnect() {
        shouldListenToUserData.set(false);
        isStreamActive = false;

        // Stop the user data stream
        String listenKey = currentListenKey.get();
        if (listenKey != null) {
            try {
                WebSocketApiUserDataStream userDataStream = (WebSocketApiUserDataStream) userDataWebsocketClient
                        .userDataStream();

                JSONObject stopParams = new JSONObject();
                stopParams.put("requestId", "stop_user_data_" + System.currentTimeMillis());

                userDataStream.userDataStreamStop(listenKey, stopParams);

                log.info("User data stream stop request sent");

            } catch (Exception e) {
                log.error("Error stopping user data stream", e);
            }
        }

        // Close the WebSocket connection
        if (userDataWebsocketClient != null) {
            userDataWebsocketClient.close();
        }

        log.info("User Data WebSocket disconnected");
    }

    /**
     * Gets the current listen key
     */
    public String getCurrentListenKey() {
        return currentListenKey.get();
    }

    /**
     * Checks if the user data stream is active
     */
    public boolean isStreamActive() {
        return isStreamActive;
    }
}
