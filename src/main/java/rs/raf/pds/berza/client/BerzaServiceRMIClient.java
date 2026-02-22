package rs.raf.pds.berza.client;

import rs.raf.pds.berza.messages.*;
import rs.raf.pds.berza.server.BerzaSocketServer;
import rs.raf.pds.berza.service.BerzaServiceInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class BerzaServiceRMIClient {

    private BerzaServiceInterface stub;
    private SocketListener socketListener;
    private String clientId;
    private String clientName;
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        String name = args.length > 1 ? args[1] : "Client-" + new Random().nextInt(1000);
        new BerzaServiceRMIClient().start(host, name);
    }

    public void start(String host, String name) throws Exception {
        this.clientName = name;

        System.out.println("I am connecting to RMI registry: " + host + ":1099");
        Registry registry = LocateRegistry.getRegistry(host, 1099);
        stub = (BerzaServiceInterface) registry.lookup("BerzaService");
        System.out.println("[RMI] I am connected!");

        clientId = stub.registerClient(new RegistracijaRMI(name));
        System.out.println("[RMI] Registered as: " + name + " | ID: " + clientId);

        socketListener = new SocketListener(clientId, host, BerzaSocketServer.SOCKET_PORT, true);
        socketListener.register();
        socketListener.start();
        System.out.println("[SOCKET] Connected to feed server!");

        ConsoleDisplay.setFeedActive(false);

        odaberKompanije();
        mainMeni();
    }

    private void odaberKompanije() throws Exception {
        System.out.println("\nAvailable companies");
        NizStockDataRMI result = stub.getStocks();
        for (int i = 0; i < result.getStocks().size(); i++) {
            StockDataRMI s = result.getStocks().get(i);
            System.out.printf("%2d. %-5s - %-20s $%.2f%n",
                    i + 1, s.getSymbol(), s.getCompanyName(), s.getCurrentPrice());
        }
        System.out.print("\nEnter simbols for tracking (example: AAPL,MSFT) or Enter for all: ");
        String input = scanner.nextLine().trim().toUpperCase();

        if (input.isEmpty()) {
            String[] sve = result.getStocks().stream()
                    .map(StockDataRMI::getSymbol).toArray(String[]::new);
            socketListener.subscribe(sve);
        } else {
            socketListener.subscribe(input.split(","));
        }
        System.out.println("[OK] \n" + 
        		"Subscribed to the feed. You can use the menu.");
    }

    private void mainMeni() throws Exception {
        while (true) {
            System.out.println();
            System.out.println("============================================================");
            System.out.println("  NASDAQ STOCK MARKET - MAIN MENU (client: " + clientName + ")");
            System.out.println("============================================================");
            System.out.println("  1 - All stock (current prices)");
            System.out.println("  2 - Ask list (ponude za prodaju)");
            System.out.println("  3 - Bid list (ponude za kupovinu)");
            System.out.println("  4 - Buy stock");
            System.out.println("  5 - Sell stock");
            System.out.println("  6 - Portfolio");
            System.out.println("  7 - Trade history");
            System.out.println("  8 - Feed tracking (real-time prices)");
            System.out.println("  0 - Exit");
            System.out.println("------------------------------------------------------------");
            System.out.print("Choice: ");

            String izbor = scanner.nextLine().trim();

            switch (izbor) {
                case "1": prikaziSveAkcije();  break;
                case "2": prikaziAsk();         break;
                case "3": prikaziBid();         break;
                case "4": posaljiNalog(OrderRMI.OrderType.BUY);  break;
                case "5": posaljiNalog(OrderRMI.OrderType.SELL); break;
                case "6": prikaziPortfolio();   break;
                case "7": prikaziIstoriju();    break;
                case "8": feedMod();            break;
                case "0":
                    socketListener.disconnect();
                    System.out.println("\nGoodbye!");
                    return;
                default:
                    System.out.println("[!] Unknown command. Enter number 0-8.");
            }
        }
    }

    private void feedMod() {
        System.out.println("\n[FEED] Tracking... (type 'q' + Enter to be back to the main menu)");

        ConsoleDisplay.clearScreen();
        ConsoleDisplay.printHeader();

        ConsoleDisplay.setFeedActive(true);

        while (true) {
            ConsoleDisplay.moveCursorToMeni();
            System.out.print("[FEED MODE] Type 'q' + Enter to be back to the main menu: ");
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("q")) break;
        }

        ConsoleDisplay.setFeedActive(false);
        ConsoleDisplay.clearScreen();
        System.out.println("[OK] Out of the feed mode.");
    }

    private void prikaziSveAkcije() throws Exception {
        NizStockDataRMI result = stub.getStocks();
        System.out.println("\n1. ALL STOCK ");
        System.out.printf("%-6s %-20s %10s %10s %11s%n",
                "SYMBOL", "NAME", "OPEN ($)", "CURRENT", "CHANGE");
        System.out.println("--------------------------------------------------------------");
        for (StockDataRMI s : result.getStocks()) {
            String promena = String.format("%+.2f", s.getChange());
            System.out.printf("%-6s %-20s %10.2f %10.2f %11s%n",
                    s.getSymbol(), s.getCompanyName(),
                    s.getOpenPrice(), s.getCurrentPrice(), promena);
        }
        System.out.println("--------------------------------------------------------------");
        cekajEnter();
    }

    private void prikaziAsk() throws Exception {
        System.out.println("\n2. ASK LIST");
        System.out.print("Symbol (example AAPL): ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        System.out.print("Number of offers (example 5): ");
        int count = Integer.parseInt(scanner.nextLine().trim());

        NizOrderRMI result = stub.getAskOrders(new StockUpitRMI(symbol, count));
        System.out.println("\n  Offers for SALE: " + symbol + " ascending");
        System.out.printf("  %-8s %10s %10s%n", "SYMBOL", "PRICE ($)", "AMOUNT");
        System.out.println("  ------------------------------");
        if (result.getOrders().isEmpty()) {
            System.out.println(" No active offers for sale.");
        } else {
            result.getOrders().forEach(o ->
                System.out.printf("  %-8s %10.2f %10d%n",
                        o.getSymbol(), o.getPrice(), o.getQuantity()));
        }
        cekajEnter();
    }

    private void prikaziBid() throws Exception {
        System.out.println("\n3. BID LIST");
        System.out.print("Symbol (example AAPL): ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        System.out.print("Number of offers (example 5): ");
        int count = Integer.parseInt(scanner.nextLine().trim());

        NizOrderRMI result = stub.getBidOrders(new StockUpitRMI(symbol, count));
        System.out.println("\n  Offers for purchase: " + symbol + " descending");
        System.out.printf("  %-8s %10s %10s%n", "SYMBOL", "PRICE ($)", "AMOUNT");
        System.out.println("  ------------------------------");
        if (result.getOrders().isEmpty()) {
            System.out.println("  There are no active offers to buy.");
        } else {
            result.getOrders().forEach(o ->
                System.out.printf("  %-8s %10.2f %10d%n",
                        o.getSymbol(), o.getPrice(), o.getQuantity()));
        }
        cekajEnter();
    }

    private void posaljiNalog(OrderRMI.OrderType type) throws Exception {
        String tipStr = type == OrderRMI.OrderType.BUY ? "PURCHASE" : "SALE";
        System.out.println("\n=== " + (type == OrderRMI.OrderType.BUY ? "4" : "5")
                + ". ORDER FOR " + tipStr);
        System.out.print("Symbol (example AAPL): ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        System.out.print("Price per stock ($): ");
        double price = Double.parseDouble(scanner.nextLine().trim().replace(",", "."));
        System.out.print("Amount of stock: ");
        int qty = Integer.parseInt(scanner.nextLine().trim());

        OrderRMI order = new OrderRMI(clientId, symbol, price, qty, type);
        String result = stub.placeOrder(order);
        System.out.println("\n  [SERVER ANSWER] " + result);

        if (result.startsWith("ORDER RECEIVED")) {
            System.out.println("  [INFO] The order has been added to the order book.");
            System.out.println("  [INFO] It will be executed when the opposite offer appears.");
        } else if (result.startsWith("PAIRED")) {
            System.out.println("  [INFO] The trade was immediately realized!");
        }
        cekajEnter();
    }

    private void prikaziPortfolio() throws Exception {
        PortfolioRMI portfolio = stub.getPortfolio(clientId);
        System.out.println("\n6. PORTFOLIO");
        System.out.println("  Client: " + clientName + " (ID: " + clientId + ")");
        System.out.printf("  %-8s %10s%n", "SYMBOL", "AMOUNT");
        System.out.println("  --------------------");
        portfolio.getAkcije().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("  %-8s %10d%n",
                        e.getKey(), e.getValue()));
        cekajEnter();
    }

    private void prikaziIstoriju() throws Exception {
        System.out.println("\n7. TRADE HISTORY");
        System.out.print("Symbol (example AAPL): ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        System.out.print("Date (Enter = today, or dd.MM.yyyy): ");
        String dateStr = scanner.nextLine().trim();
        Date date = dateStr.isEmpty() ? new Date()
                : new java.text.SimpleDateFormat("dd.MM.yyyy").parse(dateStr);

        NizTradeRMI result = stub.getTrades(new DatumUpitRMI(symbol, date));
        System.out.println("\n  Realized trades: " + symbol);
        System.out.println("  ------------------------------------------------------------");
        if (result.getTrades().isEmpty()) {
            System.out.println("  There are no recorded trades for this day.");
            System.out.println("  [ADVICE] Start the bots and try again.");
        } else {
            result.getTrades().forEach(t -> System.out.println("  " + t.toString()));
        }
        cekajEnter();
    }

    private void cekajEnter() {
        System.out.print("\n[Press ENTER for going back to the menu...] ");
        scanner.nextLine();
    }
}