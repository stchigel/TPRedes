package Secure.Mod;

import Secure.Compartido.EncryptedKey;
import Secure.Compartido.EncryptedMessage;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Scanner;

public class MainMod {
    public static byte[] decryptGcm(SecretKey key, byte[] iv, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(ciphertext);
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


    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair modKeys = keyGen.generateKeyPair();
        PublicKey pubClient = modKeys.getPublic();
        PrivateKey privClient = modKeys.getPrivate();
        try {
            Socket socket = new Socket(InetAddress.getLocalHost(), 30001);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            PublicKey pubServer = (PublicKey) in.readObject();
            System.out.println("Clave pública del servidor recibida.");
            out.writeObject(pubClient);
            out.flush();

            SecretKey aesKey = generarClave();
            out.writeObject(encriptarClave(aesKey, privClient, pubServer));
            out.flush();

            Scanner scanner = new Scanner(System.in);
            System.out.println("Moderador conectado. Use '$' al inicio para aprobar un mensaje.");
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
                        out.writeObject(encryptAndSign(mensaje + respuesta, aesKey, privClient) );
                        out.flush();
                    } else {
                        limpiarBuffer();
                        System.out.println("Justificar la censura");
                        String justification = scanner.nextLine();
                        out.writeObject(encryptAndSign(justification, aesKey, privClient));
                        out.flush();
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error en el moderador: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    private static void limpiarBuffer() throws IOException {
        while (System.in.available() > 0) {
            System.in.read(); // descarta byte a byte
        }
    }
}
