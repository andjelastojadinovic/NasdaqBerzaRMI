package rs.raf.pds.berza.messages;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class OrderRMI implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum OrderType { BUY, SELL }

    private String orderId;
    private String clientId;
    private String symbol;
    private double price;
    private int quantity;
    private OrderType type;
    private Date timestamp;
    private boolean active;

    public OrderRMI() {}

    public OrderRMI(String clientId, String symbol, double price, int quantity, OrderType type) {
        this.orderId = UUID.randomUUID().toString().substring(0, 8);
        this.clientId = clientId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.type = type;
        this.timestamp = new Date();
        this.active = true;
    }

    public String getOrderId() { return orderId; }
    public String getClientId() { return clientId; }
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public OrderType getType() { return type; }
    public Date getTimestamp() { return timestamp; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return String.format("[%s] %s x%d @ %.2f (ID:%s)", type, symbol, quantity, price, orderId);
    }
}
