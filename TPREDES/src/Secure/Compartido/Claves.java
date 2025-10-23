package Secure.Compartido;

import java.security.*;

public class Claves {
    KeyPair serverKeys;
    public PublicKey pub;
    public PrivateKey priv;

    public Claves(String algorithm, int keysize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
        keyGen.initialize(keysize);
        this.serverKeys = keyGen.generateKeyPair();
        this.pub = serverKeys.getPublic();
        this.priv = serverKeys.getPrivate();
    }
}
