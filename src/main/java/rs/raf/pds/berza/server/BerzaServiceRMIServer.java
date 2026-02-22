package rs.raf.pds.berza.server;

import rs.raf.pds.berza.messages.*;
import rs.raf.pds.berza.service.BerzaService;
import rs.raf.pds.berza.service.BerzaServiceInterface;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class BerzaServiceRMIServer implements BerzaServiceInterface {

    private final BerzaService berzaService;

    public BerzaServiceRMIServer(BerzaService berzaService) {
        this.berzaService = berzaService;
    }

    @Override
    public String registerClient(RegistracijaRMI registracija) throws RemoteException {
        return berzaService.registerClient(registracija.getClientName());
    }

    @Override
    public NizStockDataRMI getStocks() throws RemoteException {
        return new NizStockDataRMI(berzaService.getStocks());
    }

    @Override
    public NizOrderRMI getAskOrders(StockUpitRMI upit) throws RemoteException {
        return new NizOrderRMI(berzaService.getAskOrders(upit.getSymbol(), upit.getCount()));
    }

    @Override
    public NizOrderRMI getBidOrders(StockUpitRMI upit) throws RemoteException {
        return new NizOrderRMI(berzaService.getBidOrders(upit.getSymbol(), upit.getCount()));
    }

    @Override
    public String placeOrder(OrderRMI order) throws RemoteException {
        return berzaService.placeOrder(order);
    }

    @Override
    public NizTradeRMI getTrades(DatumUpitRMI upit) throws RemoteException {
        return new NizTradeRMI(berzaService.getTrades(upit.getSymbol(), upit.getDate()));
    }

    @Override
    public PortfolioRMI getPortfolio(String clientId) throws RemoteException {
        return new PortfolioRMI(clientId, berzaService.getPortfolio(clientId));
    }

    public static void main(String[] args) throws Exception {
        System.out.println("NASDAQ Stock Market RMI Server");

        BerzaService berzaService = new BerzaService();

        TradeArchiver archiver = new TradeArchiver();
        archiver.start();
        berzaService.setTradeListener(trade -> archiver.archiveTrade(trade));

        BerzaSocketServer socketServer = new BerzaSocketServer(berzaService);
        socketServer.start();

        StockSimulator simulator = new StockSimulator(berzaService, socketServer);
        simulator.start();

        BerzaServiceRMIServer server = new BerzaServiceRMIServer(berzaService);

        BerzaServiceInterface stub = (BerzaServiceInterface)
                UnicastRemoteObject.exportObject(server, 0);

        Registry registry = LocateRegistry.createRegistry(1099);
        registry.rebind("BerzaService", stub);

        System.out.println("[RMI] Service is registered as 'BerzaService' on port 1099");
        System.out.println("[SOCKET] Feed server on port " + BerzaSocketServer.SOCKET_PORT);
        System.out.println("[SERVER] Ready for clients!");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[SERVER] Shutting down...");
            simulator.stopSimulator();
            socketServer.stopServer();
            archiver.shutdown();
        }));
    }
}
