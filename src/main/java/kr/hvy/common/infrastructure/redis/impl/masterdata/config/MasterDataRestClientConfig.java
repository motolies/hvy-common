package kr.hvy.common.infrastructure.redis.impl.masterdata.config;

import kr.hvy.common.infrastructure.redis.impl.masterdata.query.MasterCodeClientProperties;
import kr.hvy.common.infrastructure.redis.impl.masterdata.query.MasterCodeLoader;
import kr.hvy.common.infrastructure.redis.impl.masterdata.query.RestClientMasterCodeLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 소비 앱에서 REST 기반 {@link MasterCodeLoader} 를 빈으로 등록하기 위한 옵트인 Config.
 * <p>
 * 사용법:
 * <pre>
 * {@code
 * @Configuration
 * @Import({MasterDataCommonConfig.class, MasterDataRestClientConfig.class})
 * public class MasterDataConfig { }
 * }
 * </pre>
 * <p>
 * {@code hvy.masterdata.client.base-url} 속성이 설정되어야만 {@link RestClientMasterCodeLoader} 가 등록된다.
 * blog-back(원본 소유자)은 이 Config 를 import 하지 않는다.
 */
@Configuration
@EnableConfigurationProperties(MasterCodeClientProperties.class)
@ConditionalOnProperty(prefix = "hvy.masterdata.client", name = "base-url")
public class MasterDataRestClientConfig {

  @Bean
  @ConditionalOnMissingBean(MasterCodeLoader.class)
  public MasterCodeLoader restClientMasterCodeLoader(MasterCodeClientProperties properties) {
    return new RestClientMasterCodeLoader(properties);
  }
}
