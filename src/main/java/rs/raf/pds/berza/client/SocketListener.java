package rs.raf.pds.berza.client;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SocketListener extends Thread {

    private final String clientId;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private Socket socket;
    private volatile boolean running = true;

    private volatile boolean displayFeed;

    private final Map<String, Double> openPrices    = new ConcurrentHashMap<>();
    private final Map<String, Double> price1hAgo    = new ConcurrentHashMap<>();
    private final Map<String, Double> price7dAgo    = new ConcurrentHashMap<>();
    private final Map<String, Double> currentPrices = new ConcurrentHashMap<>();
    private final Map<String, java.util.Deque<Double>> history = new ConcurrentHashMap<>();

    public SocketListener(String clientId, String host, int port) throws IOException {
        this(clientId, host, port, true);
    }

    public SocketListener(String clientId, String host, int port, boolean displayFeed) throws IOException {
        this.clientId = clientId;
        this.displayFeed = displayFeed;

        socket = new Socket(host, port);

        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();

        ois = new ObjectInputStream(socket.getInputStream());

        setDaemon(true);
        setName("SocketListener-" + clientId);
    }

    public void register() throws IOException {
        oos.writeObject("REGISTER:" + clientId);
        oos.flush();
    }

    public void subscribe(String... symbols) throws IOException {
        oos.writeObject("SUBSCRIBE:" + String.join(",", symbols));
        oos.flush();
    }

    @Override
    public void run() {
        try {
            while (running && !socket.isClosed()) {
                try {
                    String msg = (String) ois.readObject();
                    handleMsg(msg);
                } catch (EOFException | SocketException e) {
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                System.err.println("[SOCKET ERROR] " + e.getMessage());
            }
        }
    }

    private void handleMsg(String msg) {

        if (msg.startsWith("FEED:")) {

            String[] p = msg.substring(5).split("\\|");
            if (p.length < 4) return;

            String symbol = p[0];
            double price  = Double.parseDouble(p[2]);
            double change = Double.parseDouble(p[3]);

            openPrices.putIfAbsent(symbol, price);
            price7dAgo.putIfAbsent(symbol, price * (1 + (Math.random() * 0.14 - 0.07)));

            java.util.Deque<Double> h = history.computeIfAbsent(
                    symbol, k -> new java.util.ArrayDeque<>());

            price1hAgo.put(symbol, h.isEmpty() ? price : h.peekFirst());

            h.addLast(price);
            if (h.size() > 60) {
                h.pollFirst();
            }

            currentPrices.put(symbol, price);

            if (displayFeed) {
                ConsoleDisplay.displayStock(
                        symbol,
                        price,
                        change,
                        price1hAgo.getOrDefault(symbol, price),
                        openPrices.getOrDefault(symbol, price),
                        price7dAgo.getOrDefault(symbol, price)
                );
            }

        } else if (msg.startsWith("NOTIFY:")) {

            if (displayFeed) {
                ConsoleDisplay.moveCursorToMeni();
                System.out.println("\n" + ConsoleDisplay.ANSI_YELLOW
                        + "[NOTIFY] " + msg.substring(7)
                        + ConsoleDisplay.ANSI_RESET);
                ConsoleDisplay.moveCursorToMeni();
            }

        } else if (msg.startsWith("TRADE:")) {

            if (displayFeed) {
                String[] p = msg.substring(6).split("\\|");
                if (p.length >= 3) {
                    ConsoleDisplay.moveCursorToMeni();
                    System.out.printf("%n[TRADE] %s | $%.2f | %s akcija%n",
                            p[0],
                            Double.parseDouble(p[1]),
                            p[2]);
                    ConsoleDisplay.moveCursorToMeni();
                }
            }
        }
    }

    public void setDisplayFeed(boolean display) {
        this.displayFeed = display;
    }

    public void disconnect() {
        running = false;
        try { socket.close(); } catch (IOException ignored) {}
    }

    public Map<String, Double> getCurrentPrices() {
        return currentPrices;
    }
}