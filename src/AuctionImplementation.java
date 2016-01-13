import KeyUtils.KeyUtil;
import com.sun.corba.se.impl.protocol.giopmsgheaders.RequestMessage;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.jgroups.*;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.mux.MuxRpcDispatcher;
import org.jgroups.util.RspList;

import javax.crypto.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.security.*;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by becheru on 12/01/2016.
 */
public class AuctionImplementation extends java.rmi.server.UnicastRemoteObject
        implements SellerInterface, BidderInterface, Receiver {

    JChannel channel;
    MuxRpcDispatcher dispatcher;
    Replica r;

    HashMap<String, SecretKey> sessionKeys;
    HashMap<String, ServerChallange> waitingToSolve;

    AtomicInteger auctionIdCounter;

    public AuctionImplementation() throws RemoteException {
        sessionKeys = new HashMap<>();
        waitingToSolve = new HashMap<>();
        auctionIdCounter = new AtomicInteger(1);
        try {
            channel = new JChannel("toa.xml");
            channel.setReceiver(this);
            channel.setDiscardOwnMessages(true);
            channel.setName("FrontEnd");
            channel.connect("Server");

            dispatcher = new MuxRpcDispatcher((short) 1, channel, this, this, this);
            RspList rspList = dispatcher.callRemoteMethods(null, "getIdCounter", new Object[]{}, new Class[]{}, new RequestOptions(ResponseMode.GET_FIRST, 5000));
            if (rspList.getFirst() != null) {
                int counter = (int) rspList.getFirst(); // in case the front end stops, but replicas are not, the id counter, restart the server with the latest id counter from replicas
                // so that you dont override replica's existent auctions
                if (counter > 1) {
                    auctionIdCounter.set(counter + 1);
                    System.out.println("Got the counter from a replplica " + (counter + 1));
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public String bid(SignedObject requesterId, int auctionId, double amount) throws RemoteException {

        try {
            String userId = (String)requesterId.getObject();
            PublicKey requesterPublikKey = KeyUtil.getPublicKey(userId);
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(requesterPublikKey);
            boolean isTheRequesterVerified = requesterId.verify(requesterPublikKey, signature);

            if (!isTheRequesterVerified)
                return "Bidding unsuccesful, Message is signed with a different key, you cannot bid in someone else's name";



            RspList responseList = dispatcher.callRemoteMethods(null, "bid", new Object[]{auctionId, amount,Integer.parseInt(userId)}, new Class[]{int.class,double.class,int.class}, new RequestOptions(ResponseMode.GET_ALL, 5000));

            if (responseList.size() == 0)
                return logCrash();

            HashMap<String, Integer> majority = new HashMap<>(); // hold all the responses, and how many of each.

            Set<Address> addresses = responseList.keySet();
            for (Address addr : addresses) {
                String response = (String) responseList.get(addr).getValue();

                if (majority.containsKey(response))
                    majority.put(response, majority.get(response) + 1);
                else
                    majority.put(response, 1);

            }
            String majorityResponse = "";
            int maxShowups = 0;
            for (String s : majority.keySet()) {
                if (majority.get(s) > maxShowups) {
                    maxShowups = majority.get(s);
                    majorityResponse = s;
                }
            }
            return majorityResponse;




        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Bidding unsuccesful, request was badly formatated";

    }

    @Override
    public String createListing(String userId, SealedObject sealedAuction) throws RemoteException {
        p(userId + " is trying to create an Auction");


        // TO DO discard if auction has an set ID, it has been tempered with
        if (!sessionKeys.containsKey(userId)) {
            System.out.println(userId + " is not authenticated, ignoring the request");
            return "You are not authenticated";
        }

        SecretKey sessionKey = sessionKeys.get(userId);
        Auction a = null;
        try {
            Cipher cipher = Cipher.getInstance(sessionKey.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, sessionKey);
            a = (Auction) sealedAuction.getObject(cipher);

            if (a.isIdSet()) {
                System.out.println("The auction has been tempered with, discarded");
                return "The auction has been tempered with, dont set an id";
            } else {
                a.setId(auctionIdCounter.getAndAdd(1));
            }


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            System.out.println("Encrypted with a different key, discarding");
            return "The wrong session key has been used to encrypt, retry the handshake to obtain a new key";
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            RspList rspList = dispatcher.callRemoteMethods(null, "addAuction", new Object[]{a}, new Class[]{Auction.class}, new RequestOptions(ResponseMode.GET_ALL, 5000));
            Set<Address> addresses = rspList.keySet();
            for (Address addr : addresses) {
                Boolean response = (Boolean) rspList.get(addr).getValue();

            }
            if (rspList.size() == 0)
                return logCrash();


        } catch (Exception e) {
            e.printStackTrace();
        }


        if (a != null)
            return "Succefully added an acution with id " + a.getId();
        else
            return "An error has occured";

        // return "Succefully added an acution with id : " + r.addAuction(a);
    }


    //TO DO FIX
    @Override
    public String cancelAuction(SignedObject signedId, int auctionId) throws RemoteException {

        try {
            RspList rspList = dispatcher.callRemoteMethods(null, "getAuction", new Object[]{auctionId}, new Class[]{int.class}, new RequestOptions(ResponseMode.GET_FIRST, 5000));
            Auction a = (Auction) rspList.getFirst();
            int trueOwner = a.getOwnerId();
            System.out.println("Someone is trying to close auction " + auctionId + " , the true owner is " + trueOwner);


            System.err.println("Trying to get the session key of the user with id " + trueOwner + " \n " + sessionKeys.keySet());
            PublicKey trueOwnerPublicKey = KeyUtil.getPublicKey(String.valueOf(trueOwner));

            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(trueOwnerPublicKey);
            boolean isTheRequesterTheOwner = signedId.verify(trueOwnerPublicKey, signature);

            if (isTheRequesterTheOwner) {
                System.out.println("The requester is the true owner, stoping the auction " + a.getItemName());
                RspList responseList = dispatcher.callRemoteMethods(null, "closeAuction", new Object[]{trueOwner, auctionId}, new Class[]{int.class, int.class}, new RequestOptions(ResponseMode.GET_ALL, 5000));

                if (rspList.size() == 0)
                    return logCrash();

                HashMap<String, Integer> majority = new HashMap<>(); // hold all the responses, and how many of each.

                Set<Address> addresses = responseList.keySet();
                for (Address addr : addresses) {
                    String response = (String) responseList.get(addr).getValue();

                    if (majority.containsKey(response))
                        majority.put(response, majority.get(response) + 1);
                    else
                        majority.put(response, 1);

                }
                String majorityResponse = "";
                int maxShowups = 0;
                for (String s : majority.keySet()) {
                    if (majority.get(s) > maxShowups) {
                        maxShowups = majority.get(s);
                        majorityResponse = s;
                    }
                }
                return majorityResponse;


            } else {
                System.out.println("The requester is not the owner");
                return "You are not the owner of this auction";
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";// r.closeAuction(1, auctionId);
    }

    @Override
    public String getAllAuctions() throws RemoteException {

        RspList rspList = null;
        try {
            rspList = dispatcher.callRemoteMethods(null, "getAuctionList", new Object[]{}, new Class[]{}, new RequestOptions(ResponseMode.GET_FIRST, 5000));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String a = (String) rspList.getFirst();

        return a;
    }


    /////////////////////////////////////////// HANDSHAKE


    @Override
    public SealedObject challangeServer(String id, SealedObject challange) throws RemoteException {

        PublicKey challangerPublicKey = KeyUtil.getPublicKey(id);
        if (challangerPublicKey == null) { // if the id making the request is not known
            System.err.println("ATENTION!::::: unknown user trying to challange you");
            return null;
        }

        String answerToChallange;

        try { // try to solve the challange
            Cipher decryptCipher = Cipher.getInstance(Server.serverPrivateKey.getAlgorithm());
            decryptCipher.init(Cipher.DECRYPT_MODE, Server.serverPrivateKey);
            answerToChallange = (String) challange.getObject(decryptCipher);
            System.out.println(">>>>>>>>>>>>" + answerToChallange);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ATENTION!::::: decryption failed");
            return null;
        }

        ServerChallange serverChallange = new ServerChallange(answerToChallange); // create a response for the challanger that is made of the answer to his challange, a challange for him, and a session key

        waitingToSolve.put(id, serverChallange);

        SealedObject response = null;
        try {
            Cipher encryptCipher = Cipher.getInstance(challangerPublicKey.getAlgorithm());
            encryptCipher.init(Cipher.ENCRYPT_MODE, challangerPublicKey);
            response = new SealedObject(serverChallange, encryptCipher);

        } catch (Exception e) {
            e.printStackTrace();
        }


        return response;
    }

    @Override
    public boolean answerChallange(String id, SealedObject response) throws RemoteException {


        ServerChallange challange = waitingToSolve.get(id);
        if (challange == null) {
            System.out.println("::::: Client trying to solve a challange that does not exist");
            return false;
        }

        try {
            Cipher decryptCipher = Cipher.getInstance(challange.getSessionKey().getAlgorithm());
            decryptCipher.init(Cipher.DECRYPT_MODE, challange.getSessionKey());
            String answer = (String) response.getObject(decryptCipher);
            if (answer.equals(challange.getChallangeForClient())) {
                System.out.println("Client's identity confirmed");
                sessionKeys.put(id, challange.getSessionKey());
                waitingToSolve.remove(id);
                return true;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    ////////////////////////////////////////////////////////////


    public void p(String msg) {
        System.out.println("> AuctionImplementation : " + msg);
    }

    @Override
    public void viewAccepted(View view) {
        System.out.println("________________VIEW_______________");
        System.out.println(view);
        System.out.println("___________________________________");
    }

    public String logCrash() {

        System.err.println("*********************************************************************");
        System.err.println("*********************************************************************");
        System.err.println("*********************************************************************");
        System.err.println("************ALL REPLICAS HAVE CRASHED OR ARE NOT RESPONDING**********");
        System.err.println("***********************THE SERVER HAS FAILED*************************");
        System.err.println("*********************************************************************");
        System.err.println("*********************************************************************");
        return "Server is down!";

    }

    @Override
    public void suspect(Address address) {

    }

    @Override
    public void block() {

    }

    @Override
    public void unblock() {

    }

    @Override
    public void receive(Message message) {

    }

    @Override
    public void getState(OutputStream outputStream) throws Exception {

    }

    @Override
    public void setState(InputStream inputStream) throws Exception {

    }
}
