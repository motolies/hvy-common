package kr.hvy.common.application.crypto;

/**
 * 암호화 실패. EncryptionInterceptor 는 이 예외를 전파해 평문이 DB 에 바인딩되는 것을 막는다(fail-closed).
 */
public class EncryptionException extends CryptoException {

  public EncryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
