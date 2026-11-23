package kr.hvy.common.config.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * traceId 상위 32비트에 epoch 초를 넣는 생성기의 형식·유효성·고유성을 검증한다.
 */
class EpochPrefixedIdGeneratorTest {

  /** 2026-01-01T00:00:00Z = 0x6955b900. */
  private static final long FIXED_EPOCH_SECONDS = Instant.parse("2026-01-01T00:00:00Z").getEpochSecond();
  private static final String FIXED_PREFIX = "6955b900";

  @Test
  @DisplayName("주입한 epoch 초가 traceId 앞 8자리로 그대로 들어간다")
  void prefixesTraceIdWithEpochSeconds() {
    EpochPrefixedIdGenerator generator = new EpochPrefixedIdGenerator(() -> FIXED_EPOCH_SECONDS);

    String traceId = generator.generateTraceId();

    assertThat(traceId).startsWith(FIXED_PREFIX);
  }

  @Test
  @DisplayName("traceId·spanId 가 W3C 규격상 유효한 값이다")
  void generatesValidIds() {
    EpochPrefixedIdGenerator generator = new EpochPrefixedIdGenerator();

    String traceId = generator.generateTraceId();
    String spanId = generator.generateSpanId();

    assertThat(traceId).hasSize(TraceId.getLength()).matches("[0-9a-f]{32}");
    assertThat(TraceId.isValid(traceId)).isTrue();
    assertThat(SpanId.isValid(spanId)).isTrue();
  }

  @Test
  @DisplayName("같은 초에 생성해도 나머지 96비트가 랜덤이라 traceId 가 겹치지 않는다")
  void generatesDistinctTraceIdsWithinSameSecond() {
    EpochPrefixedIdGenerator generator = new EpochPrefixedIdGenerator(() -> FIXED_EPOCH_SECONDS);
    Set<String> traceIds = new HashSet<>();

    for (int i = 0; i < 10_000; i++) {
      traceIds.add(generator.generateTraceId());
    }

    assertThat(traceIds).hasSize(10_000).allMatch(id -> id.startsWith(FIXED_PREFIX));
  }

  @Test
  @DisplayName("기본 생성자는 현재 시각(epoch 초)을 접두사로 쓴다")
  void defaultConstructorUsesCurrentEpochSeconds() {
    long before = System.currentTimeMillis() / 1000;
    String traceId = new EpochPrefixedIdGenerator().generateTraceId();
    long after = System.currentTimeMillis() / 1000;

    long prefixSeconds = Long.parseLong(traceId.substring(0, 8), 16);

    assertThat(prefixSeconds).isBetween(before, after);
  }

  @Test
  @DisplayName("하위 96비트가 랜덤이므로 random-trace-id 플래그 대상임을 선언한다")
  void declaresRandomTraceIds() {
    assertThat(new EpochPrefixedIdGenerator().generatesRandomTraceIds()).isTrue();
  }
}
