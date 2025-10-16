package Secure.Mod;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.*;
import java.util.Scanner;

public class MainMod {
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
    public static class EncryptedMessage implements Serializable {
        public byte[] iv;
        public byte[] ciphertext;
        public EncryptedMessage(byte[] iv, byte[] ciphertext) {
            this.iv = iv;
            this.ciphertext = ciphertext;
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair modKeys = keyGen.generateKeyPair();
        PublicKey pubClient = modKeys.getPublic();
        PrivateKey privClient = modKeys.getPrivate();
        try {
            // Conexión al servidor en el puerto 40001
            Socket socket = new Socket(InetAddress.getLocalHost(), 30001);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            PublicKey pubServer = (PublicKey) in.readObject();
            System.out.println("Clave pública del servidor recibida.");

            out.writeObject(pubClient);
            out.flush();

            KeyGenerator aesGen = KeyGenerator.getInstance("AES");
            aesGen.init(128);
            SecretKey aesKey = aesGen.generateKey();
            byte[] aesBytes = aesKey.getEncoded();
            Cipher rsaEnc = Cipher.getInstance("RSA");
            rsaEnc.init(Cipher.ENCRYPT_MODE, pubServer);
            byte[] encryptedAesKey = rsaEnc.doFinal(aesBytes);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(aesBytes);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privClient);
            sig.update(hash);
            byte[] signedHash = sig.sign();
            System.out.println("Clave AES cifrada y firmada lista para enviar.");

            out.writeObject(encryptedAesKey);
            out.writeObject(signedHash);
            out.flush();


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
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }
    private static void limpiarBuffer() throws IOException {
        while (System.in.available() > 0) {
            System.in.read(); // descarta byte a byte
        }
    }
}
