package kr.hvy.common.kafka;

import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.binder.kafka.ListenerContainerWithDlqAndRetryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.lang.Nullable;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaGlobalDlqCustomizerConfig {

  @Value("${spring.application.name:unknown-app}")
  private String appName;
  private final KafkaDlqProperty dlqProperty;
  private final SimpleDeadLetterPublishingRecoverer simpleDeadLetterPublishingRecoverer;

  @Bean
  @Primary
  @ConditionalOnProperty(prefix = "spring.kafka.default-dlq", name = "enableDlq", havingValue = "true")
  public ListenerContainerWithDlqAndRetryCustomizer globalDlqCustomizer(KafkaTemplate<?, ?> kafkaTemplate) {

    /**
     * stream bridge 사용 시 spring:kafka:bootstrap-servers 설정하지 않으면 localhost:9092 로 설정됨
     * 템플릿에서 확인할 수 있음
     * kafkaTemplate.getProducerFactory().getConfigurationProperties()
     */

    return new ListenerContainerWithDlqAndRetryCustomizer() {

      @Override
      public void configure(
          AbstractMessageListenerContainer<?, ?> container,
          String destinationName,
          String group,
          @Nullable BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> dlqDestinationResolver,
          @Nullable BackOff backOff
      ) {

        BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> resolver =
            (rec, exc) -> new TopicPartition(dlqProperty.getDlqName(), rec.partition());

        DeadLetterPublishingRecoverer dlpr = new DeadLetterPublishingRecoverer(kafkaTemplate, resolver) {
          @Override
          protected ProducerRecord<Object, Object> createProducerRecord(ConsumerRecord<?, ?> record,
              TopicPartition topicPartition, Headers headers, @Nullable byte[] key, @Nullable byte[] value) {
            ProducerRecord<Object, Object> producerRecord = super.createProducerRecord(record, topicPartition, headers, key, value);
            producerRecord.headers().add("x-failed-consumer-app", appName.getBytes(StandardCharsets.UTF_8));
            producerRecord.headers().add("x-failed-consumer-group-name", group.getBytes(StandardCharsets.UTF_8));
            return producerRecord;
          }
        };

        // DLQ로 보낼 때 예외 정보 등을 헤더로 추가
        dlpr.setExceptionHeadersCreator((kafkaHeaders, exception, isKey, headerNames) -> {
          kafkaHeaders.add("x-exception-message", ExceptionUtils.getRootCauseMessage(exception).getBytes(StandardCharsets.UTF_8));
          kafkaHeaders.add("x-exception-stacktrace", ExceptionUtils.getStackTrace(exception).getBytes(StandardCharsets.UTF_8));
          // custom hook 추가
          simpleDeadLetterPublishingRecoverer.addHookExceptionHeadersCreator(kafkaHeaders, exception);
        });

        DefaultErrorHandler commonErrorHandler = getDefaultErrorHandler(dlpr);
        container.setCommonErrorHandler(commonErrorHandler);
        commonErrorHandler.setCommitRecovered(true);
      }

      @Override
      public boolean retryAndDlqInBinding(String destinationName, String group) {
        return dlqProperty.getMaxAttempts() > 0;
      }

      private DefaultErrorHandler getDefaultErrorHandler(DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
        return new DefaultErrorHandler(deadLetterPublishingRecoverer, new FixedBackOff(dlqProperty.getRetryInterval(), dlqProperty.getMaxAttempts()));
      }
    };
  }


}