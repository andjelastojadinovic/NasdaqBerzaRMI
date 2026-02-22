package rs.raf.pds.berza.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NizOrderRMI implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<OrderRMI> orders;

    public NizOrderRMI() { this.orders = new ArrayList<>(); }
    public NizOrderRMI(List<OrderRMI> orders) { this.orders = orders; }

    public List<OrderRMI> getOrders() { return orders; }
    public void addOrder(OrderRMI o) { orders.add(o); }
}
