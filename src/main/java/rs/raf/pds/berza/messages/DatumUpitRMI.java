package rs.raf.pds.berza.messages;

import java.io.Serializable;
import java.util.Date;

public class DatumUpitRMI implements Serializable {
    private static final long serialVersionUID = 1L;

    private String symbol;
    private Date date;

    public DatumUpitRMI() {}
    public DatumUpitRMI(String symbol, Date date) {
        this.symbol = symbol;
        this.date = date;
    }

    public String getSymbol() { return symbol; }
    public Date getDate() { return date; }
}
