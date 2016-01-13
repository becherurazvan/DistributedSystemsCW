import KeyUtils.ConvertUtils;
import KeyUtils.KeyUtil;

import javax.crypto.*;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.*;

/**
 * Created by becheru on 12/01/2016.
 */
public class SellerClient {

    SellerInterface serverInterface;
    BidderInterface bidderInterface;
    String id="1";

    SecretKey sessionKey;
    PublicKey serverPublicKey;
    PrivateKey myPrivateKey;
    private SecureRandom rnd;

    public SellerClient() throws Exception {
        init();


        //Thread.sleep(4000);

        System.out.println(bidderInterface.getAllAuctions());

        bid(4,5);
    }

    public void bid(int auctionId, double value) throws Exception {
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(KeyUtil.getPrivateKey("3"));
        SignedObject signedId = new SignedObject(id,KeyUtil.getPrivateKey("3"),signature);
        System.out.println(bidderInterface.bid(signedId, auctionId, value));
    }

    public void addAuction(String itemName, double startingPrice, double minimumPrice){
        try {
            Auction a = new Auction(itemName,startingPrice,minimumPrice, ConvertUtils.getAsInt(id));
            Cipher cipher  = Cipher.getInstance(sessionKey.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE,sessionKey);
            SealedObject sealedAuction = new SealedObject(a,cipher);
            System.out.println(serverInterface.createListing(id,sealedAuction));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelAuction(int auctionId){ // encrypt my ID, only I could have encrypted it with the key that is asociated on the server side with the key of the id of the owner.
        try {


            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(myPrivateKey);


            SignedObject signedId = new SignedObject(auctionId,myPrivateKey,signature);
            System.out.println("Trying to cancel auction with id " + auctionId + " --- " + serverInterface.cancelAuction(signedId, auctionId));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init() throws Exception{
        myPrivateKey = KeyUtil.getPrivateKey("1");
        if(myPrivateKey==null){
            System.out.println("User not registered");
            System.exit(1);
        }
        System.out.println("Loaded your PrivateKey");
        serverPublicKey = KeyUtil.getPublicKey(KeyUtil.SERVER_KEY_NAME);
        System.out.println("Loaded servers PublicKey");
        serverInterface = (SellerInterface) Naming.lookup("rmi://localhost/AuctionServer");
        bidderInterface = (BidderInterface) Naming.lookup("rmi://localhost/AuctionServer");
        System.out.println("Connected To The Server");
        rnd = new SecureRandom();
        doTheHandshake();
    }

    public void doTheHandshake(){
        String challange = new BigInteger(128,rnd).toString(32); //
        System.out.println("Challange for server "+challange);

        try {
            Cipher challangeEncryptCipher = Cipher.getInstance(serverPublicKey.getAlgorithm());
            challangeEncryptCipher.init(Cipher.ENCRYPT_MODE,serverPublicKey);

            SealedObject sealedChallangeForServer = new SealedObject(challange,challangeEncryptCipher);

            SealedObject serverAnswer = serverInterface.challangeServer(id, sealedChallangeForServer);
            Cipher decryptCipher = Cipher.getInstance(myPrivateKey.getAlgorithm());
            decryptCipher.init(Cipher.DECRYPT_MODE,myPrivateKey);

            ServerChallange serverChallange = (ServerChallange)serverAnswer.getObject(decryptCipher);

            System.out.print("Server answered: " + serverChallange.getChallangeAnswer() + ", ");
            if(serverChallange.getChallangeAnswer().equals(challange)){
                System.out.println("Server authenitcated succesfully");
            }else
                return;

            sessionKey = serverChallange.getSessionKey();

            Cipher answerEncyrptCipher = Cipher.getInstance(sessionKey.getAlgorithm());
            answerEncyrptCipher.init(Cipher.ENCRYPT_MODE,sessionKey);
            SealedObject sealedAnswer = new SealedObject(serverChallange.getChallangeForClient(),answerEncyrptCipher);

            serverInterface.answerChallange(id,sealedAnswer);







        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            System.err.println("Server failed authentication, Exiting...");
            System.exit(1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args){
        try {

            new SellerClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
