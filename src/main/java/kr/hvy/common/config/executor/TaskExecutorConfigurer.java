package kr.hvy.common.config.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

public class TaskExecutorConfigurer {

  /**
   * Task 실행기 빈에 {@link TraceTaskDecorator} 를 자동 부착하는 BeanPostProcessor 를 만든다.
   * <p>
   * 소비 모듈에서 {@code static @Bean} 으로 등록해야 한다 — BeanPostProcessor 는 컨테이너 초기화 극초기에 필요하고,
   * static 이 아니면 이 설정 클래스 전체가 조기 초기화되어 다른 빈의 후처리를 놓칠 수 있다.
   */
  public static BeanPostProcessor traceTaskDecoratorBeanPostProcessor() {
    return new TraceTaskDecoratorBeanPostProcessor();
  }

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
