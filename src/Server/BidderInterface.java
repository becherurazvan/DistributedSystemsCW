package Server;

import java.rmi.RemoteException;

/**
 * Created by becheru on 12/01/2016.
 */
public interface BidderInterface  extends java.rmi.Remote {

    public String[] printAuctions() throws RemoteException;

    public String bid(int auctionId, int bidderId,double amount)throws RemoteException;

}
