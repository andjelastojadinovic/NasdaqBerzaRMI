package rs.raf.pds.berza.service;

import rs.raf.pds.berza.messages.*;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BerzaServiceInterface extends Remote {

    String registerClient(RegistracijaRMI registracija) throws RemoteException;

    NizStockDataRMI getStocks() throws RemoteException;

    NizOrderRMI getAskOrders(StockUpitRMI upit) throws RemoteException;

    NizOrderRMI getBidOrders(StockUpitRMI upit) throws RemoteException;

    String placeOrder(OrderRMI order) throws RemoteException;

    NizTradeRMI getTrades(DatumUpitRMI upit) throws RemoteException;

    PortfolioRMI getPortfolio(String clientId) throws RemoteException;
}
