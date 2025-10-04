package kr.hvy.common.aop.logging.repository;

import java.time.LocalDateTime;
import kr.hvy.common.aop.logging.entity.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {

  @Modifying
  int deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}