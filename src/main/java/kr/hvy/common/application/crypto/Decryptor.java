package kr.hvy.common.application.crypto;

public interface Decryptor {

  /**
   * 암호문을 복호화한다. 빈값/null 은 그대로 반환한다.
   * 암호문에 마커가 없으므로 비어 있지 않은 값은 모두 암호문으로 간주한다.
   *
   * @throws DecryptionException 복호화 실패(키 불일치·변조·손상·평문 입력)
   */
  String decrypt(String encryptedData);

}
