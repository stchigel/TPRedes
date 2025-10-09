package Secure.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class MainServer {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair serverKeys = keyGen.generateKeyPair();

        try (ServerSocket serverSocketCli = new ServerSocket(30000);
             ServerSocket serverSocketMod = new ServerSocket(30001)) {

            Socket clientSocketMod = serverSocketMod.accept();

            ObjectOutputStream out = new ObjectOutputStream(clientSocketMod.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocketMod.getInputStream());

            HostModerador hm = new HostModerador(in);
            hm.start();

            while (true) {
                Socket clientSocket = serverSocketCli.accept();

                ObjectOutputStream outC = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream inC = new ObjectInputStream(clientSocket.getInputStream());

                String ip = clientSocket.getInetAddress().getHostAddress();
                hm.addCliente(ip, outC);

                HostCliente hc = new HostCliente(inC, out);
                hc.start();
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public String firmar(String mensaje){
        return mensaje + " ,firmado con clave privada";
    }
}
