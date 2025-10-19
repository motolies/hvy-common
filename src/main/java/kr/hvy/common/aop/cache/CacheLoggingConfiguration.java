package kr.hvy.common.aop.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 캐시 로깅 설정
 * CacheLoggingProperties를 활성화합니다.
 * Caffeine StatsCounter 기반으로 캐시 HIT/MISS를 정확하게 추적합니다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CacheLoggingProperties.class)
public class CacheLoggingConfiguration {

  @PostConstruct
  public void init() {
    log.info("✅ CacheLoggingConfiguration initialized - StatsCounter-based cache logging is ready");
  }
}
