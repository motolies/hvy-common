package kr.hvy.common.config.executor;

import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextScheduledExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 스레드 풀을 만드는 유일한 정문. 여기를 거치면 traceId/spanId 전파를 잊을 수 없다.
 * <p>
 * <b>왜 {@code ContextExecutorService} 가 {@code TaskDecorator} 보다 나은가</b> —
 * {@code TaskDecorator} 는 실행기가 태스크를 감쌀 때(=구현체에 따라 등록 시점 1회일 수 있다) 스냅샷을 뜨지만,
 * {@link ContextExecutorService} 는 <b>submit/execute 호출 시점마다</b> 새로 스냅샷을 뜬다.
 * 따라서 풀을 오래 재사용해도 항상 "지금 호출한 스레드"의 컨텍스트가 따라간다.
 * <p>
 * 자체 풀이 필요하면 {@code Executors.new*} 대신 이 클래스를 쓴다.
 */
public final class TraceExecutors {

  /**
   * 스냅샷 팩토리는 상태가 없고 스레드 안전하므로 하나만 만들어 {@link TraceTaskDecorator} 와 공유한다.
   * (매 태스크마다 builder 를 새로 도는 비용 제거)
   */
  static final ContextSnapshotFactory SNAPSHOT = ContextSnapshotFactory.builder().build();

  private TraceExecutors() {
  }

  /**
   * 이름 규칙이 있는 가상 스레드 풀을 만들고 컨텍스트 전파를 씌운다.
   * <p>
   * 반환값은 {@code ExecutorService} 이므로 try-with-resources 로 쓸 수 있다
   * ({@code close()} 기본 구현이 shutdown + awaitTermination 을 위임 호출한다).
   *
   * @param threadNamePrefix 스레드 이름 접두사 (예: {@code "kis-target-"} → {@code kis-target-0, kis-target-1 ...})
   */
  public static ExecutorService virtualThreadPerTask(String threadNamePrefix) {
    return wrap(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(threadNamePrefix, 0).factory()));
  }

  /**
   * 이미 만들어진 실행기에 컨텍스트 전파를 씌운다.
   */
  public static ExecutorService wrap(ExecutorService delegate) {
    return ContextExecutorService.wrap(delegate, SNAPSHOT);
  }

  /**
   * 스케줄 실행기용 오버로드. 정적 타입이 {@code ScheduledExecutorService} 여야 이 쪽이 선택되므로,
   * 변수 타입을 {@code ExecutorService} 로 좁혀 두면 위 오버로드가 잡혀 schedule* 메서드가 전파되지 않는다.
   */
  public static ScheduledExecutorService wrap(ScheduledExecutorService delegate) {
    return ContextScheduledExecutorService.wrap(delegate, SNAPSHOT);
  }

  /**
   * 실행기를 바꿀 수 없는 자리(예: 외부 API 가 Runnable 만 받는 경우)에서 현재 컨텍스트를 태스크에 박아둔다.
   * 호출 시점에 스냅샷을 뜨므로 <b>태스크를 넘기기 직전</b>에 호출해야 한다.
   */
  public static Runnable capture(Runnable task) {
    return SNAPSHOT.captureAll().wrap(task);
  }

  /**
   * {@link #capture(Runnable)} 의 Callable 판.
   */
  public static <T> Callable<T> capture(Callable<T> task) {
    return SNAPSHOT.captureAll().wrap(task);
  }
}
