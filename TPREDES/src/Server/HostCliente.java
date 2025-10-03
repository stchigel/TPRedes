package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class HostCliente extends Thread {
    BufferedReader in;
    PrintWriter out;
    boolean running=true;

    public HostCliente(BufferedReader in, PrintWriter out) {
        this.in = in;
        this.out = out;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void run(){
        try {
            while(running){
                String mensajeCliente = in.readLine();
                System.out.println("recibido cliente " + mensajeCliente);
                if(mensajeCliente==null){
                    System.out.println("Mensaje null de cliente");
                } else
                if(mensajeCliente.charAt(0)=='#'){
                    out.println(mensajeCliente);
                    System.out.println("Enviado IP para cerrar");
                } else {
                    out.println(mensajeCliente);
                    System.out.println("Enviado a mod");
                }

            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
