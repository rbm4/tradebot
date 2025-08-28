# Trading Bot - Binance Integration

A Java Spring Boot trading bot that integrates with Binance's WebSocket API for real-time market data streaming and automated trading operations.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Architecture Overview](#architecture-overview)
- [BinanceConfig Integration](#binanceconfig-integration)
- [WebSocket Components](#websocket-components)
- [WebsocketTradeService Integration](#websockettradeservice-integration)
- [Data Flow](#data-flow)
- [Getting Started](#getting-started)

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Binance API credentials (API Key and Secret Key)

## Configuration

### Environment Variables

Before running the application, you **must** set the following environment variables with your Binance API credentials:

```bash
# Windows (PowerShell)
$env:BINANCE_KEY="your_binance_api_key_here"
$env:BINANCE_SECRET="your_binance_secret_key_here"

# Linux/Mac
export BINANCE_KEY="your_binance_api_key_here"
export BINANCE_SECRET="your_binance_secret_key_here"
```

### Application Properties

The application also supports configuration through `application.yaml`:

```yaml
binance:
  key: ${BINANCE_KEY}
  secret: ${BINANCE_SECRET}
  trading:
    symbol: BTCUSDT  # Default trading symbol
```

⚠️ **Security Warning**: Never hardcode your API credentials in the source code. Always use environment variables or secure configuration management.

## Architecture Overview

The application follows a layered architecture with clear separation of concerns:

```
BinanceConfig → BinanceWebsocketComponent → WebSocket Streams → WebsocketTradeService
```

## BinanceConfig Integration

### Purpose
The `BinanceConfig` class serves as the central configuration hub for all Binance API interactions. It creates and configures the necessary beans for both REST API and WebSocket connections.

### Key Components

#### 1. **SpotClient Bean**
```java
@Bean
public SpotClient binanceSpotClient() {
    return new SpotClientImpl(key, secret);
}
```
- Creates the main Binance Spot trading client
- Used for placing orders and REST API operations
- Automatically configured with API credentials from environment variables

#### 2. **SpotRestApi Bean**
```java
@Bean
public SpotRestApi binanceSpotRestClient() {
    return new SpotRestApi(getConfig());
}
```
- Provides REST API functionality
- Used for market data queries and account information

#### 3. **ExchangeInfoResponse Bean**
```java
@Bean
public ExchangeInfoResponse tradingSymbol() {
    // Fetches exchange information for the configured trading symbol
}
```
- Retrieves exchange information for the trading symbol
- Contains symbol filters, lot sizes, and trading rules
- Used for order validation and quantity calculations

#### 4. **SignatureConfiguration Bean**
```java
@Bean
public SignatureConfiguration signatureConfiguration() {
    SignatureConfiguration signatureConfiguration = new SignatureConfiguration();
    signatureConfiguration.setApiKey(key);
    signatureConfiguration.setSecretKey(secret);
    return signatureConfiguration;
}
```
- **Critical Component**: This bean is the bridge between `BinanceConfig` and WebSocket components
- Encapsulates API credentials in a secure configuration object
- Injected into `BinanceWebsocketComponent` for WebSocket authentication

## WebSocket Components

### BinanceWebsocketComponent

This component acts as the **WebSocket factory**, creating authenticated WebSocket connections:

```java
@Component
@RequiredArgsConstructor
public class BinanceWebsocketComponent {
    private final SignatureConfiguration signatureConfig; // ← Injected from BinanceConfig
    
    @Bean
    public SpotWebSocketStreams initSpotStream() {
        // Creates public WebSocket streams (market data)
    }
    
    @Bean
    public WebSocketApiClientImpl initAccountWebsocketStream() {
        // Creates private WebSocket streams (account data)
    }
}
```

**Integration Point**: The `SignatureConfiguration` from `BinanceConfig` is injected here, enabling secure WebSocket connections.

### WebSocket Stream Classes

#### 1. **TradeWebsocketStream**
- **Purpose**: Streams real-time trade executions for the configured symbol
- **Data**: Individual trade events (price, quantity, timestamp)
- **Integration**: 
  ```java
  private final SpotWebSocketStreams spotWebSocketStreams; // ← From BinanceWebsocketComponent
  private final WebsocketTradeService websocketTradeService; // ← Target service
  ```

#### 2. **TickerWebsocketStream**
- **Purpose**: Streams real-time best bid/offer (book ticker) data
- **Data**: Current bid price, ask price, and quantities
- **Integration**: 
  ```java
  private final SpotWebSocketStreams spotWebSocketStreams; // ← From BinanceWebsocketComponent
  private final WebsocketTradeService websocketTradeService; // ← Target service
  ```

#### 3. **AccountListenerWebsocketStream**
- **Purpose**: Monitors account status and balance changes
- **Data**: Account permissions, balances, and trading status
- **Integration**: Uses `WebSocketApiClientImpl` for authenticated streams

## WebsocketTradeService Integration

### Service Overview
`WebsocketTradeService` is the **core trading engine** that processes incoming WebSocket data and makes trading decisions.

### Key Integration Points

#### 1. **Trade Data Processing**
```java
public void updateTrade(TradeResponse trade) {
    // Called by TradeWebsocketStream
    // Processes individual trade executions
    // Updates market momentum analysis
    // Triggers scalping opportunity analysis
}
```

#### 2. **Ticker Data Processing**
```java
public void updateTicker(BookTickerResponse ticker) {
    // Called by TickerWebsocketStream
    // Updates current bid/ask spreads
    // Triggers trading analysis when conditions are met
}
```

#### 3. **Account Data Access**
```java
private BigDecimal getAssetBalance(String asset) {
    var acc = AccountListenerWebsocketStream.accountStatus.getResult();
    // Accesses current account balances
    // Used for position sizing and risk management
}
```

### Trading Logic Flow

1. **Data Ingestion**: WebSocket streams feed real-time data
2. **Analysis**: Market momentum and scalping opportunities are analyzed
3. **Decision Making**: Trading signals are generated based on:
   - Spread analysis
   - Momentum indicators
   - Account balance checks
   - Risk management rules
4. **Order Execution**: Buy/sell orders are placed through the `OrderService`

## Data Flow

```mermaid
graph TD;
    A[Environment Variables] --> B[BinanceConfig];
    B --> C[SignatureConfiguration];
    C --> D[BinanceWebsocketComponent];
    D --> E[SpotWebSocketStreams];
    D --> F[WebSocketApiClientImpl];
    
    E --> G[TradeWebsocketStream];
    E --> H[TickerWebsocketStream];
    F --> I[AccountListenerWebsocketStream];
    
    G --> J[WebsocketTradeService.updateTrade()];
    H --> J2[WebsocketTradeService.updateTicker()];
    I --> K[AccountStatusResponse];
    
    J --> L[Trading Analysis];
    J2 --> L;
    K --> L;
    L --> M[Order Execution];
```

### Step-by-Step Flow

1. **Startup**: Application loads environment variables (`BINANCE_KEY`, `BINANCE_SECRET`)
2. **Configuration**: `BinanceConfig` creates `SignatureConfiguration` with credentials
3. **WebSocket Setup**: `BinanceWebsocketComponent` uses the signature config to create authenticated connections
4. **Stream Initialization**: Individual stream classes (`TradeWebsocketStream`, `TickerWebsocketStream`) start listening
5. **Data Processing**: Incoming WebSocket messages are routed to `WebsocketTradeService`
6. **Trading Decisions**: Service analyzes market data and executes trading strategies
7. **Order Management**: Orders are placed through the configured Binance client

## Getting Started

1. **Set Environment Variables**:
   ```bash
   $env:BINANCE_KEY="your_api_key"
   $env:BINANCE_SECRET="your_secret_key"
   ```

2. **Configure Trading Symbol** (optional):
   ```yaml
   binance:
     trading:
       symbol: BTCUSDT  # Change to your preferred trading pair
   ```

3. **Run the Application**:
   ```bash
   mvn spring-boot:run
   ```

4. **Monitor Logs**:
   - WebSocket connection status
   - Real-time market data ingestion
   - Trading decisions and order executions
   - Account balance updates

The application will automatically start all WebSocket streams and begin processing market data for automated trading operations.

---

**Note**: This is a high-frequency trading application. Ensure you understand the risks involved and test thoroughly in a sandbox environment before using with real funds.
