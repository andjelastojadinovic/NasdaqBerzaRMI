package rs.raf.pds.berza.service;

import rs.raf.pds.berza.messages.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class BerzaService {

    private final Map<String, StockDataRMI> stocks = new ConcurrentHashMap<>();
    private final Map<String, List<OrderRMI>> bidOrders = new ConcurrentHashMap<>();
    private final Map<String, List<OrderRMI>> askOrders = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> portfolios = new ConcurrentHashMap<>();
    private final Map<String, String> clients = new ConcurrentHashMap<>();
    private final List<TradeRMI> allTrades = new CopyOnWriteArrayList<>();
    private TradeListener tradeListener;

    public interface TradeListener {
        void onTrade(TradeRMI trade);
    }

    public BerzaService() {
        initStocks();
    }

    public void setTradeListener(TradeListener listener) {
        this.tradeListener = listener;
    }

    private void initStocks() {
        Date today = new Date();
        dodajAkciju("AAPL",  "Apple Inc.",          220.82, today);
        dodajAkciju("MSFT",  "Microsoft Corp.",      378.92, today);
        dodajAkciju("GOOG",  "Alphabet Inc.",        175.43, today);
        dodajAkciju("AMZN",  "Amazon.com Inc.",      224.92, today);
        dodajAkciju("TSLA",  "Tesla Inc.",           338.74, today);
        dodajAkciju("META",  "Meta Platforms",       623.77, today);
        dodajAkciju("NVDA",  "NVIDIA Corp.",         131.38, today);
        dodajAkciju("ADBE",  "Adobe Inc.",           409.72, today);
        dodajAkciju("MA",    "Mastercard Inc.",      533.94, today);
        dodajAkciju("NFLX",  "Netflix Inc.",         963.71, today);
        dodajAkciju("INTC",  "Intel Corp.",           20.88, today);
        dodajAkciju("AMD",   "Advanced Micro Dev.",  116.45, today);
    }

    private void dodajAkciju(String symbol, String name, double price, Date date) {
        stocks.put(symbol, new StockDataRMI(symbol, name, price, price, 0.0, date));
        bidOrders.put(symbol, new CopyOnWriteArrayList<>());
        askOrders.put(symbol, new CopyOnWriteArrayList<>());
    }

    public String registerClient(String clientName) {
        String clientId = UUID.randomUUID().toString().substring(0, 8);
        clients.put(clientId, clientName);
        Map<String, Integer> portfolio = new ConcurrentHashMap<>();
        for (String symbol : stocks.keySet()) {
            portfolio.put(symbol, 100);
        }
        portfolios.put(clientId, portfolio);
        System.out.println("[STOCK MARKET] Registered client: " + clientName + " (ID: " + clientId + ")");
        return clientId;
    }

    public List<StockDataRMI> getStocks() {
        return new ArrayList<>(stocks.values());
    }

    public List<OrderRMI> getAskOrders(String symbol, int count) {
        return askOrders.getOrDefault(symbol, Collections.emptyList())
                .stream()
                .filter(OrderRMI::isActive)
                .sorted(Comparator.comparingDouble(OrderRMI::getPrice))
                .limit(count)
                .collect(Collectors.toList());
    }

    public List<OrderRMI> getBidOrders(String symbol, int count) {
        return bidOrders.getOrDefault(symbol, Collections.emptyList())
                .stream()
                .filter(OrderRMI::isActive)
                .sorted(Comparator.comparingDouble(OrderRMI::getPrice).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    public synchronized String placeOrder(OrderRMI order) {
        String clientId = order.getClientId();
        String symbol = order.getSymbol();

        if (!clients.containsKey(clientId)) return "ERROR: Unregistered client!";
        if (!stocks.containsKey(symbol))    return "ERROR: Unknown symbol: " + symbol;

        Map<String, Integer> portfolio = portfolios.get(clientId);

        if (order.getType() == OrderRMI.OrderType.SELL) {
            int owned = portfolio.getOrDefault(symbol, 0);
            if (owned < order.getQuantity()) {
                return "ERROR: You don't have enough stock! You have: " + owned;
            }
        }

        if (order.getType() == OrderRMI.OrderType.BUY) {
            String result = matchBuyOrder(order);
            if (!result.startsWith("PAIRED")) {
                bidOrders.get(symbol).add(order);
                return "NALOG PRIMLJEN (ceka uparivanje): " + order.toString();
            }
            return result;
        } else {
            String result = matchSellOrder(order);
            if (!result.startsWith("UPARENO")) {
                askOrders.get(symbol).add(order);
                return "ORDER RECEIVED (waiting for pairing): " + order.toString();
            }
            return result;
        }
    }

    private String matchBuyOrder(OrderRMI buyOrder) {
        String symbol = buyOrder.getSymbol();
        Optional<OrderRMI> match = askOrders.get(symbol).stream()
                .filter(o -> o.isActive()
                        && o.getPrice() <= buyOrder.getPrice()
                        && !o.getClientId().equals(buyOrder.getClientId()))
                .min(Comparator.comparingDouble(OrderRMI::getPrice));

        if (match.isPresent()) {
            OrderRMI sellOrder = match.get();
            int qty = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());
            double tradePrice = sellOrder.getPrice();
            executeTrade(symbol, tradePrice, qty, buyOrder.getClientId(), sellOrder.getClientId());
            sellOrder.setQuantity(sellOrder.getQuantity() - qty);
            if (sellOrder.getQuantity() == 0) sellOrder.setActive(false);
            buyOrder.setQuantity(buyOrder.getQuantity() - qty);
            if (buyOrder.getQuantity() > 0) bidOrders.get(symbol).add(buyOrder);
            return "PAIRED: Bought " + qty + "x" + symbol + " @ " + tradePrice;
        }
        return "NO_MATCH";
    }

    private String matchSellOrder(OrderRMI sellOrder) {
        String symbol = sellOrder.getSymbol();
        Optional<OrderRMI> match = bidOrders.get(symbol).stream()
                .filter(o -> o.isActive()
                        && o.getPrice() >= sellOrder.getPrice()
                        && !o.getClientId().equals(sellOrder.getClientId()))
                .max(Comparator.comparingDouble(OrderRMI::getPrice));

        if (match.isPresent()) {
            OrderRMI buyOrder = match.get();
            int qty = Math.min(sellOrder.getQuantity(), buyOrder.getQuantity());
            double tradePrice = buyOrder.getPrice();
            executeTrade(symbol, tradePrice, qty, buyOrder.getClientId(), sellOrder.getClientId());
            buyOrder.setQuantity(buyOrder.getQuantity() - qty);
            if (buyOrder.getQuantity() == 0) buyOrder.setActive(false);
            sellOrder.setQuantity(sellOrder.getQuantity() - qty);
            if (sellOrder.getQuantity() > 0) askOrders.get(symbol).add(sellOrder);
            return "PAIRED: SOLD " + qty + "x" + symbol + " @ " + tradePrice;
        }
        return "NO_MATCH";
    }

    private void executeTrade(String symbol, double price, int qty, String buyerId, String sellerId) {
        TradeRMI trade = new TradeRMI(symbol, price, qty, buyerId, sellerId);
        allTrades.add(trade);

        portfolios.get(buyerId).merge(symbol, qty, Integer::sum);
        portfolios.get(sellerId).merge(symbol, -qty, Integer::sum);

        StockDataRMI stock = stocks.get(symbol);
        double open = stock.getOpenPrice();
        stocks.put(symbol, new StockDataRMI(symbol, stock.getCompanyName(), open, price, price - open, stock.getDate()));

        if (tradeListener != null) tradeListener.onTrade(trade);

        System.out.println("[TRADE] " + trade.toString());
    }

    public List<TradeRMI> getTrades(String symbol, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int y = cal.get(Calendar.YEAR), m = cal.get(Calendar.MONTH), d = cal.get(Calendar.DAY_OF_MONTH);

        return allTrades.stream()
                .filter(t -> t.getSymbol().equals(symbol))
                .filter(t -> {
                    Calendar tc = Calendar.getInstance();
                    tc.setTime(t.getTimestamp());
                    return tc.get(Calendar.YEAR) == y
                        && tc.get(Calendar.MONTH) == m
                        && tc.get(Calendar.DAY_OF_MONTH) == d;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Integer> getPortfolio(String clientId) {
        return portfolios.getOrDefault(clientId, Collections.emptyMap());
    }

    public Map<String, StockDataRMI> getStocksMap() { return stocks; }

    public void updateStockPrice(String symbol, double newPrice) {
        StockDataRMI stock = stocks.get(symbol);
        if (stock != null) {
            double open = stock.getOpenPrice();
            stocks.put(symbol, new StockDataRMI(symbol, stock.getCompanyName(),
                    open, newPrice, newPrice - open, stock.getDate()));
        }
    }
}