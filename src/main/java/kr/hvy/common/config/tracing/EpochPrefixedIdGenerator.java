package kr.hvy.common.config.tracing;

import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/**
 * 상위 4바이트에 epoch 초를 넣는 OpenTelemetry traceId 생성기.
 *
 * <p>형식: {@code [epoch 초 32비트][랜덤 32비트][랜덤 64비트]} = 128비트(32 hex).
 * traceId 앞 8자리만으로 발생 시각을 역산할 수 있어 로그 조회 시간 창을 좁힐 수 있다
 * (예: {@code 6955b900} → {@code date -r $((16#6955b900))} = 2026-01-01 00:00:00 UTC).
 *
 * <p>같은 초에 생성된 trace 는 앞 8자리가 같으므로 비교는 반드시 32자리 전체로 한다.
 * 샘플링({@code TraceIdRatioBased})은 하위 64비트로 판정하므로 영향이 없다.
 */
public final class EpochPrefixedIdGenerator implements IdGenerator {

  private static final IdGenerator RANDOM = IdGenerator.random();

  private final LongSupplier epochSecondsSupplier;

  /** 현재 시각(epoch 초)을 접두사로 쓰는 기본 생성자. */
  public EpochPrefixedIdGenerator() {
    this(() -> System.currentTimeMillis() / 1000);
  }

  /** 접두사 시각 공급자를 주입한다(테스트에서 고정 시각 검증용). */
  EpochPrefixedIdGenerator(LongSupplier epochSecondsSupplier) {
    this.epochSecondsSupplier = epochSecondsSupplier;
  }

  /** spanId 는 기본 생성기에 위임해 완전 랜덤 64비트를 유지한다. */
  @Override
  public String generateSpanId() {
    return RANDOM.generateSpanId();
  }

  /**
   * 상위 32비트=epoch 초, 나머지 96비트=랜덤인 traceId 를 만든다.
   * 상위 32비트가 epoch 초라 전부 0인 무효 traceId 는 생기지 않는다.
   */
  @Override
  public String generateTraceId() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    long epochSeconds = epochSecondsSupplier.getAsLong();
    long high = (epochSeconds << 32) | (random.nextInt() & 0xFFFFFFFFL);
    return TraceId.fromLongs(high, random.nextLong());
  }

  /**
   * W3C Trace Context Level 2 의 random-trace-id 플래그 대상임을 SDK 에 알린다.
   *
   * <p>계약은 "하위 56비트가 균일 랜덤"이고 이 생성기는 하위 96비트가 랜덤이라 충족한다.
   * 기본 생성기({@link IdGenerator#random()})도 {@code true} 를 반환하므로, 교체 전과 같은
   * traceflags(random 비트)를 유지하려면 재정의가 필요하다(기본 구현은 {@code false}).
   */
  @Override
  public boolean generatesRandomTraceIds() {
    return true;
  }
}
