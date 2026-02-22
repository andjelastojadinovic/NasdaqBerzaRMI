package rs.raf.pds.berza.messages;

import java.io.Serializable;
import java.util.Date;

public class TradeRMI implements Serializable {
    private static final long serialVersionUID = 1L;

    private String symbol;
    private double price;
    private int quantity;
    private String buyerClientId;
    private String sellerClientId;
    private Date timestamp;

    public TradeRMI() {}

    public TradeRMI(String symbol, double price, int quantity, String buyerClientId, String sellerClientId) {
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.buyerClientId = buyerClientId;
        this.sellerClientId = sellerClientId;
        this.timestamp = new Date();
    }

    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getBuyerClientId() { return buyerClientId; }
    public String getSellerClientId() { return sellerClientId; }
    public Date getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("%s | %s | %.2f x %d | Buyer: %s | Seller: %s",
                timestamp, symbol, price, quantity, buyerClientId, sellerClientId);
    }
}
