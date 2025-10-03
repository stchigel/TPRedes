package Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;

public class ClienteRecibir extends Thread{
    BufferedReader in;
    boolean running=true;

    public ClienteRecibir(BufferedReader in) {
        this.in = in;
    }

    public boolean isRunning() {
        return running;
    }

    public void run(){
        try {
            while(running){
                String mensajeHost = in.readLine();
                System.out.println(mensajeHost);
                if(Objects.equals(mensajeHost, "Chau")){
                    running=false;
                }
            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
