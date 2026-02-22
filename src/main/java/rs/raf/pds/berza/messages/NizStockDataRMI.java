package rs.raf.pds.berza.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NizStockDataRMI implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<StockDataRMI> stocks;

    public NizStockDataRMI() {
        this.stocks = new ArrayList<>();
    }

    public NizStockDataRMI(List<StockDataRMI> stocks) {
        this.stocks = stocks;
    }

    public List<StockDataRMI> getStocks() { return stocks; }
    public void addStock(StockDataRMI s) { stocks.add(s); }
}
