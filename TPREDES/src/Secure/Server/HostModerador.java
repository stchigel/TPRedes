package Secure.Server;

import javax.crypto.SecretKey;
import java.io.*;
import java.security.*;
import java.util.HashMap;
import Secure.Compartido.EncryptedMessage;
import Secure.Compartido.Utilidades;


public class HostModerador extends Thread {
    ObjectInputStream in;
    boolean running=true;
    SecretKey aesKey;
    private final PublicKey pubMod;
    PrivateKey privServer;
    HashMap<String, ObjectOutputStream> outClientes;
    HashMap<ObjectOutputStream, SecretKey> keysClientes;
    static Utilidades utils;

    public void enviarMensaje(String mensaje, HashMap<String, ObjectOutputStream> outClientes, HashMap<ObjectOutputStream, SecretKey> keysClientes, PrivateKey privServer) throws Exception {
        for (ObjectOutputStream out : outClientes.values()){
            out.writeObject(utils.encryptAndSign(mensaje, keysClientes.get(out), privServer)); // ObjectOutputStream
            out.flush();
        }
    }

    public void addCliente(String ip, ObjectOutputStream cli, SecretKey aesKey){
        outClientes.put(ip, cli);
        keysClientes.put(cli, aesKey);
    }
    public HostModerador(ObjectInputStream in, SecretKey aesKey, PrivateKey privServer, PublicKey pubMod) {
        this.in = in;
        outClientes = new HashMap<>();
        keysClientes = new HashMap<>();
        this.aesKey=aesKey;
        this.pubMod=pubMod;
        this.privServer = privServer;
    }

    private void desconectarCliente(String ip, HashMap<String, ObjectOutputStream> outClientes) throws Exception {
        ObjectOutputStream removed = outClientes.remove(ip);
        if (removed != null) {
            removed.writeObject(utils.encryptAndSign("Adiós", keysClientes.get(removed), privServer));
            keysClientes.remove(removed);
            removed.close();
            System.out.println("SinSecurity.Cliente con IP " + ip + " eliminado");
        } else System.out.println("Error");
    }

    public void run(){
        System.out.println("Iniciado HostModerador");
        try {
            while(running){
                Object obj = in.readObject();
                String mensajeMod=utils.decryptAndVerify((EncryptedMessage) obj, aesKey, pubMod);
                System.out.println("Recibido mod " + mensajeMod);
                if(!mensajeMod.isEmpty() && mensajeMod.charAt(0)=='$'){
                    System.out.println("Aceptado");
                    String sinPrimerCaracter = mensajeMod.substring(1);
                    enviarMensaje(sinPrimerCaracter, outClientes, keysClientes, privServer);
                    System.out.println("Mandado a cliente");
                } else if(!mensajeMod.isEmpty() && mensajeMod.charAt(0)=='#') {
                    String ip = mensajeMod.substring(1);
                    desconectarCliente(ip, outClientes);
                } else {
                    System.out.println("Rechazado");
                    enviarMensaje("Un moderador ha quitado un mensaje, razón: " + mensajeMod, outClientes, keysClientes, privServer);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
