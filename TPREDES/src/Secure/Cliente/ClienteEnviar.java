package Secure.Cliente;

import Secure.Compartido.Utilidades;
import javax.crypto.SecretKey;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.util.Scanner;

public class ClienteEnviar extends Thread{
    ObjectOutputStream out;
    boolean running=true;
    String nombreCliente;
    String miIP;
    SecretKey aesKey;
    PrivateKey priv;

    static Utilidades utils;

    public ClienteEnviar(ObjectOutputStream out, String miIP, SecretKey aesKey, PrivateKey priv) {
        this.out = out;
        this.miIP=miIP;
        this.aesKey = aesKey;
        this.priv = priv;
    }

    private String elegirNombre(Scanner scanner){
        System.out.println("Elegi tu nombre de usuario");
        while(true){
            String nombre = scanner.nextLine();
            if(nombre.isEmpty() || nombre.charAt(0)=='#' || nombre.charAt(0)=='$'){
                System.out.println("Nombre de usuario no válido");
            } else {
                System.out.println("Usuario seteado, ya podes chatear. Cuando quiera salir, escriba logout");
                return nombre;
            }
        }
    }

    public void run(){
        Scanner scanner = new Scanner(System.in);
        nombreCliente = elegirNombre(scanner);

        while(running) {
            try {
                String mensajeCliente = scanner.nextLine();
                if(mensajeCliente.equals("logout")){
                    out.writeObject(utils.encryptAndSign("#" + miIP, aesKey, priv));
                    out.flush();
                    running=false;
                } else {
                    out.writeObject(utils.encryptAndSign(nombreCliente + ": " + mensajeCliente, aesKey, priv));
                    out.flush();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
