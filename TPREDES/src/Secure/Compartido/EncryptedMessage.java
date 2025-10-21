package Secure.Compartido;

import java.io.Serializable;

public class EncryptedMessage implements Serializable {
    public byte[] iv;
    public byte[] ciphertext;
    public byte[] signature; // firma digital (RSA)

    public EncryptedMessage(byte[] iv, byte[] ciphertext, byte[] signature) {
        this.iv = iv;
        this.ciphertext = ciphertext;
        this.signature = signature;
    }
}
