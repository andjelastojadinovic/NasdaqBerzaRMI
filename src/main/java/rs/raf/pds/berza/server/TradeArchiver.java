package rs.raf.pds.berza.server;

import rs.raf.pds.berza.messages.TradeRMI;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TradeArchiver extends Thread {

    private static final String ARCHIVE_FILE = "trades_archive.txt";
    private final BlockingQueue<TradeRMI> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public TradeArchiver() {
        setDaemon(true);
        setName("TradeArchiver");
    }

    public void archiveTrade(TradeRMI trade) {
        queue.offer(trade);
    }

    @Override
    public void run() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(ARCHIVE_FILE, true))) {
            while (running || !queue.isEmpty()) {
                try {
                    TradeRMI trade = queue.poll(1, TimeUnit.SECONDS);
                    if (trade != null) {
                        writer.println(trade.toString());
                        writer.flush();
                        System.out.println("[ARCHIVE] " + trade.toString());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("[ARCHIVE ERROR] " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
