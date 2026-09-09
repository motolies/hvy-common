package kr.hvy.common.config.executor;

import io.micrometer.context.ContextRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

/**
 * 수동 span(tracer.nextSpan/withSpan) 도 자식 스레드로 전파되도록 ThreadLocalAccessor 를 수동 등록한다.
 * <p>
 * <b>왜 수동 등록이 필요한가</b> — {@code micrometer-observation.jar} 만
 * {@code META-INF/services/io.micrometer.context.ThreadLocalAccessor} 를 가지고 있어
 * {@link ObservationThreadLocalAccessor} 만 ServiceLoader 로 자동 등록된다.
 * {@code micrometer-tracing.jar} 에는 그 services 엔트리가 아예 없고 Spring Boot auto-config 도 등록하지 않는다.
 * 그래서 Observation 없이 raw span 만 연 경계에서는 {@code ContextSnapshot.captureAll()} 이 캡처할 값이 없어
 * 자식 스레드의 MDC(traceId/spanId)가 완전히 비어버린다.
 * <p>
 * <b>등록 순서</b> — {@link ObservationAwareSpanThreadLocalAccessor} javadoc 은
 * "반드시 ObservationThreadLocalAccessor 뒤에 등록하라"고 요구한다.
 * {@link ContextRegistry#getInstance()} 의 정적 초기화가 ServiceLoader 를 먼저 돌리므로
 * 이 시점의 등록은 항상 뒤가 되어 요구사항이 자동으로 충족된다.
 * <p>
 * 또한 {@code registerThreadLocalAccessor} 는 같은 key 의 기존 accessor 를 먼저 제거하므로 여러 번 호출해도 멱등하다.
 */
@Slf4j
public class ContextPropagationConfigurer implements InitializingBean {

  private final ObservationRegistry observationRegistry;
  private final Tracer tracer;

  public ContextPropagationConfigurer(ObservationRegistry observationRegistry, Tracer tracer) {
    this.observationRegistry = observationRegistry;
    this.tracer = tracer;
  }

  @Override
  public void afterPropertiesSet() {
    ContextRegistry.getInstance()
        .registerThreadLocalAccessor(new ObservationAwareSpanThreadLocalAccessor(observationRegistry, tracer));
    log.info("ThreadLocalAccessor registered: {}", ObservationAwareSpanThreadLocalAccessor.KEY);
  }
}
