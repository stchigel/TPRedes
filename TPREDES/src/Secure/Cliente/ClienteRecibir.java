package Secure.Cliente;

import Secure.Compartido.EncryptedMessage;
import Secure.Compartido.Utilidades;
import javax.crypto.SecretKey;
import java.io.ObjectInputStream;
import java.security.PublicKey;

public class ClienteRecibir extends Thread{
    ObjectInputStream in;
    boolean running=true;
    SecretKey aesKey;
    PublicKey pubServer;

    static Utilidades utils;

    public ClienteRecibir(ObjectInputStream in, SecretKey aesKey, PublicKey pubServer) {
        this.in = in;
        this.aesKey = aesKey;
        this.pubServer = pubServer;
    }

    public void run(){
        try {
            while(running){
                Object obj = in.readObject();
                String mensajeHost = utils.decryptAndVerify((EncryptedMessage) obj, aesKey, pubServer);
                System.out.println(mensajeHost);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
