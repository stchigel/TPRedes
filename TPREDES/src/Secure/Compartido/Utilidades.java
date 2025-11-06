package Secure.Compartido;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class Utilidades {
    /*Mensajes*/
    public static String decrypt(byte[] ciphertext, byte[] iv, SecretKey aesKey) throws Exception {
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);
        byte[] decryptedBytes = cipher.doFinal(ciphertext);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
    public static boolean verify(String message, byte[] signatureBytes, PublicKey pubKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(pubKey);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return signature.verify(signatureBytes);
    }
    public static String decryptAndVerify(EncryptedMessage msg, SecretKey aesKey, PublicKey pubKey) throws Exception {
        String decryptedMessage = decrypt(msg.ciphertext, msg.iv, aesKey);
        boolean verified = verify(decryptedMessage, msg.signature, pubKey);
        if (!verified) {
            throw new SecurityException("Firma no válida/Invalid Signature");
        }
        return decryptedMessage;
    }
    public static byte[] encrypt(String mensaje, SecretKey aesKey, byte[] iv) throws Exception {
        GCMParameterSpec spec = new GCMParameterSpec(128, iv); // 128-bit tag
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
        return cipher.doFinal(mensaje.getBytes(StandardCharsets.UTF_8));
    }
    public static byte[] sign(String mensaje, PrivateKey privKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privKey);
        signature.update(mensaje.getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }
    public static EncryptedMessage encryptAndSign(String mensaje, SecretKey aesKey, PrivateKey privKey) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        byte[] ciphertext = encrypt(mensaje, aesKey, iv);
        byte[] sigBytes = sign(mensaje, privKey);
        return new EncryptedMessage(iv, ciphertext, sigBytes);
    }

    /*Claves simétricas*/
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
    public static SecretKey generarEnviarClave(ObjectOutputStream out, PrivateKey priv, PublicKey pubReceptor) throws Exception {
        SecretKey aesKey = generarClave();
        out.writeObject(encriptarClave(aesKey, priv, pubReceptor));
        out.flush();
        return aesKey;
    }
    public static SecretKey desencriptarClave(EncryptedKey encryptedKey, PrivateKey privReceptor, PublicKey pubEmisor) throws Exception {
        byte[] encryptedAesKey = encryptedKey.encryptedAesKey;
        byte[] signedHash = encryptedKey.signedHash;
        Cipher rsaDec = Cipher.getInstance("RSA");
        rsaDec.init(Cipher.DECRYPT_MODE, privReceptor);
        byte[] aesKeyBytes = rsaDec.doFinal(encryptedAesKey);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] localHash = digest.digest(aesKeyBytes);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pubEmisor);
        sig.update(localHash);
        boolean verified = sig.verify(signedHash);
        if (verified) {
            return aesKey;
        } else {
            System.exit(0);
            return null;
        }
    }

    /*Claves asimétricas*/
    public static PublicKey intercambiarClavesCliente(ObjectOutputStream out, ObjectInputStream in, PublicKey pub) throws IOException, ClassNotFoundException {
        PublicKey pubServer = (PublicKey) in.readObject();
        out.writeObject(pub);
        out.flush();
        return pubServer;
    }
    public static PublicKey intercambiarClavesServidor(ObjectOutputStream out, ObjectInputStream in, PublicKey pub) throws IOException, ClassNotFoundException {
        out.writeObject(pub);
        out.flush();
        return (PublicKey) in.readObject();
    }
}