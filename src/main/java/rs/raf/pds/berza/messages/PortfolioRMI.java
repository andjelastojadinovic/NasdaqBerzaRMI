package rs.raf.pds.berza.messages;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PortfolioRMI implements Serializable {
    private static final long serialVersionUID = 1L;

    private String clientId;
    private Map<String, Integer> akcije; 

    public PortfolioRMI() { this.akcije = new HashMap<>(); }
    public PortfolioRMI(String clientId, Map<String, Integer> akcije) {
        this.clientId = clientId;
        this.akcije = akcije;
    }

    public String getClientId() { return clientId; }
    public Map<String, Integer> getAkcije() { return akcije; }
}
