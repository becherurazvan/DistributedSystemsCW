import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ServerChallange implements Serializable {
    private String challangeAnswer;
    private String challangeForClient;
    private SecretKey sessionKey;

    public ServerChallange(String challangeAnswer){
        this.challangeAnswer = challangeAnswer;
        SecureRandom rnd = new SecureRandom();
        this.challangeForClient =  new BigInteger(64,rnd).toString(32);
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("DES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyGen.init(56);
        sessionKey= keyGen.generateKey();

    }

    public String getChallangeAnswer() {
        return challangeAnswer;
    }

    public String getChallangeForClient() {
        return challangeForClient;
    }

    public SecretKey getSessionKey() {
        return sessionKey;
    }
}