import java.rmi.Naming;

/**
 * Created by becheru on 12/01/2016.
 */
public class Client {

    SellerInterface sellerInterface;
    BidderInterface bidderInterface;
    int userId = 19921;

    public Client() throws Exception {
        sellerInterface = (SellerInterface) Naming.lookup("rmi://localhost/AuctionServer");
        bidderInterface = (BidderInterface) Naming.lookup("rmi://localhost/AuctionServer");

        wait(1000);
        //sellerInterface.createListing(userId,new Auction("test item",userId,10,100));

    }



    public void wait(int i){
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        try {
            new Client();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
