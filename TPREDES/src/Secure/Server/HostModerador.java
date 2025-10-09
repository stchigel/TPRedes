package Secure.Server;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;

public class HostModerador extends Thread {
    ObjectInputStream in;
    boolean running=true;
    /*HashSet<PrintWriter> outClientes;*/
    HashMap<String, ObjectOutputStream> outClientes;

    public HostModerador(ObjectInputStream in) {
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

    public void addCliente(String ip, ObjectOutputStream cli){
        outClientes.put(ip, cli);
    }

    public void run(){
        System.out.println("Hola");
        Scanner scanner = new Scanner(System.in);
        try {
            while(running){
                String mensajeMod = (String) in.readObject();
                System.out.println("Recibido mod " + mensajeMod);
                if(!mensajeMod.isEmpty() && mensajeMod.charAt(0)=='$'){
                    String sinPrimerCaracter = mensajeMod.substring(1);
                    System.out.println("Aceptado");
                    for (ObjectOutputStream out : outClientes.values()){
                        out.writeObject(sinPrimerCaracter); // ObjectOutputStream
                        out.flush();
                        System.out.println("Mandado a cliente");
                    }
                } else if(!mensajeMod.isEmpty() && mensajeMod.charAt(0)=='#') {
                    String ip = mensajeMod.substring(1);
                    ObjectOutputStream removed = outClientes.remove(ip);
                    if (removed != null) {
                        removed.close(); // libera el recurso de salida
                        System.out.println("SinSecurity.Cliente con IP " + ip + " eliminado");
                    } else {
                        System.out.println("Error eliminado: entro pero no existe");
                    }
                } else {
                    for (ObjectOutputStream out : outClientes.values()){
                        System.out.println("Rechazado");
                        out.writeObject("Un moderador ha quitado un mensaje, razón: " + mensajeMod);
                        out.flush();
                    }
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
