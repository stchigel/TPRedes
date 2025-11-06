package Secure.Server;

import Secure.Compartido.Claves;
import Secure.Compartido.EncryptedKey;
import Secure.Compartido.Utilidades;
import javax.crypto.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;

public class MainServer {
    static Utilidades utils;

    private static void aceptarCliente(Socket clientSocket, ObjectOutputStream out, SecretKey aesKey, Claves claves, HostModerador hm) throws Exception {
        ObjectOutputStream outC = new ObjectOutputStream(clientSocket.getOutputStream());
        ObjectInputStream inC = new ObjectInputStream(clientSocket.getInputStream());
        PublicKey pubCliente = utils.intercambiarClavesServidor(outC, inC, claves.pub);
        SecretKey aesKeyCliente = utils.desencriptarClave((EncryptedKey) inC.readObject(), claves.priv, pubCliente);
        hm.addCliente(clientSocket.getInetAddress().getHostAddress(), outC, aesKeyCliente);
        HostCliente hc = new HostCliente(inC, out, aesKey, aesKeyCliente, claves.priv, pubCliente);
        hc.start();
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        Claves claves = new Claves("RSA", 2048);
        try (ServerSocket serverSocketCli = new ServerSocket(30000);
             ServerSocket serverSocketMod = new ServerSocket(30001)) {
            Socket clientSocketMod = serverSocketMod.accept();
            ObjectOutputStream out = new ObjectOutputStream(clientSocketMod.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocketMod.getInputStream());

            PublicKey pubModerador = utils.intercambiarClavesServidor(out, in, claves.pub);
            SecretKey aesKey = utils.desencriptarClave((EncryptedKey) in.readObject(), claves.priv, pubModerador);

            HostModerador hm = new HostModerador(in, aesKey, claves.priv, pubModerador);
            hm.start();

            while (true) {
                aceptarCliente(serverSocketCli.accept(), out, aesKey, claves, hm);
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
