import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.blocks.mux.MuxRpcDispatcher;
import org.jgroups.util.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by becheru on 12/01/2016.
 */
public class Replica extends ReceiverAdapter {

    private JChannel channel;

    private RpcDispatcher dispatcher;

    // The replicas hold a view of the auctioning data, each auction has an id of itself and that is the way you get one from the list
    HashMap<Integer,Auction> state = new HashMap<>();
    // The counter is used to make sure that every new auction will have a unique ID
    int counter;

    // Used only for printing
    String replicaId;

    public Replica() throws Exception {


        // toa.xml includes Total Ordering Anycast in the protocol stack, That is
                // your message is sent to with anycast to a list of users in Total order
                // this may not be the best solution when you will use all addresses as destination
                // for most of your request
        //squencer.xml includes Total Ordering Mulitcast, That is an multicast message is sent to the coordinator
                // that will then multicast the message to all other members


        channel = new JChannel("toa.xml");
        channel.setReceiver(this);

        // set the name of the replica
        Random rnd = new Random();
        replicaId = "Replica_" +  rnd.nextInt(10000);

        channel.setName(replicaId);
        channel.connect("Server");

        View view = channel.getView();
        List<Address> addresses = view.getMembers();


        // try to get the state from the coordinator
        // it might happend that the coordinator is the front end, so getting the state will fail
        // therefore if that happend, try to get the state from the next member
        for(Address a : addresses)
        {

            try {
                channel.getState(a,5000);

            } catch (Exception e) {
                System.err.println("---Cannot get state from: " + a.toString());
                continue;
            }
            if(a == channel.getAddress()){
                System.out.println(">>>>>>>>>>> First Replica <<<<<<<<<<<");
            }else{
                System.out.println("Got state from "  +a.toString());
                System.out.println(getAuctionList());
            }

            break;
        }



        //Use Mux (multiplexed ) dispatcher instead of the simple one
        //because the simple one will block and you wont be able to get
        //any more messages (even with flushing)
        dispatcher = new MuxRpcDispatcher((short)1,channel,this,this,this);


        p("Started succesfuly!");

    }





    public boolean addAuction(Auction a){
        counter = a.getId();
        p("Trying to add auction");
        state.put(a.getId(), a);
        p("Auction "+ a.getItemName() + " succesfully added with id " + a.getId() + " By " + a.getOwnerId());
        return true;
    }

    public String bid(int auctionId,double value, String bidderId){ // **********************************************************************
        if(state.containsKey(auctionId)){
            return state.get(auctionId).bid(value, bidderId); // **********************************************************************
        }else{
            return "No such auction exists";
        }
    }


    public String closeAuction(String clientId, int auctionId){ // **********************************************************************
        if(state.containsKey(auctionId)) {
            return state.get(auctionId).stop(clientId);
        }else{
            return "No such auction exists";
        }
    }

    public int getIdCounter(){
        return counter;
    }

    public String getAuctionList(){
        // auction id, auction name, closed, highest bid, highest bidder

        String response =  String.format("%-15s %-20s %-10s %-10s %-15s %-20s %n",
                "Auction ID","Item Name","Closed","Won","Highest Bid","Highest Bidder");
        for(int i : state.keySet()){
            Auction a = state.get(i);
            String line = String.format("%-15s %-20s %-10s %-10s %-15s %-20s %n",
                    a.getId(),a.getItemName(),a.isStopped(),a.isWon(),a.getCurrentBid(),a.getHighestBidderId());
            response += line;
        }
        return response;
    }



    public Auction getAuction(int auctionId){
        if(state.containsKey(auctionId))
            return state.get(auctionId);
        else
            return null;
    }






    public static void main(String[] args) throws Exception {
        new Replica();
    }
    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (state) {
            Util.objectToStream(state, new DataOutputStream(output));
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        HashMap<Integer,Auction> hm = (HashMap) Util.objectFromStream(new DataInputStream(input));
        synchronized (state) {
            state.clear();
            state.putAll(hm);
        }
        p("Done getting the state!");
    }

    @Override
    public void viewAccepted(View view) {
        System.out.println(view.toString());
    }
    public void p(String msg){
        System.out.println(">" + replicaId + " : "  +msg);
    }



}
