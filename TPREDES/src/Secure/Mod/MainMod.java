package Secure.Mod;

import Secure.Compartido.Claves;
import Secure.Compartido.EncryptedMessage;
import Secure.Compartido.Utilidades;
import javax.crypto.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.*;
import java.util.Scanner;

public class MainMod {
    static Utilidades utils;

    private static void limpiarBuffer() throws IOException {
        while (System.in.available() > 0) {
            System.in.read(); // descarta byte a byte
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        Claves claves = new Claves("RSA", 2048);
        try {
            Socket socket = new Socket(InetAddress.getByName(args[0]), 30001);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            PublicKey pubServer = utils.intercambiarClavesCliente(out, in, claves.pub);
            SecretKey aesKey = utils.generarEnviarClave(out, claves.priv, pubServer);

            Scanner scanner = new Scanner(System.in);
            System.out.println("Moderador conectado. Use '$' al inicio para aprobar un mensaje.");
            while (true) {
                Object obj = in.readObject();
                String respuesta = utils.decryptAndVerify((EncryptedMessage) obj, aesKey, pubServer);
                if(respuesta.charAt(0) == '#'){
                    out.writeObject(utils.encryptAndSign(respuesta, aesKey, claves.priv));
                    out.flush();
                } else {
                    System.out.println("Nuevo mensaje: " + respuesta);
                    limpiarBuffer();
                    System.out.print("Poner $ si quiere aceptarlo, cualquier otro caracter si no: ");
                    String mensaje = scanner.nextLine();
                    if(!mensaje.isEmpty() && mensaje.charAt(0) == '$'){
                        out.writeObject(utils.encryptAndSign(mensaje + respuesta, aesKey, claves.priv) );
                        out.flush();
                    } else {
                        limpiarBuffer();
                        System.out.println("Justificar la censura");
                        String justification = scanner.nextLine();
                        out.writeObject(utils.encryptAndSign(justification, aesKey, claves.priv));
                        out.flush();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
