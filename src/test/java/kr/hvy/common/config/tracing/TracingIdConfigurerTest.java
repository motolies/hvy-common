package kr.hvy.common.config.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.SdkTracerProviderBuilderCustomizer;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Boot 자동설정 SdkTracerProvider 에 epoch 접두사 생성기가 커스터마이저로 끼워지는지 검증한다.
 */
class TracingIdConfigurerTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(
          OpenTelemetrySdkAutoConfiguration.class, OpenTelemetryTracingAutoConfiguration.class))
      .withUserConfiguration(TracingIdConfigurer.class);

  @Test
  @DisplayName("기본 설정에서 자동설정 SdkTracerProvider 가 epoch 접두사 traceId 와 random 플래그를 갖는 span 을 만든다")
  void appliesEpochPrefixedIdGeneratorToAutoConfiguredTracerProvider() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(SdkTracerProviderBuilderCustomizer.class);

      long before = System.currentTimeMillis() / 1000;
      Span span = context.getBean(SdkTracerProvider.class).get("test").spanBuilder("test").startSpan();
      long after = System.currentTimeMillis() / 1000;
      span.end();

      String traceId = span.getSpanContext().getTraceId();
      assertThat(Long.parseLong(traceId.substring(0, 8), 16)).isBetween(before, after);
      // generatesRandomTraceIds()=true 가 루트 span traceflags 의 random 비트로 이어지는지
      assertThat(span.getSpanContext().getTraceFlags().isTraceIdRandom()).isTrue();
    });
  }

  @Test
  @DisplayName("옵트아웃 속성을 끄면 커스터마이저가 등록되지 않는다")
  void canBeDisabledByProperty() {
    contextRunner
        .withPropertyValues(TracingIdConfigurer.ENABLED_PROPERTY + "=false")
        .run(context -> assertThat(context).doesNotHaveBean(SdkTracerProviderBuilderCustomizer.class));
  }
}
