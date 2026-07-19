package kr.hvy.common.infrastructure.scheduler.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class SchedulerConfigurer {

  /**
   * usingDbTime(): 잠금 시각을 앱 JVM 시계가 아닌 DB 서버 시간 기준으로 기록해
   * 다중 인스턴스 간 시계/타임존 불일치로 인한 잠금 오동작을 방지한다.
   */
  protected LockProvider lockProviderByJdbc(DataSource dataSource) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new org.springframework.jdbc.core.JdbcTemplate(dataSource))
            .usingDbTime()
            .build());
  }
}