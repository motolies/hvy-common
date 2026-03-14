package kr.hvy.common.infrastructure.redis.cache;

import java.util.UUID;
import kr.hvy.common.config.cache.TwoTierCacheConfigurer;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 무효화 Pub/Sub 설정
 * RedissonClient가 존재할 때만 활성화됩니다.
 */
@Configuration
@ConditionalOnBean(RedissonClient.class)
public class CacheInvalidationConfig {

  private final String instanceId = UUID.randomUUID().toString();

  @Bean
  public CacheInvalidationPublisher cacheInvalidationPublisher(RedissonClient redissonClient) {
    return new CacheInvalidationPublisher(redissonClient, instanceId);
  }

  @Bean
  @ConditionalOnBean(TwoTierCacheConfigurer.class)
  public CacheInvalidationListener cacheInvalidationListener(
      RedissonClient redissonClient, TwoTierCacheConfigurer cacheConfigurer) {
    return new CacheInvalidationListener(redissonClient, cacheConfigurer, instanceId);
  }
}
