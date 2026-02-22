package rs.raf.pds.berza.server;

import rs.raf.pds.berza.messages.StockDataRMI;
import rs.raf.pds.berza.service.BerzaService;

import java.util.Random;

public class StockSimulator extends Thread {

    private static final long SIM_HOUR_MS = 6_000L;
    private static final int START_HOUR = 9;
    private static final int END_HOUR = 16;

    private final BerzaService berzaService;
    private final BerzaSocketServer socketServer;
    private final Random random = new Random();
    private volatile boolean running = true;

    public StockSimulator(BerzaService berzaService, BerzaSocketServer socketServer) {
        this.berzaService = berzaService;
        this.socketServer = socketServer;
        setDaemon(true);
        setName("StockSimulator");
    }

    @Override
    public void run() {
        System.out.println("[SIMULATOR] Stock market simulation started. 1 hour = " + (SIM_HOUR_MS/1000) + " seconds.");
        int simHour = START_HOUR;

        while (running) {
            try {
                simulirajSat(simHour);
                Thread.sleep(SIM_HOUR_MS);

                simHour++;
                if (simHour > END_HOUR) {
                    simHour = START_HOUR;
                    System.out.println("[SIMULATOR] New day on the stock market");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void simulirajSat(int hour) {
        for (StockDataRMI stock : berzaService.getStocksMap().values()) {
            double changePct = (random.nextDouble() * 4.0 - 2.0) / 100.0;
            double newPrice = Math.round(stock.getCurrentPrice() * (1 + changePct) * 100.0) / 100.0;
            if (newPrice <= 0) newPrice = stock.getCurrentPrice();

            berzaService.updateStockPrice(stock.getSymbol(), newPrice);

            socketServer.broadcastFeed(stock.getSymbol(), hour, newPrice,
                    newPrice - stock.getOpenPrice());
        }
    }

    public void stopSimulator() {
        running = false;
        interrupt();
    }
}
