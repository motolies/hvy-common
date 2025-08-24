package kr.hvy.common.infrastructure.scheduler.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class SchedulerConfigurer {

  protected LockProvider lockProviderByJdbc(DataSource dataSource) {
    return new JdbcTemplateLockProvider(dataSource);
  }
}