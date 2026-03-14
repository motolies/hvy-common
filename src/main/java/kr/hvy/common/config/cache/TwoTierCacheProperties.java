package kr.hvy.common.config.cache;

import java.time.Duration;

/**
 * 2단계 캐시(L1 Caffeine + L2 Redis) 설정
 */
public record TwoTierCacheProperties(
    String name,
    Duration l1Ttl,
    int l1MaxSize,
    boolean allowNullValues,
    boolean l2Enabled,
    Duration l2Ttl,
    String l2KeyPrefix
) {

  /**
   * L1(Caffeine) 전용 캐시
   */
  public static TwoTierCacheProperties l1Only(String name, Duration l1Ttl, int l1MaxSize, boolean allowNullValues) {
    return new TwoTierCacheProperties(name, l1Ttl, l1MaxSize, allowNullValues, false, Duration.ZERO, "");
  }

  /**
   * L1(Caffeine) + L2(Redis) 2단계 캐시
   */
  public static TwoTierCacheProperties twoTier(String name, Duration l1Ttl, int l1MaxSize, Duration l2Ttl) {
    return new TwoTierCacheProperties(name, l1Ttl, l1MaxSize, false, true, l2Ttl, "cache:" + name);
  }
}
