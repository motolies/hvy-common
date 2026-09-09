package kr.hvy.common.infrastructure.redis.cache;

import java.util.UUID;
import kr.hvy.common.config.cache.TwoTierCacheConfigurer;
import kr.hvy.common.observability.TraceBoundary;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
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

  /**
   * TraceBoundary 는 소비 모듈이 등록하지 않았을 수도 있으므로 ObjectProvider 로 선택 주입한다.
   * 없으면 리스너는 트레이스 경계 없이 기존과 동일하게 동작한다.
   */
  @Bean
  @ConditionalOnBean(TwoTierCacheConfigurer.class)
  public CacheInvalidationListener cacheInvalidationListener(
      RedissonClient redissonClient, TwoTierCacheConfigurer cacheConfigurer,
      ObjectProvider<TraceBoundary> traceBoundaryProvider) {
    return new CacheInvalidationListener(redissonClient, cacheConfigurer, instanceId,
        traceBoundaryProvider.getIfAvailable());
  }
}
