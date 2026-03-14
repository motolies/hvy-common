package kr.hvy.common.infrastructure.redis.cache;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import kr.hvy.common.config.cache.TwoTierCache;
import kr.hvy.common.config.cache.TwoTierCacheConfigurer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

/**
 * Redis Pub/Sub을 통한 크로스 Pod L1 캐시 무효화 리스너
 * <p>
 * 다른 Pod에서 발행한 캐시 삭제 메시지를 수신하여 로컬 L1(Caffeine)만 삭제합니다.
 * L2(Redis)는 공유 자원이므로 이미 삭제된 상태입니다.
 */
@Slf4j
public class CacheInvalidationListener implements MessageListener<CacheInvalidationMessage> {

  private final RedissonClient redissonClient;
  private final TwoTierCacheConfigurer cacheConfigurer;
  private final String instanceId;
  private int listenerId;

  public CacheInvalidationListener(RedissonClient redissonClient,
      TwoTierCacheConfigurer cacheConfigurer, String instanceId) {
    this.redissonClient = redissonClient;
    this.cacheConfigurer = cacheConfigurer;
    this.instanceId = instanceId;
  }

  @PostConstruct
  public void subscribe() {
    RTopic topic = redissonClient.getTopic(CacheInvalidationPublisher.TOPIC_NAME);
    listenerId = topic.addListener(CacheInvalidationMessage.class, this);
    log.info("Cache invalidation listener registered: instanceId={}", instanceId);
  }

  @PreDestroy
  public void unsubscribe() {
    RTopic topic = redissonClient.getTopic(CacheInvalidationPublisher.TOPIC_NAME);
    topic.removeListener(listenerId);
    log.info("Cache invalidation listener unregistered");
  }

  @Override
  public void onMessage(CharSequence channel, CacheInvalidationMessage msg) {
    if (instanceId.equals(msg.sourceInstanceId())) {
      return;
    }

    Map<String, TwoTierCache> registry = cacheConfigurer.getCacheRegistry();
    TwoTierCache cache = registry.get(msg.cacheName());
    if (cache == null) {
      log.debug("Ignoring invalidation for unknown cache: {}", msg.cacheName());
      return;
    }

    switch (msg.type()) {
      case EVICT -> {
        cache.evictLocal(msg.key());
        log.debug("L1 evicted by remote: {}/{}", msg.cacheName(), msg.key());
      }
      case CLEAR -> {
        cache.clearLocal();
        log.debug("L1 cleared by remote: {}", msg.cacheName());
      }
    }
  }
}
