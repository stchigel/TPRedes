package SinSecurity.Server;

import java.io.*;
import java.net.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocketMod.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocketMod.getOutputStream(), true);

            HostModerador hm = new HostModerador(in);
            hm.start();

            while (true) {
                Socket clientSocket = serverSocketCli.accept();
                BufferedReader inC = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter outC = new PrintWriter(clientSocket.getOutputStream(), true);



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
