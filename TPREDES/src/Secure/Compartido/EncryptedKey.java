package Secure.Compartido;

import java.io.Serializable;

public class EncryptedKey implements Serializable {
    public byte[] encryptedAesKey;
    public byte[] signedHash;

    public EncryptedKey(byte[] encryptedAesKey, byte[] signedHash) {
        this.encryptedAesKey = encryptedAesKey;
        this.signedHash = signedHash;
    }
}
