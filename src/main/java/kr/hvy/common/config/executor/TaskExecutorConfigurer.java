package kr.hvy.common.config.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

public class TaskExecutorConfigurer {

  protected TaskExecutor virtualThreadExecutor(ExecutorService executorService) {
    ConcurrentTaskExecutor exec = new ConcurrentTaskExecutor(executorService);
    exec.setTaskDecorator(new TraceTaskDecorator()); // Micrometer 컨텍스트 전파
    return exec;
  }

  protected ExecutorService vtExecutorService(String threadNamePrefix) {
    return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(threadNamePrefix, 0).factory());
  }

  protected ExecutorService vtExecutorService() {
    return vtExecutorService("async-vt-");
  }

}
