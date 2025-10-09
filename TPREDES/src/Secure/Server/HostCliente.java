package Secure.Server;

import java.io.*;

public class HostCliente extends Thread {
    ObjectInputStream in;
    ObjectOutputStream out;
    boolean running=true;

    public HostCliente(ObjectInputStream in, ObjectOutputStream out) {
        this.in = in;
        this.out = out;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void run(){
        try {
            while(running){
                String mensajeCliente = (String) in.readObject();
                System.out.println("recibido cliente " + mensajeCliente);
                if(mensajeCliente==null){
                    System.out.println("Mensaje null de cliente");
                } else
                if(mensajeCliente.charAt(0)=='#'){
                    out.writeObject(mensajeCliente);
                    out.flush();
                    System.out.println("Enviado IP para cerrar");
                } else {
                    out.writeObject(mensajeCliente);
                    out.flush();
                    System.out.println("Enviado a mod");
                }

            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
