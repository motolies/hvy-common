package kr.hvy.common.config.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 컨텍스트에 등록되는 모든 Task 실행기 빈에 {@link TraceTaskDecorator} 를 자동 부착한다.
 * <p>
 * "새 Executor 빈을 만들면서 데코레이터 붙이기를 잊는다"는 실패 모드를 구조적으로 제거하는 것이 목적이다.
 * 이미 데코레이터가 붙어 있는 빈에 다시 부착해도 같은 동작이라 무해하다.
 */
@Slf4j
public class TraceTaskDecoratorBeanPostProcessor implements BeanPostProcessor {

  private final TaskDecorator decorator = new TraceTaskDecorator();

  /**
   * 초기화 전에 데코레이터를 심는다. {@code setTaskDecorator} 는 세 구현체 모두 실행 시점에 필드를 지연 참조하므로
   * {@code initialize()} 이후 호출되어도 안전하다.
   */
  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    // ⚠ TaskScheduler 는 반드시 제외한다.
    //    Spring 7 ThreadPoolTaskScheduler.decorateTaskIfNecessary() 는 스케줄 "등록 시 1회만" decorate() 하고
    //    DelegatingRunnableScheduledFuture.decoratedRunnable 을 주기 실행마다 재사용한다.
    //    → ContextSnapshot 이 기동 시점으로 고정되어 모든 주기 실행이 같은 썩은 traceId 를 갖는 조용한 버그가 된다.
    //    ThreadPoolTaskExecutor 는 매 execute() 마다 필드를 읽으므로 안전하다.
    //    ConcurrentTaskScheduler(extends ConcurrentTaskExecutor)·SimpleAsyncTaskScheduler(extends SimpleAsyncTaskExecutor)
    //    가 아래 분기에 그대로 걸리므로 구체 타입이 아니라 TaskScheduler 인터페이스로 걸러야 한다.
    //    붙일 필요도 없다 — @Scheduled 경계는 Boot 4.1 ScheduledTasksObservationAutoConfiguration 이 이미 Observation 으로 감싼다.
    if (bean instanceof TaskScheduler) {
      return bean;
    }

    if (bean instanceof ThreadPoolTaskExecutor executor) {
      executor.setTaskDecorator(decorator);
    } else if (bean instanceof SimpleAsyncTaskExecutor executor) {
      executor.setTaskDecorator(decorator);
    } else if (bean instanceof ConcurrentTaskExecutor executor) {
      executor.setTaskDecorator(decorator);
    } else {
      return bean;
    }

    log.debug("TraceTaskDecorator attached to executor bean: {}", beanName);
    return bean;
  }
}
