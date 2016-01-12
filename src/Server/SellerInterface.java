package Server;

import java.rmi.RemoteException;

/**
 * Created by becheru on 12/01/2016.
 */
public interface SellerInterface extends java.rmi.Remote {

    public int createListing(String name, double startingPrice, double minPrice,int ownerId) throws RemoteException;

    public String cancelAuction(int auctionId, int ownerId) throws RemoteException;

    public String[] printAuctions() throws RemoteException;

}