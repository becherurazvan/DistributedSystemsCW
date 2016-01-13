import javax.crypto.SealedObject;
import java.rmi.RemoteException;
import java.security.SignedObject;

/**
 * Created by becheru on 12/01/2016.
 */
public interface BidderInterface extends java.rmi.Remote  {


    // check if the bid is actually done by whoever says is doing the bid,  maybe change the rest of the values to a SignedObject
    public String bid(SignedObject requesterId, int auctionId, double amount) throws RemoteException;

    public String getAllAuctions() throws RemoteException;


    // method to be called when you want to challange the server to prove that it is the actual server
    public SealedObject challangeServer(String id,SealedObject challange) throws RemoteException;

    //method to be called when you solved server's challange
    public boolean answerChallange(String id, SealedObject response) throws RemoteException;

}
