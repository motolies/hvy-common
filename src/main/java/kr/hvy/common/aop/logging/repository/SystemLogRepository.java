package kr.hvy.common.aop.logging.repository;

import java.time.LocalDateTime;
import kr.hvy.common.aop.logging.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

  @Modifying
  int deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}