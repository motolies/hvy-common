package kr.hvy.common.redis;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@Slf4j
@ConditionalOnBean(RedissonClient.class)
@RequiredArgsConstructor
public class RedissonUtils {

  private final RedissonClient redissonClient;

  /**
   * Redisson lock 획득
   *
   * @param waitTime  대기 시간
   * @param leaseTime Lock timeout (Lock 획득후 강제반환 시간)
   * @param timeUnit
   * @param keys
   * @return
   */
  public Optional<RLock> tryLock(long waitTime, long leaseTime, TimeUnit timeUnit, String keys) {
    RLock lock = redissonClient.getLock(keys);
    try {
      boolean isLocked = lock.tryLock(waitTime, leaseTime, timeUnit);
      if (!isLocked) {
        return Optional.empty();
      }
    } catch (InterruptedException e) {
      log.warn("", e);
      return Optional.empty();
    }

    return Optional.of(lock);
  }

  public Optional<RLock> tryLock(long waitTime, TimeUnit timeUnit, String keys) {
    return tryLock(waitTime, -1, timeUnit, keys);
  }

  public Optional<RLock> tryLock(String keys) {
    return tryLock(3l, -1, TimeUnit.SECONDS, keys);
  }

  public void unlock(String keys) {
    RLock lock = redissonClient.getLock(keys);
    lock.unlock();
  }

  public void unlock(RLock lock) {
    lock.unlock();
  }

}
