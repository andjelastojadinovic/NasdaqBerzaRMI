package rs.raf.pds.berza.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NizTradeRMI implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<TradeRMI> trades;

    public NizTradeRMI() { this.trades = new ArrayList<>(); }
    public NizTradeRMI(List<TradeRMI> trades) { this.trades = trades; }

    public List<TradeRMI> getTrades() { return trades; }
    public void addTrade(TradeRMI t) { trades.add(t); }
}
