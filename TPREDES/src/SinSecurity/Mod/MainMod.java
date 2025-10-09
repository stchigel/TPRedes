package SinSecurity.Mod;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class MainMod {
    public static void main(String[] args) {
        try {
            // Conexión al servidor en el puerto 40001
            Socket socket = new Socket(InetAddress.getLocalHost(), 30001);

            // Flujo de entrada: mensajes desde el servidor (si se envían)
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Flujo de salida: mensajes hacia el servidor
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Scanner para leer desde la consola lo que escriba el moderador
            Scanner scanner = new Scanner(System.in);

            System.out.println("Moderador conectado. Use '$' al inicio para aprobar un mensaje.");

            // Bucle principal
            while (true) {
                if (in.ready()) {
                    String respuesta = in.readLine();
                    if(respuesta.charAt(0)=='#'){
                        out.println(respuesta);
                        System.out.println("Enviado IP para cerrar");
                    } else {
                        System.out.println("Nuevo mensaje: " + respuesta);
                        limpiarBuffer();
                        System.out.print("Poner $ si quiere aceptarlo, cualquier otro caracter si no: ");
                        String mensaje = scanner.nextLine();
                        if(!mensaje.isEmpty() && mensaje.charAt(0)=='$'){
                            out.println(mensaje + respuesta);
                        } else {
                            limpiarBuffer();
                            System.out.println("Justificar la censura");
                            String justification = scanner.nextLine();
                            out.println(justification);
                        }
                    }




                }

            }

        } catch (IOException e) {
            System.out.println("Error en el moderador: " + e.getMessage());
        }
    }
    private static void limpiarBuffer() throws IOException {
        while (System.in.available() > 0) {
            System.in.read(); // descarta byte a byte
        }
    }
}
