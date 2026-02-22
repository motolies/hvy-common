package kr.hvy.common.core.util;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
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

  public static ApplicationEventPublisher getEventPublisher() {
    return context;
  }

  public static String getActiveProfile() {
    Set<String> profiles = getProfiles();
    return getProfiles().stream().collect(joining(",", "[", "]"));
  }

  public static boolean getActiveProfile(String profile) {
    return getProfiles().stream().anyMatch(p -> p.equals(profile));
  }

  public static boolean isDefaultProfileOnly() {
    Set<String> profiles = getProfiles();
    return profiles.size() == 1 && profiles.contains("default");
  }

  private static Set<String> getProfiles() {
    String[] profiles = context.getEnvironment().getActiveProfiles();
    if (ArrayUtils.isEmpty(profiles)) {
      profiles = context.getEnvironment().getDefaultProfiles();
    }
    return Set.of(profiles);
  }

  public static String getDeploymentProfile() {
    return Arrays.stream(context.getEnvironment().getDefaultProfiles()).collect(joining(",", "[", "]"));
  }

}