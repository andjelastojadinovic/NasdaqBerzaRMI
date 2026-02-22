# NASDAQ BERZA – RMI & SOCKET DISTRIBUTED SYSTEM

## PROJECT OVERVIEW

Homework assignment for the course **Selected Topics in Business Application Design**.

The project represents a distributed simulation of the Nasdaq stock exchange, implemented using:

- Java RMI (Remote Method Invocation)
- TCP Socket Server (real-time feed)
- Multithreading and concurrent collections

The system simulates real-time stock price changes, order matching, portfolio management, and trade history, while supporting multiple concurrent clients and automated trading bots.

---

## IMPLEMENTED FEATURES

### Core Functionality (RMI)

- `getStocks()` – list of all companies  
- `getAskOrders()` – sell orders (ascending by price)  
- `getBidOrders()` – buy orders (descending by price)  
- `placeOrder()` – BUY / SELL orders  
- `getTrades()` – trade history by symbol and date  
- `getPortfolio()` – client portfolio  

### TCP Socket Feed

Real-time stock updates.

Message format:

```
FEED:symbol|hour|price|change
```

- Updates every 6 seconds (1 simulated hour)
- Broadcast to all connected clients

### Stock Simulator

- 1 simulated hour = 6 seconds
- Trading hours: 09:00–16:00
- After 16:00, a new trading day automatically starts
- Prices fluctuate slightly each simulated hour

### Order Matching Engine

- BUY matches the lowest ASK where `askPrice ≤ buyPrice`
- SELL matches the highest BID where `bidPrice ≥ sellPrice`
- Self-trade prevention (client cannot trade with itself)
- Portfolio validation before accepting SELL order
- Thread-safe order book handling

### Trade Archiving

- Separate thread (`TradeArchiver`)
- Every executed trade is written to:

```
trades_archive.txt
```

### Console Real-Time Display

- Fixed row per company (ANSI cursor positioning)
- Green color = price increase
- Red color = price decrease
- Arrows ↑ ↓
- 1H / 24H / 7D percentage change
- No screen flickering

### Bot Clients (AutoClient)

- Supports 10+ concurrent bots
- Random BUY/SELL orders
- Price delta ±1% from current price
- Used to simulate realistic market activity

---

## PROJECT STRUCTURE

```
NasdaqBerzaRMI/
├── src/main/java/rs/raf/pds/berza/
│   ├── service/
│   │   ├── BerzaServiceInterface.java
│   │   └── BerzaService.java
│   │
│   ├── server/
│   │   ├── BerzaServiceRMIServer.java
│   │   ├── BerzaSocketServer.java
│   │   ├── StockSimulator.java
│   │   └── TradeArchiver.java
│   │
│   ├── client/
│   │   ├── BerzaServiceRMIClient.java
│   │   ├── SocketListener.java
│   │   ├── ConsoleDisplay.java
│   │   └── AutoClient.java
│   │
│   └── messages/
		├── DatumUpitRMI.java
		├── NizOrderRMI.java
		├── NizStockDataRMI.java
		├── NizTradeRMI.java
		├── OrderRMI.java
		├── PortfolioRMI.java
		├── RegistracijaRMI.java
│       ├── StockDataRMI.java
│       ├── StockUpitRMI.java       
│       └── TradeRMI.java
│
└── trades_archive.txt
```

---

## SYSTEM ARCHITECTURE

### 1️ RMI SERVER (Port 1099)

Responsible for:

- Stock data retrieval
- Order book management (Bid / Ask)
- Order matching logic
- Portfolio management
- Trade history queries

Registered as:

```
BerzaService
```

---

### 2️ SOCKET SERVER (Port 9090)

Responsible for:

- Real-time stock feed
- Broadcasting price updates
- Sending trade notifications

Clients connect and subscribe to stock symbols.

---

### 3️ CLIENT TYPES

#### Interactive Client

- Menu-based interface
- RMI calls for trading
- SocketListener for real-time feed
- ANSI table display

#### AutoClient (Bots)

- No console display
- Generates random orders
- Simulates market activity
- Enables realistic order matching

---

## CLIENT MENU

```
1 - All stocks (getStocks)
2 - Ask list (getAskOrders)
3 - Bid list (getBidOrders)
4 - Buy stocks (BUY)
5 - Sell stocks (SELL)
6 - Portfolio
7 - Trade history
8 - Tracking feed (real-time)
0 - Exit
```

---

## REAL-TIME FEED MODE

Example output:

```
SYMBOL   PRICE($)      CHANGE       1H        24H        7D
AAPL     221.54        +1.53% ↑     +0.21% ↑  +1.53% ↑   +3.21% ↑
MSFT     383.17        -0.45% ↓     -0.10% ↓  -0.45% ↓   +1.22% ↑
```

Features:

- Same company always appears in the same row
- No full screen clearing
- Green = growth
- Red = decline
- Updates every 6 seconds

---

## RUNNING THE APPLICATION

### 1️ Build (Maven)

```bash
mvn clean package
```

---

### 2️ Start Server

```bash
java -cp target/classes rs.raf.pds.berza.server.BerzaServiceRMIServer
```

Expected output:

```
NASDAQ Stock Market RMI Server
[SIMULATOR] Stock market simulation started.
[RMI] Service is registered as 'BerzaService'
[SOCKET] Feed server on port 9090
[SERVER] Ready for clients!
```

---

### 3️ Start Interactive Client

```bash
java -cp target/classes rs.raf.pds.berza.client.BerzaServiceRMIClient
```

Optional:

```bash
java -cp target/classes rs.raf.pds.berza.client.BerzaServiceRMIClient localhost Name
```

---

### 4️ Start Bots

```bash
java -cp target/classes rs.raf.pds.berza.client.AutoClient localhost 10
```

Arguments:

- `localhost` – server host  
- `10` – number of bots  

---

## SIMULATION DETAILS

| Feature | Value |
|----------|--------|
| Companies | 12 NASDAQ companies |
| Initial portfolio | 100 shares per company |
| Simulated hour | 6 seconds |
| Trading hours | 09:00–16:00 |
| Bot price delta | ±1% |

---

## THREAD SAFETY

The system uses:

- `ConcurrentHashMap`
- Separate threads:
  - `StockSimulator`
  - `TradeArchiver`
  - `SocketListener`
- Synchronized order book updates

Supports 10+ concurrent clients simultaneously.

---

## TECHNOLOGIES USED

- Java 11
- Java RMI
- TCP Sockets
- Multithreading
- Concurrent Collections
- ANSI terminal control
- Maven
- Git
