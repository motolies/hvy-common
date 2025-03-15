package kr.hvy.common.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;

@Slf4j
public class DefaultDeadLetterPublishingRecoverer implements SimpleDeadLetterPublishingRecoverer {

  @Override
  public void addHookExceptionHeadersCreator(Headers headers, Exception exception) {
    log.debug("Add exception hook headers creator");
  }
}
