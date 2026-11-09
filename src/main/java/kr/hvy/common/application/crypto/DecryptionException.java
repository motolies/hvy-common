package kr.hvy.common.application.crypto;

/**
 * 복호화 실패(키 불일치·변조·손상·암호문이 아닌 값).
 * 암호문 문자열을 결과로 남기지 않고 명시적으로 실패시켜 키 오설정과 평문 혼입을 드러낸다.
 */
public class DecryptionException extends CryptoException {

  public DecryptionException(String message) {
    super(message);
  }

  public DecryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
