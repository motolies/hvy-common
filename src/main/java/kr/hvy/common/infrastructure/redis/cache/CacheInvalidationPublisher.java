package kr.hvy.common.infrastructure.redis.cache;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

/**
 * 캐시 무효화 이벤트를 Redis Pub/Sub으로 발행
 */
@Slf4j
public class CacheInvalidationPublisher {

  public static final String TOPIC_NAME = "cache:invalidation";

  private final RTopic topic;
  private final String instanceId;

  public CacheInvalidationPublisher(RedissonClient redissonClient, String instanceId) {
    this.topic = redissonClient.getTopic(TOPIC_NAME);
    this.instanceId = instanceId;
  }

  public void publishEvict(String cacheName, String key) {
    try {
      var msg = new CacheInvalidationMessage(instanceId, cacheName, key, CacheInvalidationMessage.MessageType.EVICT);
      topic.publish(msg);
      log.debug("Cache invalidation published: EVICT {}/{}", cacheName, key);
    } catch (Exception e) {
      log.warn("Failed to publish cache invalidation: {}", e.getMessage());
    }
  }

  public void publishClear(String cacheName) {
    try {
      var msg = new CacheInvalidationMessage(instanceId, cacheName, null, CacheInvalidationMessage.MessageType.CLEAR);
      topic.publish(msg);
      log.debug("Cache invalidation published: CLEAR {}", cacheName);
    } catch (Exception e) {
      log.warn("Failed to publish cache invalidation: {}", e.getMessage());
    }
  }
}
