package SinSecurity.Cliente;

import java.io.PrintWriter;
import java.util.Scanner;

public class ClienteEnviar extends Thread{
    PrintWriter out;
    boolean running=true;
    String nombreCliente;
    String miIP;

    public ClienteEnviar(PrintWriter out, String miIP) {
        this.out = out;
        this.miIP=miIP;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void run(){
        System.out.println("Elegi tu nombre de usuario");
        boolean elegidoNombre=false;
        Scanner scanner = new Scanner(System.in);

        while(!elegidoNombre){
            nombreCliente = scanner.nextLine();
            if(nombreCliente.isEmpty() || nombreCliente.charAt(0)=='#' || nombreCliente.charAt(0)=='$'){
                System.out.println("Nombre de usuario no válido");
            } else {
                System.out.println("Usuario seteado, ya podes chatear. Cuando quiera salir, escriba logout");
                elegidoNombre=true;
            }
        }



        while(running) {
            String mensajeCliente = scanner.nextLine();
            if(mensajeCliente.equals("logout")){
                out.println("#" + miIP);
                running=false;
            } else {
                out.println(nombreCliente + ": " + mensajeCliente);
            }

        }
    }
}
