package kr.hvy.common.infrastructure.redis.lock;


import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import kr.hvy.common.core.exception.RedissonLockAcquisitionException;
import kr.hvy.common.infrastructure.redis.util.RedissonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnBean(RedissonUtils.class)
public class RedissonLockTemplate {

  private static final String REDISSON_LOCK_PREFIX = "LOCK:";
  private final RedissonUtils redissonUtils;


  private static final long DEFAULT_WAIT_TIME = 10L;
  private static final long DEFAULT_LEASE_TIME = 10L;

  /**
   * 기본 대기 시간(10초)과 락 유지 시간(10초)을 사용합니다.
   * <pre>
   *       public String processOrder() {
   *         return template.execute("order:orderId", () -> {
   *             // 개별 로직
   *             return "OK";
   *         });
   *     }
   * </pre>
   *
   * @param <T>     the type parameter
   * @param lockKey the lock key
   * @param action  the action
   * @return the t
   */
  public <T> T execute(String lockKey, Callable<T> action) {
    return execute(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, action);
  }

  /**
   * 대기 시간과 락 유지 시간을 초 단위로 지정할 수 있습니다.
   * <pre>
   *       public String processOrder() {
   *         return template.execute("order:orderId", 5, 30, () -> {
   *             // 개별 로직 (최대 5초 대기, 30초간 락 유지)
   *             return "OK";
   *         });
   *     }
   * </pre>
   *
   * @param <T>       the type parameter
   * @param lockKey   the lock key
   * @param waitTime  락 획득 대기 시간 (초)
   * @param leaseTime 락 유지 시간 (초)
   * @param action    the action
   * @return the t
   */
  public <T> T execute(String lockKey, long waitTime, long leaseTime, Callable<T> action) {
    String lockName = REDISSON_LOCK_PREFIX + lockKey;

    // RedissonUtils를 사용하여 락 획득 시도
    Optional<RLock> lockOptional = redissonUtils.tryLock(waitTime, leaseTime, TimeUnit.SECONDS, lockName);

    // 락 획득 실패 시 예외 발생
    RLock rLock = lockOptional.orElseThrow(() -> new RedissonLockAcquisitionException(lockName));

    try {
      return action.call();
    } catch (Exception e) {
      log.error("### Distributed Lock Template ERROR : {}", e.getMessage(), e);
      throw new RuntimeException(e);
    } finally {
      try {
        // RedissonUtils를 사용하여 락 해제
        redissonUtils.unlock(rLock);
      } catch (IllegalMonitorStateException e) {
        log.info("Redisson Lock Already UnLock : {}", lockName);
      }
      log.debug("### Distributed Lock Template End {}", lockName);
    }
  }

}
