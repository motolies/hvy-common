package kr.hvy.common.infrastructure.redis.impl.masterdata.query;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link RestClientMasterCodeLoader} 용 properties.
 *
 * <pre>
 * hvy:
 *   masterdata:
 *     client:
 *       base-url: https://blog-back.internal   # 필수
 *       timeout: 2s                             # 선택, 기본 2초
 *       api-key: xxx                            # 선택, Authorization 헤더로 전달
 * </pre>
 */
@ConfigurationProperties(prefix = "hvy.masterdata.client")
public record MasterCodeClientProperties(
    String baseUrl,
    Duration timeout,
    String apiKey
) {

  public MasterCodeClientProperties {
    if (timeout == null) {
      timeout = Duration.ofSeconds(2);
    }
  }
}
