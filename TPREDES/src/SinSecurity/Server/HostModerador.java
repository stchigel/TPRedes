package SinSecurity.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

public class HostModerador extends Thread {
    BufferedReader in;
    boolean running=true;
    /*HashSet<PrintWriter> outClientes;*/
    HashMap<String, PrintWriter> outClientes;

    public HostModerador(BufferedReader in) {
        this.in = in;
        /*outClientes = new HashSet<>();*/
        outClientes = new HashMap<>();
    }

    public boolean isRunning() {
        return running;
    }

    /*public void addCliente(PrintWriter cli){
        outClientes.add(cli);
    }*/

    public void addCliente(String ip, PrintWriter cli){
        outClientes.put(ip, cli);
    }

    public void run(){
        System.out.println("Hola");
        Scanner scanner = new Scanner(System.in);
        try {
            while(running){
                String mensajeMod = in.readLine();
                System.out.println("Recibido mod " + mensajeMod);
                if(!mensajeMod.isEmpty() && mensajeMod.charAt(0)=='$'){
                    String sinPrimerCaracter = mensajeMod.substring(1);
                    System.out.println("Aceptado");
                    for (PrintWriter out : outClientes.values()){
                        out.println(sinPrimerCaracter);
                        System.out.println("Mandado a cliente");
                    }
                } else if(!mensajeMod.isEmpty() && mensajeMod.charAt(0)=='#') {
                    String ip = mensajeMod.substring(1);
                    PrintWriter removed = outClientes.remove(ip);
                    if (removed != null) {
                        removed.close(); // libera el recurso de salida
                        System.out.println("SinSecurity.Cliente con IP " + ip + " eliminado");
                    } else {
                        System.out.println("Error eliminado: entro pero no existe");
                    }
                } else {
                    for (PrintWriter out : outClientes.values()){
                        System.out.println("Rechazado");
                        out.println("Un moderador ha quitado un mensaje, razón: " + mensajeMod);
                    }
                }
            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
