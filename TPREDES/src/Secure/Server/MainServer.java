package Secure.Server;

import Secure.Compartido.EncryptedKey;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.util.Base64;

public class MainServer {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair serverKeys = keyGen.generateKeyPair();
        PublicKey pub = serverKeys.getPublic();
        PrivateKey priv = serverKeys.getPrivate();

        try (ServerSocket serverSocketCli = new ServerSocket(30000);
             ServerSocket serverSocketMod = new ServerSocket(30001)) {

            Socket clientSocketMod = serverSocketMod.accept();

            ObjectOutputStream out = new ObjectOutputStream(clientSocketMod.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocketMod.getInputStream());

            out.writeObject(pub);
            out.flush();
            System.out.println("Clave pública del servidor enviada.");
            PublicKey pubModerador = (PublicKey) in.readObject();
            System.out.println("Clave pública del mod recibida.");

            EncryptedKey encryptedKey = (EncryptedKey) in.readObject();
            byte[] encryptedAesKey = encryptedKey.encryptedAesKey;
            byte[] signedHash = encryptedKey.signedHash;

            Cipher rsaDec = Cipher.getInstance("RSA");
            rsaDec.init(Cipher.DECRYPT_MODE, priv);
            byte[] aesKeyBytes = rsaDec.doFinal(encryptedAesKey);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
            System.out.println("Clave AES descifrada correctamente.");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] localHash = digest.digest(aesKeyBytes);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pubModerador);
            sig.update(localHash);
            boolean verified = sig.verify(signedHash);

            if (verified) {
                System.out.println("Firma del cliente verificada. Clave AES auténtica.");
            } else {
                System.out.println("Firma inválida. Posible ataque o error.");
                clientSocketMod.close();
                serverSocketMod.close();
                System.exit(0);
            }

            HostModerador hm = new HostModerador(in, aesKey, pubModerador);
            hm.start();

            while (true) {
                Socket clientSocket = serverSocketCli.accept();

                ObjectOutputStream outC = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream inC = new ObjectInputStream(clientSocket.getInputStream());

                String ip = clientSocket.getInetAddress().getHostAddress();
                hm.addCliente(ip, outC);

                HostCliente hc = new HostCliente(inC, out);
                hc.start();
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
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

    public String firmar(String mensaje){
        return mensaje + " ,firmado con clave privada";
    }
}
