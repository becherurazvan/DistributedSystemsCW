import javax.crypto.SealedObject;
import java.rmi.RemoteException;
import java.security.SignedObject;

/**
 * Created by becheru on 12/01/2016.
 */
public interface SellerInterface extends java.rmi.Remote  {


    // create a listing
    public String createListing(String userId, SealedObject sealedAuction) throws RemoteException; // DONE


    //cancel a listing if the message is signed with the private key of the user
    //identified by that requester id
    public String cancelAuction(SignedObject signedRequesterId, int auctionId) throws RemoteException;  // Done

    public String getAllAuctions() throws RemoteException; // DONE


    // method to be called when you want to challange the server to prove that it is the actual server
    public SealedObject challangeServer(String id,SealedObject challange) throws RemoteException;

    //method to be called when you solved server's challange
    public boolean answerChallange(String id, SealedObject response) throws RemoteException;


}
