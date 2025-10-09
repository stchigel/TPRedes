package SinSecurity.Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainCliente {
    private String nombreCliente;
    private String clave;

    public static void main(String[] args) {

        try {
            Socket socketTCP = new Socket(InetAddress.getLocalHost(), 30000);
            String miIP = socketTCP.getLocalAddress().getHostAddress(); // esta es la IP de red real usada en la conexión


            BufferedReader in = new BufferedReader(new InputStreamReader(socketTCP.getInputStream()));
            PrintWriter out = new PrintWriter(socketTCP.getOutputStream(), true);

            ClienteEnviar ce = new ClienteEnviar(out, miIP);
            ClienteRecibir cr = new ClienteRecibir(in);

            ce.start();//lanza los hilos
            cr.start();

            try {
                ce.join();
                cr.running = false; //para cortar el hilo del bucle receptor
                cr.join();

                in.close();
                out.close(); //Cierro los sockets y streams
                socketTCP.close();

                System.out.println("SinSecurity.Cliente cerrado correctamente.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (UnknownHostException e) {
            System.out.println("Host desconocido: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error de I/O: " + e.getMessage());
        }
    }
}