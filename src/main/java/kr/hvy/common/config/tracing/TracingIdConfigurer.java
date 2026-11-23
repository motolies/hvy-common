package kr.hvy.common.config.tracing;

import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.SdkTracerProviderBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry traceId 생성기를 {@link EpochPrefixedIdGenerator} 로 교체하는 설정.
 *
 * <p>Boot 자동설정({@code OpenTelemetryTracingAutoConfiguration})이 만드는 SdkTracerProvider 에
 * 커스터마이저로만 끼워 넣으므로 샘플러·리소스·span limit 등 나머지 자동설정은 그대로 유지된다.
 * 소비 모듈이 {@code kr.hvy.common} 을 컴포넌트 스캔하면 별도 등록 없이 적용된다.
 *
 * <p>기본 활성. 문제 시 {@code hvy.tracing.epoch-prefixed-trace-id.enabled=false}
 * (env {@code HVY_TRACING_EPOCHPREFIXEDTRACEID_ENABLED=false})로 재빌드 없이 기본 랜덤 생성기로 되돌린다.
 */
@Configuration
@ConditionalOnClass(SdkTracerProviderBuilder.class)
@ConditionalOnProperty(
    name = TracingIdConfigurer.ENABLED_PROPERTY,
    havingValue = "true",
    matchIfMissing = true)
public class TracingIdConfigurer {

  static final String ENABLED_PROPERTY = "hvy.tracing.epoch-prefixed-trace-id.enabled";

  /** SdkTracerProvider 빌더에 epoch 접두사 traceId 생성기를 등록한다. */
  @Bean
  public SdkTracerProviderBuilderCustomizer epochPrefixedTraceIdCustomizer() {
    return builder -> builder.setIdGenerator(new EpochPrefixedIdGenerator());
  }
}
