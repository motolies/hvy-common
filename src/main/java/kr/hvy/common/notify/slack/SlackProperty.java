package kr.hvy.common.notify.slack;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "slack")
public class SlackProperty {

  private String token;
}
