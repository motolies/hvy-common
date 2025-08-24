package kr.hvy.common.core.exception;

/**
 * 레이트 리미트가 초과되었을 때 발생하는 예외
 */
public class RateLimitExceededException extends RuntimeException {

  public RateLimitExceededException() {
    super();
  }

  public RateLimitExceededException(String message) {
    super(message);
  }

  public RateLimitExceededException(String message, Throwable cause) {
    super(message, cause);
  }

  public RateLimitExceededException(Throwable cause) {
    super(cause);
  }
}