package Secure.Cliente;

import Secure.Compartido.EncryptedMessage;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Objects;

public class ClienteRecibir extends Thread{
    ObjectInputStream in;
    boolean running=true;
    SecretKey aesKey;
    PublicKey pubServer;

    public static String decryptAndVerify(EncryptedMessage msg, SecretKey aesKey, PublicKey pubKey) throws Exception {
        GCMParameterSpec spec = new GCMParameterSpec(128, msg.iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);
        byte[] decryptedBytes = cipher.doFinal(msg.ciphertext);
        String decryptedMessage = new String(decryptedBytes, StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(pubKey);
        signature.update(decryptedMessage.getBytes(StandardCharsets.UTF_8));
        boolean verified = signature.verify(msg.signature);
        if (!verified) {
            throw new SecurityException("Firma no válida");
        }
        return decryptedMessage;
    }
    public ClienteRecibir(ObjectInputStream in, SecretKey aesKey, PublicKey pubServer) {
        this.in = in;
        this.aesKey = aesKey;
        this.pubServer = pubServer;
    }

    public boolean isRunning() {
        return running;
    }

    public void run(){
        try {
            while(running){
                Object obj = in.readObject();
                String mensajeHost = decryptAndVerify((EncryptedMessage) obj, aesKey, pubServer);
                System.out.println(mensajeHost);
            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
