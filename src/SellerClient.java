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
public class SellerClient extends Client {

    SellerInterface serverInterface;

    SecretKey sessionKey;
    PublicKey serverPublicKey;
    PrivateKey myPrivateKey;
    private SecureRandom rnd;

    public SellerClient() throws Exception {
        super();
    }


    @Override
    protected void inputDetected(List<String> input) {
        switch (input.get(0)) {
            case "add_auction":
                if (input.size() < 3 || input.size() > 4) {
                    System.err.println("Wrong formating e.g. add_auction name starting_price minimum_price(optional)");
                    break;
                }

                if (!isDouble(input.get(2)))
                    System.err.println(input.get(2) + " --> starting price needs to be a number");
                else if (input.size() > 3 && !isDouble(input.get(3)))
                    System.err.println(input.get(3) + " --> minimum price needs to be a number");
                else {
                    try {
                        if (input.size() > 3 && Double.parseDouble(input.get(3)) < Double.parseDouble(input.get(2))) {
                            System.err.println("Minimum price cannot be lower than starting price, you can just not add it");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Wrong Formating, try again");
                        break;
                    }
                    if (input.size() > 3) {
                        try {
                            if (Double.parseDouble(input.get(2)) <= 0) {
                                System.out.println("Starting price must be bigger than 0");
                                break;
                            }
                            String response = addAuction(input.get(1), Double.parseDouble(input.get(2)), Double.parseDouble(input.get(3)));
                            System.out.println(response);
                        } catch (NumberFormatException e) {
                            System.out.println("Wrong Formating, try again");
                            break;
                        }
                    } else {
                        try {
                            String response = addAuction(input.get(1), Double.parseDouble(input.get(2)), Double.parseDouble(input.get(2))); // if no min price is set, set it to starting price
                            System.out.println(response);
                        } catch (NumberFormatException e) {
                            System.out.println("Wrong Formating, try again");
                            break;
                        }
                    }
                }

                break;
            case "close_auction":

                if (input.size() < 2 || input.size() > 2) {
                    System.err.println("Wrong formating e.g. close_auction auctionId");
                    break;
                }
                if (!isInt(input.get(1))) {
                    System.err.println(input.get(1) + " --> auction id must be a number");
                    break;
                }
                try {
                    String response = cancelAuction(Integer.parseInt(input.get(1)));
                    System.out.println(response);
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Wrong Formating, try again");
                    break;
                }

            case "list_auctions":
                try {
                    System.out.println(serverInterface.getAllAuctions());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case "spamAuctions":

                int i =0;
                while(true){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(addAuction("SpamAuciton_"+i,10,i));
                }
            default:
                System.err.println("No such command, try: add_auction -name -starting price -minimum price , close_auction -auction id, list_auctions");

        }
    }

    public String addAuction(String itemName, double startingPrice, double minimumPrice) {
        String response = "No response";
        try {
            Auction a = new Auction(itemName, startingPrice, minimumPrice, id);
            Cipher cipher = Cipher.getInstance(sessionKey.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
            SealedObject sealedAuction = new SealedObject(a, cipher);
            response = serverInterface.createListing(id, sealedAuction);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public String cancelAuction(int auctionId) { // encrypt my ID, only I could have encrypted it with the key that is asociated on the server side with the key of the id of the owner.
        String response = "";
        try {


            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(myPrivateKey);


            SignedObject signedId = new SignedObject(id, myPrivateKey, signature);
            response = "Trying to cancel auction with id " + auctionId + " --- " + serverInterface.cancelAuction(signedId, auctionId);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
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
        serverInterface = (SellerInterface) Naming.lookup("rmi://localhost/AuctionServer");
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

            SealedObject serverAnswer = serverInterface.challangeServer(id, sealedChallangeForServer);
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

            serverInterface.answerChallange(id, sealedAnswer);


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

            new SellerClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
