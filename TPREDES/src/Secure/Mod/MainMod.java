package Secure.Mod;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class MainMod {
    public static void main(String[] args) {
        try {
            // Conexión al servidor en el puerto 40001
            Socket socket = new Socket(InetAddress.getLocalHost(), 30001);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Scanner para leer desde la consola lo que escriba el moderador
            Scanner scanner = new Scanner(System.in);

            System.out.println("Moderador conectado. Use '$' al inicio para aprobar un mensaje.");

            // Bucle principal
            while (true) {
                Object obj = in.readObject(); // bloquea hasta recibir un objeto
                if (!(obj instanceof String)) continue;
                String respuesta = (String) obj;
                if(respuesta.charAt(0) == '#'){
                    out.writeObject(respuesta);
                    out.flush();
                    System.out.println("Enviado IP para cerrar");
                } else {
                    System.out.println("Nuevo mensaje: " + respuesta);
                    limpiarBuffer();
                    System.out.print("Poner $ si quiere aceptarlo, cualquier otro caracter si no: ");
                    String mensaje = scanner.nextLine();
                    if(!mensaje.isEmpty() && mensaje.charAt(0) == '$'){
                        out.writeObject(mensaje + respuesta);
                        out.flush();
                    } else {
                        limpiarBuffer();
                        System.out.println("Justificar la censura");
                        String justification = scanner.nextLine();
                        out.writeObject(justification);
                        out.flush();
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Error en el moderador: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    private static void limpiarBuffer() throws IOException {
        while (System.in.available() > 0) {
            System.in.read(); // descarta byte a byte
        }
    }
}
