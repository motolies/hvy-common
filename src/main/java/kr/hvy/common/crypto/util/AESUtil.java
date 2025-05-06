package kr.hvy.common.crypto.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import kr.hvy.common.crypto.Decryptor;
import kr.hvy.common.crypto.Encryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

public class AESUtil implements Encryptor, Decryptor {

  private static final String KEY_ALGORITHM = "AES";
  private static final String FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final String PADDING = "AES/CBC/PKCS5Padding";

  private static String secretKey;

  @Value("${system.crypto.aes.key}")
  public void setSecretKey(String secretKey) {
    AESUtil.secretKey = secretKey;
  }

  private static final int KEY_LENGTH = 256;
  private static final int ITERATION_COUNT = 8192;
  private static final int IV_LENGTH = 16;
  private static final int SALT_LENGTH = 16;


  public String encrypt(String data) throws Exception {
    if (!StringUtils.hasText(data)) {
      return data;
    }

    byte[] salt = generateSalt();
    byte[] iv = generateIV();
    IvParameterSpec ivspec = new IvParameterSpec(iv);

    SecretKey secretKeyObject = generateSecretKey(secretKey, salt);
    Cipher cipher = Cipher.getInstance(PADDING);
    cipher.init(Cipher.ENCRYPT_MODE, secretKeyObject, ivspec);

    byte[] cipherText = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
    byte[] encryptedData = combineArrays(salt, iv, cipherText);

    return Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedData);
  }

  public String decrypt(String encryptedData) throws Exception {
    if (!StringUtils.hasText(encryptedData)) {
      return encryptedData;
    }

    byte[] encryptedDataBytes = Base64.getUrlDecoder().decode(encryptedData);
    byte[] salt = extractSalt(encryptedDataBytes);
    byte[] iv = extractIV(encryptedDataBytes);
    IvParameterSpec ivspec = new IvParameterSpec(iv);

    SecretKey secretKeyObject = generateSecretKey(secretKey, salt);
    Cipher cipher = Cipher.getInstance(PADDING);
    cipher.init(Cipher.DECRYPT_MODE, secretKeyObject, ivspec);

    byte[] cipherText = extractCipherText(encryptedDataBytes);
    byte[] decryptedText = cipher.doFinal(cipherText);

    return new String(decryptedText, StandardCharsets.UTF_8);
  }

  private static SecretKey generateSecretKey(String secretKey, byte[] salt) throws Exception {
    SecretKeyFactory factory = SecretKeyFactory.getInstance(FACTORY_ALGORITHM);
    KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
    SecretKey tmp = factory.generateSecret(spec);
    return new SecretKeySpec(tmp.getEncoded(), KEY_ALGORITHM);
  }

  private static byte[] generateSalt() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] salt = new byte[SALT_LENGTH];
    secureRandom.nextBytes(salt);
    return salt;
  }

  private static byte[] generateIV() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] iv = new byte[IV_LENGTH];
    secureRandom.nextBytes(iv);
    return iv;
  }

  private static byte[] combineArrays(byte[] salt, byte[] iv, byte[] array1) {
    byte[] combinedArray = new byte[salt.length + iv.length + array1.length];
    System.arraycopy(salt, 0, combinedArray, 0, salt.length);
    System.arraycopy(iv, 0, combinedArray, salt.length, iv.length);
    System.arraycopy(array1, 0, combinedArray, salt.length + iv.length, array1.length);
    return combinedArray;
  }

  private static byte[] extractSalt(byte[] encryptedData) {
    byte[] salt = new byte[SALT_LENGTH];
    System.arraycopy(encryptedData, 0, salt, 0, SALT_LENGTH);
    return salt;
  }

  private static byte[] extractIV(byte[] encryptedData) {
    byte[] iv = new byte[IV_LENGTH];
    System.arraycopy(encryptedData, SALT_LENGTH, iv, 0, IV_LENGTH);
    return iv;
  }

  private static byte[] extractCipherText(byte[] encryptedData) {
    byte[] cipherText = new byte[encryptedData.length - (SALT_LENGTH + IV_LENGTH)];
    System.arraycopy(encryptedData, SALT_LENGTH + IV_LENGTH, cipherText, 0, cipherText.length);
    return cipherText;
  }

}