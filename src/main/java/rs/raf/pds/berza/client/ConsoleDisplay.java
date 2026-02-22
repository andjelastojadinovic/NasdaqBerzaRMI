package rs.raf.pds.berza.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConsoleDisplay {

    public static final String ANSI_RESET    = "\u001B[0m";
    public static final String ANSI_GREEN    = "\u001B[32m";
    public static final String ANSI_RED      = "\u001B[31m";
    public static final String ANSI_YELLOW   = "\u001B[33m";
    public static final String ANSI_BOLD     = "\u001B[1m";
    public static final String ANSI_CYAN     = "\u001B[36m";
    public static final String ANSI_BG_BLACK = "\u001B[40m";

    private static final int DATA_START_ROW = 3;
    private static final int TICKER_WIDTH   = 120; 

    private static final Map<String, Integer> symbolRow = new LinkedHashMap<>();
    private static int nextDataRow = DATA_START_ROW;
    private static boolean initialized = false;

    private static volatile boolean inputMode  = false;
    private static volatile boolean feedActive = false;

    private static final Map<String, Double> tickerPrices  = new ConcurrentHashMap<>();
    private static final Map<String, Double> tickerChanges = new ConcurrentHashMap<>();

    private static TickerThread tickerThread = null;

    public static void setInputMode(boolean mode) {
        inputMode = mode;
    }

    public static void setFeedActive(boolean active) {
        feedActive = active;
        if (active) {
            startTicker();
        } else {
            stopTicker();
        }
    }

    private static void startTicker() {
        if (tickerThread != null && tickerThread.isAlive()) return;
        tickerThread = new TickerThread();
        tickerThread.setDaemon(true);
        tickerThread.start();
    }

    private static void stopTicker() {
        if (tickerThread != null) {
            tickerThread.stopTicker();
            tickerThread = null;
        }
    }

    private static class TickerThread extends Thread {

        private volatile boolean running = true;
        private int offset = 0;

        TickerThread() {
            setDaemon(true);
            setName("TickerThread");
        }

        public void stopTicker() {
            running = false;
            interrupt();
        }

        @Override
        public void run() {
            while (running) {
                try {
                    if (feedActive && initialized) {
                        drawTicker();
                    }
                    Thread.sleep(120);
                    offset++;
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private void drawTicker() {
            String tickerContent = buildTickerString();
            if (tickerContent.isEmpty()) return;

            String plain = stripAnsi(tickerContent);
            if (plain.length() < 2) return;

            String doubled      = tickerContent + "     " + tickerContent;
            String doubledPlain = plain + "     " + plain;

            int loopLen = plain.length() + 5; 
            int pos = offset % loopLen;

            String visible = extractVisible(doubled, doubledPlain, pos, TICKER_WIDTH);

            int tickerRow = nextDataRow + 3;
            synchronized (ConsoleDisplay.class) {
                System.out.printf("\u001B[%d;1H\u001B[2K", tickerRow);
                System.out.print(ANSI_BG_BLACK + ANSI_BOLD);
                System.out.print(visible);
                System.out.print(ANSI_RESET);
                System.out.printf("\u001B[%d;1H", nextDataRow + 2);
                System.out.flush();
            }
        }

        private String buildTickerString() {
            if (tickerPrices.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Double> entry : tickerPrices.entrySet()) {
                String symbol = entry.getKey();
                double price  = entry.getValue();
                double change = tickerChanges.getOrDefault(symbol, 0.0);
                boolean up    = change >= 0;
                String color  = up ? ANSI_GREEN : ANSI_RED;
                String arrow  = up ? "\u2191" : "\u2193";

                sb.append(ANSI_CYAN).append(ANSI_BOLD).append(symbol).append(ANSI_RESET)
                  .append(ANSI_BG_BLACK)
                  .append(" ").append(String.format("%.2f", price)).append(" ")
                  .append(color).append(String.format("%+.2f%%", change))
                  .append(arrow).append(ANSI_RESET)
                  .append(ANSI_BG_BLACK).append("   ");
            }
            return sb.toString();
        }

        private String stripAnsi(String s) {
            return s.replaceAll("\u001B\\[[;\\d]*m", "");
        }

        private String extractVisible(String colored, String plain, int pos, int width) {
            StringBuilder result = new StringBuilder();
            int plainIdx = 0;
            int i = 0;
            int count = 0;

            while (i < colored.length() && count < width) {
                if (colored.charAt(i) == '\u001B'
                        && i + 1 < colored.length()
                        && colored.charAt(i + 1) == '[') {
                    int end = colored.indexOf('m', i);
                    if (end != -1) {
                        if (plainIdx >= pos) result.append(colored, i, end + 1);
                        i = end + 1;
                        continue;
                    }
                }
                if (plainIdx >= pos) {
                    result.append(colored.charAt(i));
                    count++;
                }
                plainIdx++;
                i++;
            }
            while (count < width) { result.append(' '); count++; }
            return result.toString();
        }
    }

    private static synchronized void ensureInitialized() {
        if (!initialized) {
            System.out.print("\u001B[2J\u001B[H");
            System.out.flush();
            System.out.print(ANSI_BOLD + ANSI_CYAN);
            System.out.println("  SYMBOL   PRICE($)        CHANGE         1H             24H            7D");
            System.out.println("  -------+------------+---------------+---------------+---------------+---------------");
            System.out.print(ANSI_RESET);
            System.out.flush();
            initialized = true;
        }
    }

    public static synchronized void displayStock(String symbol,
                                                 double currentPrice,
                                                 double changeFromOpen,
                                                 double price1h,
                                                 double openPrice,
                                                 double price7d) {
        if (!feedActive) return;

        ensureInitialized();

        if (!symbolRow.containsKey(symbol)) {
            symbolRow.put(symbol, nextDataRow++);
        }
        int row = symbolRow.get(symbol);

        double chPct = openPrice != 0 ? changeFromOpen / openPrice * 100 : 0;
        double ch1h  = price1h   != 0 ? (currentPrice - price1h)  / price1h  * 100 : 0;
        double ch24h = chPct;
        double ch7d  = price7d   != 0 ? (currentPrice - price7d)  / price7d  * 100 : 0;

        boolean up   = chPct >= 0;
        String color = up ? ANSI_GREEN : ANSI_RED;
        String arrow = up ? "\u2191" : "\u2193";

        System.out.printf("\u001B[%d;1H\u001B[2K", row);
        System.out.printf("  %-5s  %s%-11.2f%s %s%+8.2f%% %s%s  %s  %s  %s",
                symbol,
                color, currentPrice, ANSI_RESET,
                color, chPct, arrow, ANSI_RESET,
                fmt(ch1h), fmt(ch24h), fmt(ch7d));
        System.out.flush();

        tickerPrices.put(symbol, currentPrice);
        tickerChanges.put(symbol, chPct);

        System.out.printf("\u001B[%d;1H", nextDataRow + 2);
        System.out.flush();
    }

    private static String fmt(double pct) {
        String c = pct >= 0 ? ANSI_GREEN : ANSI_RED;
        String a = pct >= 0 ? "\u2191" : "\u2193";
        return String.format("%s%+7.2f%% %s%s  ", c, pct, a, ANSI_RESET);
    }

    public static void printHeader() {
        ensureInitialized();
    }

    public static void printMeniSeparator() {
        if (!initialized) return;
        System.out.printf("\u001B[%d;1H", nextDataRow + 1);
        System.out.println("  -------+------------+---------------+---------------+---------------+---------------");
        System.out.flush();
    }

    public static void moveCursorToMeni() {
        System.out.printf("\u001B[%d;1H", nextDataRow + 2);
        System.out.flush();
    }

    public static void clearScreen() {
        stopTicker();
        initialized = false;
        symbolRow.clear();
        nextDataRow = DATA_START_ROW;
        tickerPrices.clear();
        tickerChanges.clear();
        System.out.print("\u001B[2J\u001B[H");
        System.out.flush();
    }

    public static int getTotalSymbols() {
        return symbolRow.size();
    }
}