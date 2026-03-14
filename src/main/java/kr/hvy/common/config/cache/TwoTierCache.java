package kr.hvy.common.config.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import kr.hvy.common.infrastructure.redis.cache.CacheInvalidationPublisher;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;

/**
 * L1(Caffeine) + L2(Redis) 2단계 캐시 구현체
 * <p>
 * 조회: L1 -> L2 -> valueLoader(DB)
 * 저장: L1 + L2 동시 저장
 * 삭제: L1 + L2 삭제 + Pub/Sub 발행 (크로스 Pod 무효화)
 */
@Slf4j
public class TwoTierCache implements Cache {

  private final String name;
  private final CaffeineCache l1;
  private final RMapCache<String, String> l2;
  private final TwoTierCacheProperties props;
  private final ObjectMapper objectMapper;
  private final CacheInvalidationPublisher publisher;

  public TwoTierCache(String name, CaffeineCache l1, RMapCache<String, String> l2,
      TwoTierCacheProperties props, ObjectMapper objectMapper, CacheInvalidationPublisher publisher) {
    this.name = name;
    this.l1 = l1;
    this.l2 = l2;
    this.props = props;
    this.objectMapper = objectMapper;
    this.publisher = publisher;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Object getNativeCache() {
    return l1.getNativeCache();
  }

  @Override
  public ValueWrapper get(Object key) {
    // L1
    ValueWrapper l1Val = l1.get(key);
    if (l1Val != null) {
      return l1Val;
    }

    if (l2 == null) {
      return null;
    }

    // L2
    String json = safeGetL2(String.valueOf(key));
    if (json == null) {
      return null;
    }

    Object value = deserialize(json);
    if (value != null) {
      l1.put(key, value);
      return () -> value;
    }
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(Object key, Class<T> type) {
    // L1
    T l1Val = l1.get(key, type);
    if (l1Val != null) {
      return l1Val;
    }

    if (l2 == null) {
      return null;
    }

    // L2
    String json = safeGetL2(String.valueOf(key));
    if (json == null) {
      return null;
    }

    T value = deserialize(json, type);
    if (value != null) {
      l1.put(key, value);
    }
    return value;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(Object key, Callable<T> valueLoader) {
    // L1
    ValueWrapper l1Val = l1.get(key);
    if (l1Val != null) {
      return (T) l1Val.get();
    }

    // L2
    if (l2 != null) {
      String json = safeGetL2(String.valueOf(key));
      if (json != null) {
        Object value = deserialize(json);
        if (value != null) {
          l1.put(key, value);
          return (T) value;
        }
      }
    }

    // valueLoader (DB)
    T value;
    try {
      value = valueLoader.call();
    } catch (Exception e) {
      throw new ValueRetrievalException(key, valueLoader, e);
    }
    put(key, value);
    return value;
  }

  @Override
  public void put(Object key, Object value) {
    l1.put(key, value);
    if (l2 != null && value != null) {
      safePutL2(String.valueOf(key), value);
    }
  }

  @Override
  public void evict(Object key) {
    l1.evict(key);
    if (l2 != null) {
      safeRemoveL2(String.valueOf(key));
    }
    if (publisher != null) {
      publisher.publishEvict(name, String.valueOf(key));
    }
  }

  @Override
  public void clear() {
    l1.clear();
    if (l2 != null) {
      safeClearL2();
    }
    if (publisher != null) {
      publisher.publishClear(name);
    }
  }

  /**
   * 다른 Pod에서 수신한 무효화 메시지 처리 - L1만 삭제
   */
  public void evictLocal(String key) {
    l1.evict(key);
  }

  /**
   * 다른 Pod에서 수신한 무효화 메시지 처리 - L1 전체 삭제
   */
  public void clearLocal() {
    l1.clear();
  }

  // ========== L2 안전 접근 (Redis 장애 시 graceful degradation) ==========

  private String safeGetL2(String key) {
    try {
      return l2.get(key);
    } catch (Exception e) {
      log.warn("L2 cache read failed, key={}: {}", key, e.getMessage());
      return null;
    }
  }

  private void safePutL2(String key, Object value) {
    try {
      String json = objectMapper.writeValueAsString(value);
      l2.put(key, json, props.l2Ttl().toMillis(), TimeUnit.MILLISECONDS);
    } catch (JsonProcessingException e) {
      log.warn("L2 cache serialize failed, key={}: {}", key, e.getMessage());
    } catch (Exception e) {
      log.warn("L2 cache write failed, key={}: {}", key, e.getMessage());
    }
  }

  private void safeRemoveL2(String key) {
    try {
      l2.remove(key);
    } catch (Exception e) {
      log.warn("L2 cache remove failed, key={}: {}", key, e.getMessage());
    }
  }

  private void safeClearL2() {
    try {
      l2.clear();
    } catch (Exception e) {
      log.warn("L2 cache clear failed: {}", e.getMessage());
    }
  }

  // ========== 직렬화/역직렬화 ==========

  private Object deserialize(String json) {
    try {
      return objectMapper.readValue(json, Object.class);
    } catch (JsonProcessingException e) {
      log.warn("L2 cache deserialize failed: {}", e.getMessage());
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T deserialize(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      log.warn("L2 cache deserialize failed for type {}: {}", type.getSimpleName(), e.getMessage());
      return null;
    }
  }
}
