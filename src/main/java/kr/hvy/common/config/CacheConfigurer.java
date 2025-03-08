package kr.hvy.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

@EnableCaching
public class CacheConfigurer {

  protected CacheManager localCacheManager(List<CaffeineCache> caches) {
    SimpleCacheManager cacheManager = new SimpleCacheManager();
    cacheManager.setCaches(caches);
    return cacheManager;
  }

  protected CaffeineCache localCache(String name, Duration timeout, int maxSize, boolean allowNullValues) {
    return new CaffeineCache(name,
        Caffeine.newBuilder()
            .expireAfterWrite(timeout)
            .maximumSize(maxSize)
            .recordStats() // 통계 정보를 수집하도록 설정
            .build()
        , allowNullValues);
  }
}
