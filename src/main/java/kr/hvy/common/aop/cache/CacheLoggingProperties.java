package kr.hvy.common.aop.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 캐시 로깅 설정 속성
 */
@Data
@ConfigurationProperties(prefix = "cache.logging")
public class CacheLoggingProperties {

  /**
   * 캐시 로깅 활성화 여부
   */
  private boolean enabled = false;

  /**
   * HIT 로그 출력 여부 (DEBUG 레벨)
   */
  private boolean logHits = false;

  /**
   * MISS 로그 출력 여부 (INFO 레벨)
   */
  private boolean logMisses = true;

  /**
   * EVICTION 로그 출력 여부 (INFO 레벨)
   */
  private boolean logEvictions = true;
}
