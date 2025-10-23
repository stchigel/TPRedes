package Secure.Cliente;

import Secure.Compartido.EncryptedMessage;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Scanner;

public class ClienteEnviar extends Thread{
    ObjectOutputStream out;
    boolean running=true;
    String nombreCliente;
    String miIP;
    SecretKey aesKey;
    PrivateKey priv;

    public static EncryptedMessage encryptAndSign(String mensaje, SecretKey aesKey, PrivateKey privKey) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv); // 128-bit tag
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
        byte[] ciphertext = cipher.doFinal(mensaje.getBytes(StandardCharsets.UTF_8));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privKey);
        signature.update(mensaje.getBytes(StandardCharsets.UTF_8));
        byte[] sigBytes = signature.sign();

        return new EncryptedMessage(iv, ciphertext, sigBytes);
    }

    public ClienteEnviar(ObjectOutputStream out, String miIP, SecretKey aesKey, PrivateKey priv) {
        this.out = out;
        this.miIP=miIP;
        this.aesKey = aesKey;
        this.priv = priv;
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
            try {
                String mensajeCliente = scanner.nextLine();
                if(mensajeCliente.equals("logout")){
                    out.writeObject(encryptAndSign("#" + miIP, aesKey, priv));
                    out.flush();
                    running=false;
                } else {

                    out.writeObject(encryptAndSign(nombreCliente + ": " + mensajeCliente, aesKey, priv));
                    out.flush();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
