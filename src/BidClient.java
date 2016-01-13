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
import java.util.List;

/**
 * Created by becheru on 12/01/2016.
 */
public class BidClient extends Client {


    BidderInterface bidderInterface;

    SecretKey sessionKey;
    PublicKey serverPublicKey;
    PrivateKey myPrivateKey;
    private SecureRandom rnd;

    public BidClient() throws Exception {
        super();
    }

    @Override
    protected void inputDetected(List<String> input) {
        switch (input.get(0)) {
            case "bid":
                if(input.size() < 3 || input.size() > 3) {
                    System.out.println("Wrong format --> bid --bid_id --bid_value");
                    break;
                }
                if(!isInt(input.get(1))) {
                    System.out.println(input.get(1) + " must be a number!");
                    break;
                }else if(!isDouble(input.get(2))) {
                    System.out.println(input.get(2) + " must be a number!");
                    break;
                }
                try {
                    String response = bid(Integer.parseInt(input.get(1)), Double.parseDouble(input.get(2)));
                    System.out.println(response);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }catch(NumberFormatException e){
                    System.out.println("Wrong Formating, try again");
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "list_auctions":
                try {
                    System.out.println(bidderInterface.getAllAuctions());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;

            default:
                System.err.println("No such command, try: bid --bid_id --bid_value, list_auctions");

        }
    }

    public String bid(int auctionId, double value) throws Exception {

        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(myPrivateKey);

        Object[] bidDetails = new Object[]{id,auctionId,value};
        SignedObject signedDetails = new SignedObject(bidDetails, myPrivateKey, signature);
        return   bidderInterface.bid(signedDetails);
    }


    public void init() throws Exception {
        myPrivateKey = KeyUtil.getPrivateKey(id);
        if (myPrivateKey == null) {
            System.out.println("User not registered");
            System.exit(1);
        }
        System.out.println("Loaded your PrivateKey");
        serverPublicKey = KeyUtil.getPublicKey(KeyUtil.SERVER_KEY_NAME);
        System.out.println("Loaded servers PublicKey");
        bidderInterface = (BidderInterface) Naming.lookup("rmi://localhost/AuctionServer");
        System.out.println("Connected To The Server");
        rnd = new SecureRandom();
        doTheHandshake();
    }

    public void doTheHandshake() {
        String challange = new BigInteger(128, rnd).toString(32); //
        System.out.println("Challange for server " + challange);

        try {
            Cipher challangeEncryptCipher = Cipher.getInstance(serverPublicKey.getAlgorithm());
            challangeEncryptCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);

            SealedObject sealedChallangeForServer = new SealedObject(challange, challangeEncryptCipher);

            SealedObject serverAnswer = bidderInterface.challangeServer(id, sealedChallangeForServer);
            Cipher decryptCipher = Cipher.getInstance(myPrivateKey.getAlgorithm());
            decryptCipher.init(Cipher.DECRYPT_MODE, myPrivateKey);

            ServerChallange serverChallange = (ServerChallange) serverAnswer.getObject(decryptCipher);

            System.out.print("Server answered: " + serverChallange.getChallangeAnswer() + ", ");
            if (serverChallange.getChallangeAnswer().equals(challange)) {
                System.out.println("Server authenitcated succesfully");
            } else
                return;

            sessionKey = serverChallange.getSessionKey();

            Cipher answerEncyrptCipher = Cipher.getInstance(sessionKey.getAlgorithm());
            answerEncyrptCipher.init(Cipher.ENCRYPT_MODE, sessionKey);
            SealedObject sealedAnswer = new SealedObject(serverChallange.getChallangeForClient(), answerEncyrptCipher);

            bidderInterface.answerChallange(id, sealedAnswer);


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


    public static void main(String[] args) {
        try {

            new BidClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
