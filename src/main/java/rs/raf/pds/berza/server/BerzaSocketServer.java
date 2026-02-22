package rs.raf.pds.berza.server;

import rs.raf.pds.berza.messages.TradeRMI;
import rs.raf.pds.berza.service.BerzaService;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class BerzaSocketServer extends Thread {

    public static final int SOCKET_PORT = 5556;

    private final BerzaService berzaService;
    private final Map<String, ObjectOutputStream> clientStreams = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public BerzaSocketServer(BerzaService berzaService) {
        this.berzaService = berzaService;
        setName("BerzaSocketServer");
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(SOCKET_PORT);
            System.out.println("[SOCKET] Listening to server on port " + SOCKET_PORT);
            while (running) {
                try {
                    Socket s = serverSocket.accept();
                    pool.submit(new ClientHandler(s));
                } catch (SocketException e) {
                    if (running) System.err.println("[SOCKET] " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[SOCKET] Error: " + e.getMessage());
        }
    }

    public void broadcastFeed(String symbol, int hour, double price, double change) {
        String msg = String.format("FEED:%s|%d|%.2f|%.2f", symbol, hour, price, change);
        Set<String> subs = subscriptions.getOrDefault(symbol, Collections.emptySet());
        for (String clientId : subs) sendToClient(clientId, msg);
    }

    public void broadcastTrade(TradeRMI trade) {
        String msg = String.format("TRADE:%s|%.2f|%d", trade.getSymbol(), trade.getPrice(), trade.getQuantity());
        Set<String> subs = subscriptions.getOrDefault(trade.getSymbol(), Collections.emptySet());
        for (String clientId : subs) sendToClient(clientId, msg);
    }

    public void notifyClient(String clientId, String message) {
        sendToClient(clientId, "NOTIFY:" + message);
    }

    private void sendToClient(String clientId, String msg) {
        ObjectOutputStream oos = clientStreams.get(clientId);
        if (oos != null) {
            try {
                synchronized (oos) { oos.writeObject(msg); oos.flush(); }
            } catch (IOException e) {
                clientStreams.remove(clientId);
            }
        }
    }

    public void stopServer() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException e) {}
        pool.shutdown();
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private String clientId;
        private ObjectOutputStream oos;

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try {
                oos = new ObjectOutputStream(socket.getOutputStream());
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                String init = (String) ois.readObject();
                if (init.startsWith("REGISTER:")) {
                    clientId = init.substring(9);
                    clientStreams.put(clientId, oos);
                    sendToClient(clientId, "WELCOME:" + clientId);
                    System.out.println("[SOCKET] Client connected: " + clientId);
                }

                while (running && !socket.isClosed()) {
                    try {
                        String cmd = (String) ois.readObject();
                        handleCmd(cmd);
                    } catch (EOFException | SocketException e) { break; }
                }
            } catch (IOException | ClassNotFoundException e) {
            	
            } finally {
                if (clientId != null) {
                    clientStreams.remove(clientId);
                    subscriptions.values().forEach(s -> s.remove(clientId));
                    System.out.println("[SOCKET] Client disconnected: " + clientId);
                }
                try { socket.close(); } catch (IOException e) {}
            }
        }

        private void handleCmd(String cmd) {
            if (cmd.startsWith("SUBSCRIBE:")) {
                for (String sym : cmd.substring(10).split(",")) {
                    subscriptions.computeIfAbsent(sym.trim().toUpperCase(),
                            k -> ConcurrentHashMap.newKeySet()).add(clientId);
                }
                sendToClient(clientId, "SUBSCRIBED:OK");
            } else if (cmd.equals("PING")) {
                sendToClient(clientId, "PONG");
            }
        }
    }
}
