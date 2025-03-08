package kr.hvy.common.util;

import java.util.Arrays;
import java.util.Optional;
import static java.util.stream.Collectors.joining;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextUtils implements ApplicationContextAware {

  private static ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    context = applicationContext;
  }

  public static <T> Optional<T> getBean(Class<T> beanClass) {
    return Optional.ofNullable(context.getBean(beanClass));
  }

  public static String getActiveProfile() {
    String[] profiles = context.getEnvironment().getActiveProfiles();
    if (ArrayUtils.isEmpty(profiles)) {
      profiles = context.getEnvironment().getDefaultProfiles();
    }
    return Arrays.stream(profiles).collect(joining(",", "[", "]"));
  }

  public static String getDeploymentProfile() {
    return Arrays.stream(context.getEnvironment().getDefaultProfiles()).collect(joining(",", "[", "]"));
  }

}