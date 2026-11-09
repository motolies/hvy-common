package kr.hvy.common.application.crypto;

/**
 * 암복호화 실패를 나타내는 런타임 예외의 공통 상위 타입.
 * 인터셉터는 이 예외를 잡아 메트릭을 남긴 뒤 그대로 전파한다(평문·암호문을 조용히 흘리지 않는다).
 */
public class CryptoException extends RuntimeException {

  public CryptoException(String message) {
    super(message);
  }

  public CryptoException(String message, Throwable cause) {
    super(message, cause);
  }
}
