package Secure.Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Objects;

public class ClienteRecibir extends Thread{
    ObjectInputStream in;
    boolean running=true;

    public ClienteRecibir(ObjectInputStream in) {
        this.in = in;
    }

    public boolean isRunning() {
        return running;
    }

    public void run(){
        try {
            while(running){
                Object obj = in.readObject();
                if (!(obj instanceof String)) continue;
                String mensajeHost = (String) obj;
                System.out.println(mensajeHost);
                if(Objects.equals(mensajeHost, "Chau")){
                    running=false;
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
