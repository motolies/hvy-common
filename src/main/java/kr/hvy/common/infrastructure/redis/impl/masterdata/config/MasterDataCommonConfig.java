package kr.hvy.common.infrastructure.redis.impl.masterdata.config;

import kr.hvy.common.infrastructure.redis.impl.masterdata.cache.MasterCodeCacheService;
import kr.hvy.common.infrastructure.redis.impl.masterdata.query.MasterCodeLoader;
import kr.hvy.common.infrastructure.redis.impl.masterdata.query.MasterCodeQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 마스터코드 캐시 공통 Configuration.
 * <p>
 * hvy-common 은 Spring Boot auto-configuration 등록 없이 <b>명시적 {@code @Import}</b> 로 사용한다.
 * 기존 {@code RedisConfigurer}, {@code TwoTierCacheConfigurer} 패턴과 동일.
 * <p>
 * 소유자(blog-back) 또는 소비 앱의 설정 클래스에서 다음과 같이 활성화:
 * <pre>
 * {@code
 * @Configuration
 * @Import(MasterDataCommonConfig.class)
 * public class MasterDataConfig {
 *   @Bean
 *   public MasterCodeLoader masterCodeLoader() { ... }   // Jpa or RestClient
 * }
 * }
 * </pre>
 * <p>
 * 이 Config 는 {@link MasterCodeCacheService} 와 {@link MasterCodeQuery} 빈을 자동 등록한다.
 * {@link MasterCodeLoader} 구현체는 소비 측에서 직접 제공해야 한다.
 */
@Configuration
public class MasterDataCommonConfig {

  /**
   * {@link CacheManager} 는 소비 측에서 반드시 먼저 등록되어 있어야 한다(대개 {@code TwoTierCacheConfigurer} 상속).
   * 소비 측이 동일 타입 빈을 직접 제공한 경우엔 override 하지 않는다.
   * <p>
   * {@code @ConditionalOnBean} 은 일반 {@code @Configuration} 에서 순서 의존적이라 사용하지 않는다.
   * {@code CacheManager} 가 없으면 Spring 이 생성자 주입 실패로 명확히 알려준다.
   */
  @Bean
  @ConditionalOnMissingBean
  public MasterCodeCacheService masterCodeCacheService(CacheManager cacheManager) {
    return new MasterCodeCacheService(cacheManager);
  }

  /**
   * {@link MasterCodeLoader} 는 소비 측(blog-back 의 JpaLoader 또는 소비 앱의 RestClientLoader) 이 빈으로 제공해야 한다.
   */
  @Bean
  @ConditionalOnMissingBean
  public MasterCodeQuery masterCodeQuery(MasterCodeCacheService cache, MasterCodeLoader loader) {
    return new MasterCodeQuery(cache, loader);
  }
}
