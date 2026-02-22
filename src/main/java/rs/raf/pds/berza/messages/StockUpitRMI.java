package rs.raf.pds.berza.messages;

import java.io.Serializable;
import java.util.Date;

public class StockUpitRMI implements Serializable {
    private static final long serialVersionUID = 1L;

    private String symbol;
    private int count;

    public StockUpitRMI() {}
    public StockUpitRMI(String symbol, int count) {
        this.symbol = symbol;
        this.count = count;
    }

    public String getSymbol() { return symbol; }
    public int getCount() { return count; }
}
