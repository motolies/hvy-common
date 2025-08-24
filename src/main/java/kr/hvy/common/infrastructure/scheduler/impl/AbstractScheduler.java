package kr.hvy.common.infrastructure.scheduler.impl;

import java.util.Objects;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@Slf4j
public abstract class AbstractScheduler {

  protected void setupToken() {
    UserDetails userDetails = User.builder()
        .username("SCHEDULER")
        .password("SCHEDULER_PASSWORD")
        .roles("SCHEDULER", "ADMIN")
        .build();

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  protected Consumer<Runnable> proceedScheduler(String logMsg) {
    return schedulerRunnable -> {
      StopWatch stopWatch = StopWatch.createStarted();
      log.info("### Scheduler." + logMsg + " start ###");
      setupToken();

      try {
        if (Objects.nonNull(schedulerRunnable)) {
          schedulerRunnable.run();
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }

      stopWatch.stop();
      log.info("### Scheduler." + logMsg + " end : {} sec ###", stopWatch);
    };
  }

}
