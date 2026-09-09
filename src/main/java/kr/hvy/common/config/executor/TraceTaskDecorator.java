package kr.hvy.common.config.executor;


import org.jspecify.annotations.NonNull;
import org.springframework.core.task.TaskDecorator;

public class TraceTaskDecorator implements TaskDecorator {

  // JSpecify @NonNull 은 TYPE_USE 애노테이션이므로 반환 타입 바로 앞에 둔다
  @Override
  public @NonNull Runnable decorate(@NonNull Runnable runnable) {
    // 부모 스레드의 Trace/Observation 컨텍스트를 캡처해서 자식 스레드에서 복원(→ Slf4JEventListener 가 MDC 재생성)
    // 팩토리는 TraceExecutors 와 공유한다 — 매 태스크마다 builder 를 새로 돌 이유가 없다
    return TraceExecutors.SNAPSHOT.captureAll().wrap(runnable);
  }
}
