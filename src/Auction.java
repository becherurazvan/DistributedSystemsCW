import java.io.Serializable;

/**
 * Created by becheru on 12/01/2016.
 */
public class Auction implements Serializable {
    //Auction class holds all the data about the auction and it's owner

    private String itemName;
    private Integer id;
    private String ownerId; // **********************************************************************
    private String highestBidderId; // **********************************************************************
    private double startingPrice;
    private double minimumPrice;
    private double currentBid;
    private String history = "";
    private String  winnerId;// **********************************************************************
    private boolean won;
    private boolean stopped=false;


    public Auction(String itemName, double startingPrice, double minimumPrice, String ownerId) {  // **********************************************************************
        this.ownerId = ownerId;
        this.startingPrice = startingPrice;
        this.minimumPrice = minimumPrice;
        this.currentBid = startingPrice;
        this.itemName = itemName;
    }

    // you can only bid on something if the current bid is smaller than your bid and if its not stopped
    public String bid(double value, String bidderId){ // **********************************************************************
        if(stopped){
            return "Auction is closed";
        }
        if(value<=currentBid){
            return "Your bid must be bigger than the current highest big";
        }
        currentBid = value;
        highestBidderId = bidderId;
        history = bidderId + " bidded " + value + " on " + itemName + "\n" + history;
        return "Succesfully bidded " + value + " on " + itemName + " you are currently the highest bidder";
    }

    // The task to set the id of a bid is handled by the server, not the client
    public void setId(int id) {
        this.id = id;
    }

    public boolean isIdSet(){
        return id != null;
    }


    // You can only stop an auction if you are the true owner
    public String stop(String ownerId){ // Returns whether the bid was won or not        // **********************************************************************

        System.err.println("----------------------" + ownerId + " @ " + this.ownerId + "     =    " + ownerId.equals(this.ownerId));
        if(!ownerId.equals(this.ownerId))  // **********************************************************************
            return "You are not allowed to close this auction!";
        if(stopped)
            return "Auction already stopped";
        stopped = true;
        if(currentBid>minimumPrice){
            winnerId = highestBidderId;
            won = true;
            return "Auction won by "+ winnerId + " with the highest bid of "+ currentBid;
        }else{
            winnerId = "-1"; // **********************************************************************
            won = false;

            return "Auction closed, no winner :(";
        }
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getMinimumPrice() {
        return minimumPrice;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public int getId() {
        return id;
    }

    public String  getWinnerId(){return winnerId;} // **********************************************************************

    public String getItemName() {
        return itemName;
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean isWon() {
        return won;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    } // **********************************************************************
}
