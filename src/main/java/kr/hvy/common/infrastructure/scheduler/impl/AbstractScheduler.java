package kr.hvy.common.infrastructure.scheduler.impl;

import brave.Span;
import brave.Tracer;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@Slf4j
public abstract class AbstractScheduler {

  // brave.Tracer — 하위 스케줄러 생성자 변경을 피하기 위해 추상 베이스에 필드 주입
  @Autowired
  private Tracer tracer;

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
      // 스케줄러 경계에서 새 trace span을 생성 → 스케줄러 로그(%X{traceId})와 Slack 오류 알림이 동일 traceId 공유
      Span span = tracer.nextSpan().name("scheduler." + logMsg);
      try (Tracer.SpanInScope ws = tracer.withSpanInScope(span.start())) {
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
      } finally {
        span.finish();
      }
    };
  }

}
