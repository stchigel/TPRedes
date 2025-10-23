package Secure.Server;

import Secure.Compartido.Claves;
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

    public static SecretKey desencriptarClave(EncryptedKey encryptedKey, PrivateKey privReceptor, PublicKey pubEmisor) throws Exception {
        byte[] encryptedAesKey = encryptedKey.encryptedAesKey;
        byte[] signedHash = encryptedKey.signedHash;
        Cipher rsaDec = Cipher.getInstance("RSA");
        rsaDec.init(Cipher.DECRYPT_MODE, privReceptor);
        byte[] aesKeyBytes = rsaDec.doFinal(encryptedAesKey);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        System.out.println("Clave AES descifrada correctamente.");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] localHash = digest.digest(aesKeyBytes);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pubEmisor);
        sig.update(localHash);
        boolean verified = sig.verify(signedHash);
        if (verified) {
            System.out.println("Firma del cliente verificada. Clave AES auténtica.");
            return aesKey;
        } else {
            System.out.println("Firma inválida. Posible ataque o error.");
            System.exit(0);
            return null;
        }
    }
    public static PublicKey intercambiarClaves(ObjectOutputStream out, ObjectInputStream in, PublicKey pub) throws IOException, ClassNotFoundException {
        out.writeObject(pub);
        out.flush();
        return (PublicKey) in.readObject();
    }
    public static void iniciarCliente(ServerSocket socket, HostModerador hm, Claves claves, ObjectOutputStream outMod, SecretKey aesKeyMod) throws Exception {
        Socket clientSocket = socket.accept();
        ObjectOutputStream outC = new ObjectOutputStream(clientSocket.getOutputStream());
        ObjectInputStream inC = new ObjectInputStream(clientSocket.getInputStream());
        PublicKey pubCliente = intercambiarClaves(outC, inC, claves.pub);
        SecretKey aesKeyCliente = desencriptarClave((EncryptedKey) inC.readObject(), claves.priv, pubCliente);
        hm.addCliente(clientSocket.getInetAddress().getHostAddress(), outC, aesKeyCliente);
        HostCliente hc = new HostCliente(inC, outMod, aesKeyMod, aesKeyCliente, claves.priv, pubCliente);
        hc.start();
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        Claves claves = new Claves("RSA", 2048);
        try (ServerSocket serverSocketCli = new ServerSocket(30000);
             ServerSocket serverSocketMod = new ServerSocket(30001)) {
            Socket clientSocketMod = serverSocketMod.accept();
            ObjectOutputStream out = new ObjectOutputStream(clientSocketMod.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocketMod.getInputStream());

            PublicKey pubModerador = intercambiarClaves(out, in, claves.pub);
            SecretKey aesKey = desencriptarClave((EncryptedKey) in.readObject(), claves.priv, pubModerador);

            HostModerador hm = new HostModerador(in, aesKey, claves.priv, pubModerador);
            hm.start();

            while (true) {
                Socket clientSocket = serverSocketCli.accept();
                ObjectOutputStream outC = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream inC = new ObjectInputStream(clientSocket.getInputStream());
                PublicKey pubCliente = intercambiarClaves(outC, inC, claves.pub);
                SecretKey aesKeyCliente = desencriptarClave((EncryptedKey) inC.readObject(), claves.priv, pubCliente);
                hm.addCliente(clientSocket.getInetAddress().getHostAddress(), outC, aesKeyCliente);
                HostCliente hc = new HostCliente(inC, out, aesKey, aesKeyCliente, claves.priv, pubCliente);
                hc.start();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
