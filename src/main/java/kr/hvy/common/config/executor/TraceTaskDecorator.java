package kr.hvy.common.config.executor;


import io.micrometer.context.ContextSnapshot;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

@Component
public class TraceTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 부모 스레드의 Trace/MDC/Observation 컨텍스트를 캡처해서 자식 스레드에서 복원
        return ContextSnapshot.captureAll().wrap(runnable);
    }
}
