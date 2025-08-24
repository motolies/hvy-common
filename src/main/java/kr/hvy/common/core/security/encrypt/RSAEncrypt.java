package kr.hvy.common.core.security.encrypt;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RSAEncrypt {

  private static final String algorithm = "RSA";

  public static KeyPair makeRsaKeyPair() throws NoSuchAlgorithmException {

    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
    /* 1024비트의 공개키와 개인키를 생성
     * 개인 블로그니까 1024비트로 충분하다고 생각했음
     * 공개 서비스인 경우 2048비트로 해야함
     */
    keyPairGenerator.initialize(1024);

    return keyPairGenerator.genKeyPair();
  }

  public static byte[] getKeyBytes(String key) {
    return Base64.getDecoder().decode(key);
  }

  public static PrivateKey getPrivateKey(String key) throws InvalidKeySpecException, NoSuchAlgorithmException {
    byte[] decodedKey = Base64.getDecoder().decode(key);
    return KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
  }

  public static PublicKey getPublicKey(String key) throws InvalidKeySpecException, NoSuchAlgorithmException {
    byte[] decodedKey = Base64.getDecoder().decode(key);
    return KeyFactory.getInstance(algorithm).generatePublic(new PKCS8EncodedKeySpec(decodedKey));
  }

  public static String getDecryptMessage(String secretMsg, byte[] privateKey) throws NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException {

    PrivateKey privKey = KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(privateKey));

    // RSA/ECB/OAEPWithSHA-256AndMGF1Padding
    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
    cipher.init(Cipher.DECRYPT_MODE, privKey);

    // RSA/ECB/OAEPPadding
//		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
//		OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT);
//		cipher.init(Cipher.DECRYPT_MODE, privKey, oaepParams);

    byte[] bMsg = Base64.getDecoder().decode(secretMsg);
    byte[] decryptMsg = cipher.doFinal(bMsg);
    return new String(decryptMsg);
  }

  public static String getDecryptMessage(String secretMsg, String privateKey) throws NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
    Cipher cipher = Cipher.getInstance(algorithm);
    PrivateKey pk = getPrivateKey(privateKey);
    cipher.init(Cipher.DECRYPT_MODE, pk);
    byte[] bMsg = Base64.getDecoder().decode(secretMsg);
    byte[] decryptMsg = cipher.doFinal(bMsg);
    return new String(decryptMsg);
  }

}