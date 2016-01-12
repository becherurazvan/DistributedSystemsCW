package Server;

import java.rmi.RemoteException;

/**
 * Created by becheru on 12/01/2016.
 */
public class AuctionImplementation  extends java.rmi.server.UnicastRemoteObject implements  SellerInterface,BidderInterface {

    public AuctionImplementation() throws RemoteException {

    }

    @Override
    public String bid(int auctionId, int bidderId, double amount) throws RemoteException {
        return null;
    }

    @Override
    public int createListing(String name, double startingPrice, double minPrice, int ownerId) throws RemoteException {
        return 0;
    }

    @Override
    public String cancelAuction(int auctionId, int ownerId) throws RemoteException {
        return null;
    }

    @Override
    public String[] printAuctions() throws RemoteException {
        return new String[0];
    }
}
