package kr.hvy.common.aop.cache;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.LongAdder;

/**
 * Caffeine 캐시 StatsCounter 구현체
 * 캐시 HIT/MISS를 직접 캡처하여 정확한 로깅을 제공합니다.
 */
@Slf4j
public class LoggingStatsCounter implements StatsCounter {

  private final String cacheName;
  private final CacheLoggingProperties properties;

  // Thread-safe 통계 카운터
  private final LongAdder hitCount = new LongAdder();
  private final LongAdder missCount = new LongAdder();
  private final LongAdder loadSuccessCount = new LongAdder();
  private final LongAdder loadFailureCount = new LongAdder();
  private final LongAdder totalLoadTime = new LongAdder();
  private final LongAdder evictionCount = new LongAdder();
  private final LongAdder evictionWeight = new LongAdder();

  public LoggingStatsCounter(String cacheName, CacheLoggingProperties properties) {
    this.cacheName = cacheName;
    this.properties = properties;
  }

  @Override
  public void recordHits(int count) {
    hitCount.add(count);

    if (properties.isEnabled() && properties.isLogHits()) {
      log.debug("[CACHE-HIT] {} - hits={}, totalHits={}",
          cacheName, count, hitCount.sum());
    }
  }

  @Override
  public void recordMisses(int count) {
    missCount.add(count);

    if (properties.isEnabled() && properties.isLogMisses()) {
      log.info("[CACHE-MISS] {} - misses={}, totalMisses={}",
          cacheName, count, missCount.sum());
    }
  }

  @Override
  public void recordLoadSuccess(long loadTime) {
    loadSuccessCount.increment();
    totalLoadTime.add(loadTime);

    if (properties.isEnabled()) {
      log.debug("[CACHE-LOAD-SUCCESS] {} - loadTime={}ns",
          cacheName, loadTime);
    }
  }

  @Override
  public void recordLoadFailure(long loadTime) {
    loadFailureCount.increment();
    totalLoadTime.add(loadTime);

    if (properties.isEnabled()) {
      log.warn("[CACHE-LOAD-FAILURE] {} - loadTime={}ns",
          cacheName, loadTime);
    }
  }

  @Override
  public void recordEviction(int weight, RemovalCause cause) {
    evictionCount.increment();
    evictionWeight.add(weight);

    if (properties.isEnabled() && properties.isLogEvictions()) {
      log.info("[CACHE-EVICTION] {} - weight={}, cause={}, totalEvictions={}",
          cacheName, weight, cause, evictionCount.sum());
    }
  }

  @Override
  public CacheStats snapshot() {
    return CacheStats.of(
        hitCount.sum(),
        missCount.sum(),
        loadSuccessCount.sum(),
        loadFailureCount.sum(),
        totalLoadTime.sum(),
        evictionCount.sum(),
        evictionWeight.sum()
    );
  }
}
