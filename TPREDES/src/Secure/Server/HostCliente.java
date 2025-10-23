package Secure.Server;

import Secure.Compartido.EncryptedMessage;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;

public class HostCliente extends Thread {
    ObjectInputStream in;
    ObjectOutputStream out;
    boolean running=true;
    SecretKey aesKeyMod;
    SecretKey aesKeyCliente;
    PrivateKey privServer;
    PublicKey pubCliente;

    public HostCliente(ObjectInputStream in, ObjectOutputStream out, SecretKey aesKeyMod, SecretKey aesKeyCliente, PrivateKey privServer, PublicKey pubCliente) {
        this.in = in;
        this.out = out;
        this.aesKeyMod = aesKeyMod;
        this.aesKeyCliente = aesKeyCliente;
        this.privServer = privServer;
        this.pubCliente = pubCliente;
    }

    public void setRunning(boolean running) {
        this.running = running;
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
    public void run(){
        try {
            while(running){
                String mensajeCliente = decryptAndVerify((EncryptedMessage) in.readObject(), aesKeyCliente, pubCliente);
                System.out.println("recibido cliente " + mensajeCliente);
                if(mensajeCliente==null){
                    System.out.println("Mensaje null de cliente");
                } else
                if(mensajeCliente.charAt(0)=='#'){
                    out.writeObject(encryptAndSign(mensajeCliente, aesKeyMod, privServer));
                    out.flush();
                    System.out.println("Enviado IP para cerrar");
                } else {
                    out.writeObject(encryptAndSign(mensajeCliente, aesKeyMod, privServer));
                    out.flush();
                    System.out.println("Enviado a mod");
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
