package kr.hvy.common.infrastructure.redis.impl.masterdata.cache;

import java.time.Duration;
import java.util.List;
import kr.hvy.common.config.cache.TwoTierCacheProperties;

/**
 * 마스터코드 3개 캐시의 기본 TwoTier 설정 팩토리.
 * <p>
 * blog-back 과 소비 앱은 다음과 같이 사용한다.
 * <pre>
 * {@code
 * @Configuration
 * public class CacheConfig extends TwoTierCacheConfigurer {
 *   @Bean
 *   public CacheManager cacheManager() {
 *     return twoTierCacheManager(MasterCodeCacheProperties.all());
 *   }
 * }
 * }
 * </pre>
 * <p>
 * 기본값: L1 TTL 6시간, L2 TTL 1일. Caffeine 최대 크기는 tree=50, node=200, children=100.
 * 값은 blog-back 기존 설정과 동일하게 유지된다.
 */
public final class MasterCodeCacheProperties {

  private static final Duration DEFAULT_L1_TTL = Duration.ofHours(6);
  private static final Duration DEFAULT_L2_TTL = Duration.ofDays(1);

  private MasterCodeCacheProperties() {
  }

  /**
   * 3개 캐시(tree, node, children) 전체를 기본 TTL 로 생성.
   */
  public static List<TwoTierCacheProperties> all() {
    return List.of(
        TwoTierCacheProperties.twoTier(MasterCodeCacheNames.TREE, DEFAULT_L1_TTL, 50, DEFAULT_L2_TTL),
        TwoTierCacheProperties.twoTier(MasterCodeCacheNames.NODE, DEFAULT_L1_TTL, 200, DEFAULT_L2_TTL),
        TwoTierCacheProperties.twoTier(MasterCodeCacheNames.CHILDREN, DEFAULT_L1_TTL, 100, DEFAULT_L2_TTL)
    );
  }

  /**
   * 커스텀 TTL 이 필요한 경우 사용.
   */
  public static List<TwoTierCacheProperties> all(Duration l1Ttl, Duration l2Ttl) {
    return List.of(
        TwoTierCacheProperties.twoTier(MasterCodeCacheNames.TREE, l1Ttl, 50, l2Ttl),
        TwoTierCacheProperties.twoTier(MasterCodeCacheNames.NODE, l1Ttl, 200, l2Ttl),
        TwoTierCacheProperties.twoTier(MasterCodeCacheNames.CHILDREN, l1Ttl, 100, l2Ttl)
    );
  }
}
