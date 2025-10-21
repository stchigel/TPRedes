package Secure.Server;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HashMap;
import java.util.Scanner;

import Secure.Compartido.EncryptedMessage;

public class HostModerador extends Thread {
    public static byte[] encryptGcm(SecretKey key, byte[] iv, byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv); // 128-bit tag
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        return cipher.doFinal(plaintext);
    }
    public static byte[] decryptGcm(SecretKey key, byte[] iv, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(ciphertext);
    }

    public String desencriptarMensaje(EncryptedMessage encryptedMessage, PublicKey pub) throws Exception {
        String mensaje;
        byte[] decrypted = decryptGcm(aesKey, encryptedMessage.iv, encryptedMessage.ciphertext);
        mensaje = new String(decrypted, StandardCharsets.UTF_8);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pub);
        sig.update(decrypted);
        boolean verified = sig.verify(encryptedMessage.signature);
        if (!verified) {
            System.out.println("Firma inválida");
            mensaje = null;
        }
        return mensaje;
    }

    ObjectInputStream in;
    boolean running=true;
    SecretKey aesKey;
    private final PublicKey pubMod;
    /*HashSet<PrintWriter> outClientes;*/
    HashMap<String, ObjectOutputStream> outClientes;

    public HostModerador(ObjectInputStream in, SecretKey aesKey, PublicKey pubMod) {
        this.in = in;
        /*outClientes = new HashSet<>();*/
        outClientes = new HashMap<>();
        this.aesKey=aesKey;
        this.pubMod=pubMod;
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
        SecureRandom sr = new SecureRandom();
        try {
            while(running){
                Object obj = in.readObject();
                String mensajeMod;
                mensajeMod=desencriptarMensaje((EncryptedMessage) obj, pubMod);

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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
