package rs.raf.pds.berza.client;

import rs.raf.pds.berza.messages.*;
import rs.raf.pds.berza.server.BerzaSocketServer;
import rs.raf.pds.berza.service.BerzaServiceInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;

public class AutoClient extends Thread {

    private final String host;
    private final String name;
    private BerzaServiceInterface stub;
    private SocketListener socketListener;
    private String clientId;
    private final Random random = new Random();
    private volatile boolean running = true;

    private static final int MIN_INTERVAL_MS = 2000;
    private static final int MAX_INTERVAL_MS = 8000;
    private static final double MAX_DELTA_PCT = 0.01; 

    public AutoClient(String host, String name) {
        this.host = host;
        this.name = name;
        setDaemon(true);
        setName("AutoBot-" + name);
    }

    @Override
    public void run() {
        try {
            connect();
            autoTrade();
        } catch (Exception e) {
            System.err.println("[AUTO " + name + "] Error: " + e.getMessage());
        }
    }

    private void connect() throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, 1099);
        stub = (BerzaServiceInterface) registry.lookup("BerzaService");
        clientId = stub.registerClient(new RegistracijaRMI(name));

        socketListener = new SocketListener(clientId, host, BerzaSocketServer.SOCKET_PORT, false); 
        socketListener.register();
        socketListener.start();

        NizStockDataRMI stocks = stub.getStocks();
        String[] symbols = stocks.getStocks().stream().map(StockDataRMI::getSymbol).toArray(String[]::new);
        socketListener.subscribe(symbols);

        System.out.println("[AUTO] " + name + " connected (ID: " + clientId + ")");
    }

    private void autoTrade() throws Exception {
        List<StockDataRMI> stocks = stub.getStocks().getStocks();

        while (running) {
            try {
                StockDataRMI stock = stocks.get(random.nextInt(stocks.size()));
                String symbol = stock.getSymbol();

                double currentPrice = socketListener.getCurrentPrices()
                        .getOrDefault(symbol, stock.getCurrentPrice());

                double delta = currentPrice * (random.nextDouble() * 2 * MAX_DELTA_PCT - MAX_DELTA_PCT);
                double orderPrice = Math.round((currentPrice + delta) * 100.0) / 100.0;
                if (orderPrice <= 0) orderPrice = currentPrice;

                int qty = random.nextInt(30) + 1;
                OrderRMI.OrderType type = random.nextBoolean() ? OrderRMI.OrderType.BUY : OrderRMI.OrderType.SELL;

                // RMI poziv
                OrderRMI order = new OrderRMI(clientId, symbol, orderPrice, qty, type);
                String result = stub.placeOrder(order);
                System.out.println("[AUTO " + name + "] " + type + " " + qty + "x" + symbol + " @ " + orderPrice + " -> " + result);

                Thread.sleep(MIN_INTERVAL_MS + random.nextInt(MAX_INTERVAL_MS - MIN_INTERVAL_MS));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[AUTO " + name + "] " + e.getMessage());
                Thread.sleep(2000);
            }
        }
    }

    public void stopClient() {
        running = false;
        interrupt();
        if (socketListener != null) socketListener.disconnect();
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int num = args.length > 1 ? Integer.parseInt(args[1]) : 10;

        System.out.println("Running " + num + " auto clients on " + host + "...");
        AutoClient[] clients = new AutoClient[num];
        for (int i = 0; i < num; i++) {
            clients[i] = new AutoClient(host, "Bot-" + (i + 1));
            clients[i].start();
            Thread.sleep(300);
        }

        System.out.println("All bots active. Press Enter for shutting down...");
        new java.util.Scanner(System.in).nextLine();
        for (AutoClient c : clients) c.stopClient();
    }
}
