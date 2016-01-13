package KeyUtils;

import java.io.*;
import java.security.*;

/**
 * Created by becheru on 12/01/2016.
 */
public class KeyUtil {

    public final static String SERVER_KEY_NAME="SERVER";

    public static void main(String[] args){


        //createKey(SERVER_KEY_NAME);
        //createMultipleKeys(5);

        getPublicKey(String.valueOf(20));


    }

    public static void createMultipleKeys(int amount){
        for(int i=1;i<=amount;i++){
            createKey(String.valueOf(i));
        }
    }

    public static void createKey(String userName){
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(4096);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            try {
                saveKeys(keyPair,userName);
            } catch (IOException e) {
                e.printStackTrace();
            }


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static void saveKeys(KeyPair keyPair, String userId) throws IOException {

        File file = new File("./Keys/"+userId);
        System.out.println("Does key user already exist?: " + !file.mkdirs());

        FileOutputStream fos = new FileOutputStream(file.getAbsolutePath()+ "/PublicKey.key");
        ObjectOutputStream publicKeyObjectStream = new ObjectOutputStream(fos);
        publicKeyObjectStream.writeObject(keyPair.getPublic());

        fos = new FileOutputStream(file.getAbsolutePath()+"/PrivateKey.key");
        ObjectOutputStream privateKeyObjectStream = new ObjectOutputStream(fos);
        privateKeyObjectStream.writeObject(keyPair.getPrivate());


        fos.close();

    }

    public static PrivateKey getPrivateKey(String id){

        System.out.println("KEyUtil: trying to get private key for " + id);
        PrivateKey privateKey = null;
        try {
            FileInputStream fileInputStream = new FileInputStream("./Keys/"+id + "/PrivateKey.key");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            privateKey = (PrivateKey)objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return privateKey;

    }

    public static PublicKey getPublicKey(String id){

        PublicKey publicKey = null;
        try {
            FileInputStream fileInputStream = new FileInputStream("./Keys/"+id + "/PublicKey.key");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            publicKey = (PublicKey)objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
           return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return publicKey;

    }

}
