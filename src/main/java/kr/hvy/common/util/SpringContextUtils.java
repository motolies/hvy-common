package kr.hvy.common.util;

import java.util.Optional;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContextUtils implements ApplicationContextAware {

  private static ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    context = applicationContext;
  }

  public static <T> Optional<T> getBean(Class<T> beanClass) {
    return Optional.ofNullable(context.getBean(beanClass));
  }


}
