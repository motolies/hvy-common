package kr.hvy.common.kafka;

import org.apache.kafka.common.header.Headers;

public interface SimpleDeadLetterPublishingRecoverer {

  /**
   * 예외 정보를 Header에 넣을 때 비즈니스 로직 추가 훅
   */
  void addHookExceptionHeadersCreator(Headers headers, Exception exception);
}
