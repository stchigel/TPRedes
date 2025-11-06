package Secure.Server;

import Secure.Compartido.EncryptedMessage;
import Secure.Compartido.Utilidades;
import javax.crypto.SecretKey;
import java.io.*;
import java.security.PrivateKey;
import java.security.PublicKey;

public class HostCliente extends Thread {
    ObjectInputStream in;
    ObjectOutputStream out;
    boolean running=true;
    SecretKey aesKeyMod;
    SecretKey aesKeyCliente;
    PrivateKey privServer;
    PublicKey pubCliente;
    static Utilidades utils;

    public HostCliente(ObjectInputStream in, ObjectOutputStream out, SecretKey aesKeyMod, SecretKey aesKeyCliente, PrivateKey privServer, PublicKey pubCliente) {
        this.in = in;
        this.out = out;
        this.aesKeyMod = aesKeyMod;
        this.aesKeyCliente = aesKeyCliente;
        this.privServer = privServer;
        this.pubCliente = pubCliente;
    }

    public void run(){
        try {
            while(running){
                String mensajeCliente = utils.decryptAndVerify((EncryptedMessage) in.readObject(), aesKeyCliente, pubCliente);
                System.out.println("Recibido cliente " + mensajeCliente);
                if(mensajeCliente.charAt(0)=='#'){
                    out.writeObject(utils.encryptAndSign(mensajeCliente, aesKeyMod, privServer));
                    out.flush();
                    System.out.println("Enviado IP para cerrar");
                    running=false;
                } else {
                    out.writeObject(utils.encryptAndSign(mensajeCliente, aesKeyMod, privServer));
                    out.flush();
                    System.out.println("Enviado a mod");
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
