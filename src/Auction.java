import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by becheru on 12/01/2016.
 */
public class Auction implements Serializable {
    String itemName;
    Integer id;
    int ownerId;
    int highestBidderId;
    double startingPrice;
    double minimumPrice;
    double currentBid;
    String history = "";
    int winnerId;
    boolean won;
    boolean stopped=false;


    public Auction(String itemName, double startingPrice, double minimumPrice, int ownerId) {
        this.ownerId = ownerId;
        this.startingPrice = startingPrice;
        this.minimumPrice = minimumPrice;
        this.currentBid = startingPrice;
        this.itemName = itemName;
    }

    public String bid(double value, int bidderId){
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

    public void setId(int id) {
        this.id = id;
    }

    public boolean isIdSet(){
        return id != null;
    }

    public String stop(int ownerId){ // Returns whether the bid was won or not

        if(ownerId!=this.ownerId)
            return "You are not allowed to close this auction!";
        if(stopped)
            return "Auction already stopped";
        stopped = true;
        if(currentBid>minimumPrice){
            winnerId = highestBidderId;
            won = true;
            return "Auction won by "+ winnerId + " with the highest bid of "+ currentBid;
        }else{
            winnerId = -1;
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

    public int getOwnerId() {
        return ownerId;
    }

    public int getId() {
        return id;
    }

    public int getWinnerId(){return winnerId;}

    public String getItemName() {
        return itemName;
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean isWon() {
        return won;
    }

    public int getHighestBidderId() {
        return highestBidderId;
    }
}
