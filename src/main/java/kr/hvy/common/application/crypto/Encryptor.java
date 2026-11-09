package kr.hvy.common.application.crypto;

public interface Encryptor {

  /**
   * 평문을 암호화한다. 빈값/null 은 그대로 반환한다.
   *
   * @throws EncryptionException 암호화 실패
   */
  String encrypt(String data);
}
