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

    HashMap<Integer,Auction> state = new HashMap<>();

    int counter;


    String replicaId;

    public Replica() throws Exception {

        channel = new JChannel("toa.xml");
        channel.setReceiver(this);

        Random rnd = new Random();
        replicaId = "Replica_" +  rnd.nextInt(10000);

        channel.setName(replicaId);
        channel.connect("Server");

        View view = channel.getView();
        List<Address> addresses = view.getMembers();

        for(Address a : addresses)
        {

            try {
                channel.getState(a,10000);

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

    public String bid(int auctionId,double value, int bidderId){
        if(state.containsKey(auctionId)){
            return state.get(auctionId).bid(value, bidderId);
        }else{
            return "No such auction exists";
        }
    }


    public String closeAuction(int clientId, int auctionId){
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
