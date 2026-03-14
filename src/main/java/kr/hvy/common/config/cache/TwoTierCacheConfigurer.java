package kr.hvy.common.config.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kr.hvy.common.aop.cache.CacheLoggingProperties;
import kr.hvy.common.aop.cache.LoggingStatsCounter;
import kr.hvy.common.config.jackson.ObjectMapperConfigurer;
import kr.hvy.common.infrastructure.redis.cache.CacheInvalidationPublisher;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

/**
 * L1(Caffeine) + L2(Redis) 2단계 캐시 설정 베이스 클래스
 * <p>
 * 각 모듈에서 이 클래스를 상속하여 CacheManager 빈을 생성합니다.
 * <pre>
 * {@literal @}Configuration
 * public class CacheConfig extends TwoTierCacheConfigurer {
 *   {@literal @}Bean
 *   public CacheManager cacheManager() {
 *     return super.twoTierCacheManager(List.of(
 *       TwoTierCacheProperties.l1Only("category", Duration.ofDays(1), 200, false),
 *       TwoTierCacheProperties.twoTier("masterCode", Duration.ofMinutes(10), 50, Duration.ofHours(1))
 *     ));
 *   }
 * }
 * </pre>
 */
@EnableCaching
public class TwoTierCacheConfigurer {

  @Autowired(required = false)
  private CacheLoggingProperties cacheLoggingProperties;

  @Autowired(required = false)
  private RedissonClient redissonClient;

  @Autowired(required = false)
  private CacheInvalidationPublisher publisher;

  private static final ObjectMapper objectMapper = createL2ObjectMapper();

  /**
   * L2(Redis) 직렬화용 ObjectMapper
   * EVERYTHING default typing을 사용하여 타입 정보를 JSON에 포함시킵니다.
   * 이를 통해 역직렬화 시 LinkedHashMap이 아닌 원래 타입으로 복원됩니다.
   */
  private static ObjectMapper createL2ObjectMapper() {
    ObjectMapper mapper = ObjectMapperConfigurer.getObjectMapper();
    mapper.activateDefaultTyping(
        LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.EVERYTHING,
        JsonTypeInfo.As.PROPERTY
    );
    return mapper;
  }

  private final Map<String, TwoTierCache> cacheRegistry = new ConcurrentHashMap<>();

  protected CacheManager twoTierCacheManager(List<TwoTierCacheProperties> cacheProps) {
    SimpleCacheManager manager = new SimpleCacheManager();

    List<TwoTierCache> caches = cacheProps.stream()
        .map(this::createTwoTierCache)
        .toList();

    caches.forEach(c -> cacheRegistry.put(c.getName(), c));

    manager.setCaches(caches);
    return manager;
  }

  public Map<String, TwoTierCache> getCacheRegistry() {
    return cacheRegistry;
  }

  private TwoTierCache createTwoTierCache(TwoTierCacheProperties props) {
    CaffeineCache l1 = buildCaffeineCache(props);

    RMapCache<String, String> l2 = null;
    if (props.l2Enabled() && redissonClient != null) {
      l2 = redissonClient.getMapCache(props.l2KeyPrefix());
    }

    CacheInvalidationPublisher pub = props.l2Enabled() ? publisher : null;

    return new TwoTierCache(props.name(), l1, l2, props, objectMapper, pub);
  }

  private CaffeineCache buildCaffeineCache(TwoTierCacheProperties props) {
    Caffeine<Object, Object> builder = Caffeine.newBuilder()
        .expireAfterWrite(props.l1Ttl())
        .maximumSize(props.l1MaxSize());

    if (cacheLoggingProperties != null && cacheLoggingProperties.isEnabled()) {
      builder.recordStats(() -> new LoggingStatsCounter(props.name(), cacheLoggingProperties));
    } else {
      builder.recordStats();
    }

    return new CaffeineCache(props.name(), builder.build(), props.allowNullValues());
  }
}
