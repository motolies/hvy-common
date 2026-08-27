package kr.hvy.common.config.executor;


import io.micrometer.context.ContextSnapshotFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.core.task.TaskDecorator;

public class TraceTaskDecorator implements TaskDecorator {

  // JSpecify @NonNull 은 TYPE_USE 애노테이션이므로 반환 타입 바로 앞에 둔다
  @Override
  public @NonNull Runnable decorate(@NonNull Runnable runnable) {
    // 부모 스레드의 Trace/MDC/Observation 컨텍스트를 캡처해서 자식 스레드에서 복원
    return ContextSnapshotFactory.builder().build().captureAll().wrap(runnable);
  }
}
