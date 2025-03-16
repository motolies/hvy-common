package kr.hvy.common.db.log;

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

}
