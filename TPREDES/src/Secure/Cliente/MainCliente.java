package Secure.Cliente;

import Secure.Compartido.Claves;
import Secure.Compartido.Utilidades;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.*;

public class MainCliente {
    static Utilidades utils;

    public static void main(String[] args) throws NoSuchAlgorithmException {
        Claves claves = new Claves("RSA", 2048);
        try {
            Socket socketTCP = new Socket(InetAddress.getByName(args[0]), 30000);
            String miIP = socketTCP.getLocalAddress().getHostAddress();

            ObjectOutputStream out = new ObjectOutputStream(socketTCP.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socketTCP.getInputStream());

            PublicKey pubServer = utils.intercambiarClavesCliente(out, in, claves.pub);
            SecretKey aesKey = utils.generarEnviarClave(out, claves.priv, pubServer);

            ClienteEnviar ce = new ClienteEnviar(out, miIP, aesKey, claves.priv);
            ClienteRecibir cr = new ClienteRecibir(in, aesKey, pubServer);
            ce.start();
            cr.start();

            try {
                ce.join();
                cr.running = false;
                cr.join();
                in.close();
                out.close();
                socketTCP.close();
                System.out.println("SinSecurity.Cliente cerrado correctamente.");
            } catch (InterruptedException e) {
                System.out.println(e.toString());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}