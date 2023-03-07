package misc;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Cryptographer {
    public static byte[] encryptAES256(String plaintext, String key) throws Exception {
        byte[] keyBytes = Arrays.copyOf(MessageDigest.getInstance("SHA-256")
                .digest(key.getBytes(StandardCharsets.UTF_8)), 32);
        Key aesKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
    }
    public static byte[] encryptAES256(String plaintext, byte[] keyBytes) throws Exception {
        if (keyBytes.length != 32) throw new Exception("Unexpected keyBytes size");
        Key aesKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public static String decryptAES256(byte[] ciphertext, String key) throws Exception {
        byte[] keyBytes = Arrays.copyOf(MessageDigest.getInstance("SHA-256")
                .digest(key.getBytes(StandardCharsets.UTF_8)), 32);
        Key aesKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }
    public static String decryptAES256(byte[] ciphertext, byte[] keyBytes) throws Exception {
        if (keyBytes.length != 32) throw new Exception("Unexpected keyBytes size");
        Key aesKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }
    public static int hashDJB2(String input){
        int hash = 5381;
        for (int i = 0; i < input.length(); i++) {
            hash = ((hash << 5) + hash) + input.charAt(i);
        }
        return hash;
    }
}
