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
    public static String decryptAndVerify(EncryptedMessage msg, SecretKey aesKey, PublicKey pubKey) throws Exception {
        GCMParameterSpec spec = new GCMParameterSpec(128, msg.iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);
        byte[] decryptedBytes = cipher.doFinal(msg.ciphertext);
        String decryptedMessage = new String(decryptedBytes, StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(pubKey);
        signature.update(decryptedMessage.getBytes(StandardCharsets.UTF_8));
        boolean verified = signature.verify(msg.signature);
        if (!verified) {
            throw new SecurityException("Firma no válida");
        }
        return decryptedMessage;
    }
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

    ObjectInputStream in;
    boolean running=true;
    SecretKey aesKey;
    private final PublicKey pubMod;
    PrivateKey privServer;
    HashMap<String, ObjectOutputStream> outClientes;
    HashMap<ObjectOutputStream, SecretKey> keysClientes;

    public HostModerador(ObjectInputStream in, SecretKey aesKey, PrivateKey privServer, PublicKey pubMod) {
        this.in = in;
        outClientes = new HashMap<>();
        keysClientes = new HashMap<>();
        this.aesKey=aesKey;
        this.pubMod=pubMod;
        this.privServer = privServer;
    }

    public boolean isRunning() {
        return running;
    }

    public void addCliente(String ip, ObjectOutputStream cli, SecretKey aesKey){
        outClientes.put(ip, cli);
        keysClientes.put(cli, aesKey);
    }

    public void run(){
        System.out.println("Hola");
        Scanner scanner = new Scanner(System.in);
        SecureRandom sr = new SecureRandom();
        try {
            while(running){
                Object obj = in.readObject();
                String mensajeMod;
                mensajeMod=decryptAndVerify((EncryptedMessage) obj, aesKey, pubMod);

                System.out.println("Recibido mod " + mensajeMod);
                if(!mensajeMod.isEmpty() && mensajeMod.charAt(0)=='$'){
                    String sinPrimerCaracter = mensajeMod.substring(1);
                    System.out.println("Aceptado");
                    for (ObjectOutputStream out : outClientes.values()){
                        out.writeObject(encryptAndSign(sinPrimerCaracter, keysClientes.get(out), privServer)); // ObjectOutputStream
                        out.flush();
                        System.out.println("Mandado a cliente");
                    }
                } else if(!mensajeMod.isEmpty() && mensajeMod.charAt(0)=='#') {
                    String ip = mensajeMod.substring(1);
                    ObjectOutputStream removed = outClientes.remove(ip);
                    if (removed != null) {
                        removed.writeObject(encryptAndSign("Adiós", keysClientes.get(removed), privServer));
                        keysClientes.remove(removed);
                        removed.close();
                        System.out.println("SinSecurity.Cliente con IP " + ip + " eliminado");
                    } else {
                        System.out.println("Error eliminado: entro pero no existe");
                    }
                } else {
                    for (ObjectOutputStream out : outClientes.values()){
                        System.out.println("Rechazado");
                        out.writeObject(encryptAndSign("Un moderador ha quitado un mensaje, razón: " + mensajeMod, keysClientes.get(out), privServer));
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
