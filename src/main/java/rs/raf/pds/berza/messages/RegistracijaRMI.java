package rs.raf.pds.berza.messages;

import java.io.Serializable;

public class RegistracijaRMI implements Serializable {
    private static final long serialVersionUID = 1L;

    private String clientName;

    public RegistracijaRMI() {}
    public RegistracijaRMI(String clientName) { this.clientName = clientName; }

    public String getClientName() { return clientName; }
}
