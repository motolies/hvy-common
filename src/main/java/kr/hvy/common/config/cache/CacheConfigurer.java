package kr.hvy.common.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import kr.hvy.common.aop.cache.CacheLoggingProperties;
import kr.hvy.common.aop.cache.LoggingStatsCounter;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

@EnableCaching
public class CacheConfigurer {

  @Autowired(required = false)
  private CacheLoggingProperties cacheLoggingProperties;

  protected CacheManager localCacheManager(List<CaffeineCache> caches) {
    SimpleCacheManager cacheManager = new SimpleCacheManager();
    cacheManager.setCaches(caches);
    return cacheManager;
  }

  protected CaffeineCache localCache(String name, Duration timeout, int maxSize, boolean allowNullValues) {
    Caffeine<Object, Object> builder = Caffeine.newBuilder()
        .expireAfterWrite(timeout)
        .maximumSize(maxSize);

    // CacheLoggingProperties가 설정되어 있고 enabled이면 LoggingStatsCounter 사용
    if (cacheLoggingProperties != null && cacheLoggingProperties.isEnabled()) {
      builder.recordStats(() -> new LoggingStatsCounter(name, cacheLoggingProperties));
    } else {
      // 그렇지 않으면 기본 통계만 수집
      builder.recordStats();
    }

    return new CaffeineCache(name, builder.build(), allowNullValues);
  }
}
