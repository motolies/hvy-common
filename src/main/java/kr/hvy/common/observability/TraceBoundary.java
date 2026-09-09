package kr.hvy.common.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.function.Supplier;

/**
 * 트레이스 경계를 여는 단일 프리미티브.
 * <p>
 * HTTP 요청·{@code @Scheduled} 처럼 프레임워크가 경계를 만들어 주지 않는 진입점
 * (Redisson Netty 콜백, {@code ApplicationReadyEvent} 리스너, 이벤트 컨슈머 등)에서
 * 이 클래스로 감싸면 그 안의 모든 로그가 traceId/spanId 를 갖는다.
 * <p>
 * <b>raw span({@code tracer.nextSpan}) 이 아니라 Observation 인 이유</b> —
 * ServiceLoader 로 자동 등록되는 accessor 는 {@code ObservationThreadLocalAccessor} 하나뿐이다.
 * Observation 으로 열어야 그 accessor 가 값을 잡아,
 * 이 경계 안에서 파생되는 자식 스레드까지 traceId 가 따라간다.
 * (raw span 은 {@code ContextPropagationConfigurer} 의 수동 accessor 등록이 있어야만 전파된다.)
 */
public class TraceBoundary {

  private final ObservationRegistry observationRegistry;

  public TraceBoundary(ObservationRegistry observationRegistry) {
    this.observationRegistry = observationRegistry;
  }

  /**
   * 새 Observation 경계 안에서 작업을 실행한다. 예외는 Observation 에 기록된 뒤 그대로 전파된다.
   *
   * @param name Observation 이름 (예: {@code "cache.invalidation"})
   */
  public void run(String name, Runnable body) {
    Observation.createNotStarted(name, observationRegistry).observe(body);
  }

  /**
   * 반환값이 있는 {@link #run(String, Runnable)}.
   */
  public <T> T call(String name, Supplier<T> body) {
    return Observation.createNotStarted(name, observationRegistry).observe(body);
  }
}
