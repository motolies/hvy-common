package kr.hvy.common.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.concurrent.TimedSemaphore;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TimedSemaphoreHandler {

  private final ConcurrentHashMap<String, TimedSemaphore> semaphoreMap = new ConcurrentHashMap<>();
  // milliseconds
  private final int DEFAULT_TIME_PERIOD = 500;
  // numberOfOperationsToOperateSimultaneously
  private final int DEFAULT_LIMIT = 1;

  private TimedSemaphore getSemaphore(String key, Integer timePeriod, Integer limit) {
    int period = ObjectUtils.defaultIfNull(timePeriod, DEFAULT_TIME_PERIOD);
    int lim = ObjectUtils.defaultIfNull(limit, DEFAULT_LIMIT);
    return semaphoreMap.computeIfAbsent(key, k -> new TimedSemaphore(period, TimeUnit.MILLISECONDS, lim));
  }

  private void acquireSemaphore(TimedSemaphore semaphore) {
    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      log.error("acquireSemaphore : {}",e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  public void acquire(String key, Integer timePeriod, Integer limit) {
    acquireSemaphore(getSemaphore(key, timePeriod, limit));
  }

  public void acquire(String key, Integer timePeriod) {
    acquireSemaphore(getSemaphore(key, timePeriod, null));
  }

  public void acquire(String key) {
    acquireSemaphore(getSemaphore(key, null, null));
  }
}