package kr.hvy.common.config.executor;


import io.micrometer.context.ContextSnapshotFactory;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

public class TraceTaskDecorator implements TaskDecorator {

  @Override
  @NonNull
  public Runnable decorate(@NonNull Runnable runnable) {
    // 부모 스레드의 Trace/MDC/Observation 컨텍스트를 캡처해서 자식 스레드에서 복원
    return ContextSnapshotFactory.builder().build().captureAll().wrap(runnable);
  }
}
