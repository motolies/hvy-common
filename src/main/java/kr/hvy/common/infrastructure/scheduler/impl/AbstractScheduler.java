package kr.hvy.common.infrastructure.scheduler.impl;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
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

  // 벤더 중립 micrometer ObservationRegistry — 하위 스케줄러 생성자 변경을 피하기 위해 추상 베이스에 필드 주입
  @Autowired
  private ObservationRegistry observationRegistry;

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

  /**
   * 스케줄러 본문을 트레이스 경계로 감싼다 → 스케줄러 로그(%X{traceId})와 Slack 오류 알림이 동일 traceId 를 공유한다.
   * <p>
   * raw span(tracer.nextSpan + withSpan)이 아니라 Observation 으로 여는 이유:
   * ServiceLoader 로 자동 등록되는 accessor 는 ObservationThreadLocalAccessor 하나뿐이라,
   * Observation 이어야 이 경계 안에서 파생되는 자식 스레드(비동기 실행기)까지 traceId 가 따라간다.
   */
  protected Consumer<Runnable> proceedScheduler(String logMsg) {
    return schedulerRunnable -> Observation.createNotStarted("scheduler." + logMsg, observationRegistry)
        .observe(() -> {
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
        });
  }

}
