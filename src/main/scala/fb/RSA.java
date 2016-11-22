package fb;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author Babak Alipour (babak.alipour@gmail.com)
 */
public class RSA {

    private Cipher cipher;
    private SecureRandom random;
    private Signature sig;

    public RSA() {
        try {
            cipher = Cipher.getInstance("RSA");
            random = SecureRandom.getInstanceStrong();
            sig = Signature.getInstance("SHA256withRSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Logger.getLogger(AES.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     * @param plainText
     * @param pu RSAPublicKey
     * @return Encrypted String
     * @throws Exception
     */
    public String encryptUsingPublicKey(String plainText, RSAPublicKey pu) throws Exception {
        byte[] plainTextByte = plainText.getBytes();
        cipher.init(Cipher.ENCRYPT_MODE, pu);
        byte[] encryptedByte = cipher.doFinal(plainTextByte);
        Base64.Encoder encoder = Base64.getEncoder();
        String encryptedText = encoder.encodeToString(encryptedByte);
        return encryptedText;
    }

    /**
     *
     * @param plainText
     * @param pk RSAPublicKey
     * @return Encrypted String
     * @throws Exception
     */
    public String encryptUsingPrivateKey(String plainText, RSAPrivateKey pk) throws Exception {
        byte[] plainTextByte = plainText.getBytes();
        cipher.init(Cipher.ENCRYPT_MODE, pk);
        byte[] encryptedByte = cipher.doFinal(plainTextByte);
        Base64.Encoder encoder = Base64.getEncoder();
        String encryptedText = encoder.encodeToString(encryptedByte);
        return encryptedText;
    }

    /**
     *
     * @param encryptedText
     * @param pu RSAPrivateKey
     * @return Decrypted String
     * @throws Exception
     */
    public String decryptUsingPublicKey(String encryptedText, RSAPublicKey pu) throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] encryptedTextByte = decoder.decode(encryptedText);
        cipher.init(Cipher.DECRYPT_MODE, pu);
        byte[] decryptedByte = cipher.doFinal(encryptedTextByte);
        String decryptedText = new String(decryptedByte);
        return decryptedText;
    }

    /**
     *
     * @param encryptedText
     * @param pk RSAPrivateKey
     * @return Decrypted String
     * @throws Exception
     */
    public String decryptUsingPrivateKey(String encryptedText, RSAPrivateKey pk) throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] encryptedTextByte = decoder.decode(encryptedText);
        cipher.init(Cipher.DECRYPT_MODE, pk);
        byte[] decryptedByte = cipher.doFinal(encryptedTextByte);
        String decryptedText = new String(decryptedByte);
        return decryptedText;
    }

    /**
     *
     * @param data
     * @param pk RSAPrivateKey
     * @return digital signature of data
     * @throws Exception
     */
    public String sign(String data, RSAPrivateKey pk) throws Exception {
        sig.initSign(pk, random);
        sig.update(data.getBytes());
        byte[] signatureBytes = sig.sign();
        Base64.Encoder encoder = Base64.getEncoder();
        String signature = encoder.encodeToString(signatureBytes);
        return signature;
    }

    /**
     *
     * @param data
     * @param signature
     * @param pu RSAPublicKey
     * @return boolean true if verification success, false otherwise
     * @throws Exception
     */
    public boolean verifySignature(String data, String signature, RSAPublicKey pu) throws Exception {
        sig.initVerify(pu);
        sig.update(data.getBytes());
        Base64.Decoder decoder = Base64.getDecoder();
        return sig.verify(decoder.decode(signature));
    }

}
