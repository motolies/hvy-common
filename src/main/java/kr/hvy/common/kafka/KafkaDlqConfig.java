package kr.hvy.common.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaDlqConfig {

  @Bean
  @ConditionalOnMissingBean(SimpleDeadLetterPublishingRecoverer.class)
  public SimpleDeadLetterPublishingRecoverer defaultDeadLetterPublishingRecoverer() {
    return new DefaultDeadLetterPublishingRecoverer();
  }

}
