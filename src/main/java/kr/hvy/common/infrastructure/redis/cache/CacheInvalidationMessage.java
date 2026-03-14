package kr.hvy.common.infrastructure.redis.cache;

import java.io.Serializable;

/**
 * 크로스 Pod L1 캐시 무효화 메시지
 */
public record CacheInvalidationMessage(
    String sourceInstanceId,
    String cacheName,
    String key,
    MessageType type
) implements Serializable {

  public enum MessageType {
    EVICT, CLEAR
  }
}
