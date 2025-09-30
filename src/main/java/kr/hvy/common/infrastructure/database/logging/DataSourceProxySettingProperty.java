package kr.hvy.common.infrastructure.database.logging;

import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "hvy.sql.datasource-wrapper")
public class DataSourceProxySettingProperty {

  private boolean enableWrapper;
  private boolean format;
  private Set<String> dataSourceNames;
  // 슬로우 쿼리 임계치 (ms)
  private long slowQueryThreshold = 1000; // 예: 1000ms 이상이면 slow query로 간주

  // ResultSet 로깅 설정
  private boolean enableResultSetLogging = false; // ResultSet 테이블 로깅 활성화
  private int maxResultSetRows = Integer.MAX_VALUE; // 최대 표시 행 수 (무제한)
  private int maxResultSetColumns = Integer.MAX_VALUE; // 최대 표시 컬럼 수 (무제한)
  private int maxColumnValueLength = Integer.MAX_VALUE; // 컬럼 값 최대 표시 길이 (무제한)

}
