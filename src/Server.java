import KeyUtils.KeyUtil;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.PrivateKey;

public class Server {

    public static PrivateKey serverPrivateKey;

    public Server() {

       serverPrivateKey= KeyUtil.getPrivateKey(KeyUtil.SERVER_KEY_NAME);
        try {

            LocateRegistry.createRegistry(1099);

            AuctionImplementation auctionImplementation = new AuctionImplementation();
            Naming.rebind("rmi://localhost/AuctionServer", (SellerInterface)auctionImplementation);
            Naming.rebind("rmi://localhost/AuctionServer", (BidderInterface)auctionImplementation);

            System.out.println("Server Started");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        new Server();
    }

}
