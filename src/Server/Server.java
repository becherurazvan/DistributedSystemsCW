package Server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/**
 * Created by becheru on 12/01/2016.
 */


//Frontend
public class Server {
    public Server() {

        try {

            LocateRegistry.createRegistry(1099);

            AuctionImplementation auctionImplementation =new AuctionImplementation();
            SellerInterface auctionSellerImplementtion = auctionImplementation;
            BidderInterface auctionBidderInterface =  auctionImplementation;
            Naming.rebind("rmi://localhost/AuctionServer", auctionSellerImplementtion);
            Naming.rebind("rmi://localhost/AuctionServer", auctionBidderInterface);

            System.out.println("Server Started");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        new Server();
    }
}
