package kr.hvy.common.aop.logging.repository;

import java.time.LocalDateTime;
import kr.hvy.common.aop.logging.entity.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {

  @Modifying
  @Query("DELETE FROM ApiLog a WHERE a.created.at < :cutoffDate")
  int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}