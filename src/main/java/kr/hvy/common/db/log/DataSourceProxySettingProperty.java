package kr.hvy.common.db.log;

import java.util.Set;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "datasource-wrapper")
public class DataSourceProxySettingProperty {

  private boolean wrap;
  private boolean format;
  private Set<String> names;

}
