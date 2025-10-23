package Secure.Cliente;

import Secure.Compartido.Claves;
import Secure.Compartido.EncryptedKey;
import Secure.Compartido.EncryptedMessage;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class MainCliente {
    public static SecretKey generarClave() throws NoSuchAlgorithmException {
        KeyGenerator aesGen = KeyGenerator.getInstance("AES");
        aesGen.init(128);
        return aesGen.generateKey();
    }
    public static EncryptedKey encriptarClave(SecretKey aesKey, PrivateKey privEmisor, PublicKey pubReceptor) throws Exception {
        byte[] aesBytes = aesKey.getEncoded();
        Cipher rsaEnc = Cipher.getInstance("RSA");
        rsaEnc.init(Cipher.ENCRYPT_MODE, pubReceptor);
        byte[] encryptedAesKey = rsaEnc.doFinal(aesBytes);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(aesBytes);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privEmisor);
        sig.update(hash);
        byte[] signedHash = sig.sign();
        return new EncryptedKey(encryptedAesKey, signedHash);
    }
    public static PublicKey intercambiarClaves(ObjectOutputStream out, ObjectInputStream in, PublicKey pub) throws IOException, ClassNotFoundException {
        PublicKey pubServer = (PublicKey) in.readObject();
        out.writeObject(pub);
        out.flush();
        return pubServer;
    }
    public static SecretKey generarEnviarClave(ObjectOutputStream out, PrivateKey priv, PublicKey pubReceptor) throws Exception {
        SecretKey aesKey = generarClave();
        out.writeObject(encriptarClave(aesKey, priv, pubReceptor));
        out.flush();
        return aesKey;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        Claves claves = new Claves("RSA", 2048);
        try {
            Socket socketTCP = new Socket(InetAddress.getByName(args[0]), 30000);
            String miIP = socketTCP.getLocalAddress().getHostAddress();

            ObjectOutputStream out = new ObjectOutputStream(socketTCP.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socketTCP.getInputStream());

            PublicKey pubServer = intercambiarClaves(out, in, claves.pub);
            SecretKey aesKey = generarEnviarClave(out, claves.priv, pubServer);

            ClienteEnviar ce = new ClienteEnviar(out, miIP, aesKey, claves.priv);
            ClienteRecibir cr = new ClienteRecibir(in, aesKey, pubServer);
            ce.start();
            cr.start();

            try {
                ce.join();
                cr.running = false;
                cr.join();
                in.close();
                out.close();
                socketTCP.close();
                System.out.println("SinSecurity.Cliente cerrado correctamente.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}