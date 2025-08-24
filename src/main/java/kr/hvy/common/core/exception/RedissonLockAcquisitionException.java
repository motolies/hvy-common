package kr.hvy.common.core.exception;

public class RedissonLockAcquisitionException extends RuntimeException {

  public RedissonLockAcquisitionException(String message) {
    super(message);
  }

  public RedissonLockAcquisitionException(String message, Throwable cause) {
    super(message, cause);
  }

  public RedissonLockAcquisitionException(Throwable cause) {
    super(cause);
  }

  public RedissonLockAcquisitionException() {
    super("Rest API Error");
  }
}
