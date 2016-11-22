package fb;
/**
 *
 * @author Babak Alipour (babak.alipour@gmail.com)
 */
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

//Credits: http://javapapers.com/java/java-symmetric-aes-encryption-decryption-using-jce/
public class AES {

    private KeyGenerator keyGenerator;
    private Cipher cipher;

    public AES() {
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
            cipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Logger.getLogger(AES.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     * @return new secret key based on a strong random number
     * @throws NoSuchAlgorithmException
     */
    public SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        SecureRandom random;
        random = SecureRandom.getInstanceStrong();
        keyGenerator.init(256, random);
        SecretKey secretKey = keyGenerator.generateKey();
        return secretKey;
    }

    /**
     *
     * @param plainText
     * @param secretKey
     * @return Encrypted String
     * @throws Exception
     */
    public String encrypt(String plainText, SecretKey secretKey) throws Exception {
        byte[] plainTextByte = plainText.getBytes();
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedByte = cipher.doFinal(plainTextByte);
        Base64.Encoder encoder = Base64.getEncoder();
        String encryptedText = encoder.encodeToString(encryptedByte);
        return encryptedText;
    }

    /**
     *
     * @param encryptedText
     * @param secretKey
     * @return Decrypted String
     * @throws Exception
     */
    public String decrypt(String encryptedText, SecretKey secretKey) throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] encryptedTextByte = decoder.decode(encryptedText);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedByte = cipher.doFinal(encryptedTextByte);
        String decryptedText = new String(decryptedByte);
        return decryptedText;
    }
}
