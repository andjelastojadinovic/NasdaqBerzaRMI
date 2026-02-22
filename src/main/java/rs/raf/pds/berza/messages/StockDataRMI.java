package rs.raf.pds.berza.messages;

import java.io.Serializable;
import java.util.Date;

public class StockDataRMI implements Serializable {
    private static final long serialVersionUID = 1L;

    private String symbol;       
    private String companyName;
    private double openPrice;     
    private double currentPrice;
    private double change;        
    private Date date;

    public StockDataRMI() {}

    public StockDataRMI(String symbol, String companyName, double openPrice, double currentPrice, double change, Date date) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.openPrice = openPrice;
        this.currentPrice = currentPrice;
        this.change = change;
        this.date = date;
    }

    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public double getOpenPrice() { return openPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public double getChange() { return change; }
    public Date getDate() { return date; }

    @Override
    public String toString() {
        return String.format("%-5s | %-20s | Open: %.2f | Trenutna: %.2f | Promena: %+.2f",
                symbol, companyName, openPrice, currentPrice, change);
    }
}
