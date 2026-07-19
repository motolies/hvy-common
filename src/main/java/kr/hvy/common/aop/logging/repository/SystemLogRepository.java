package kr.hvy.common.aop.logging.repository;

import java.time.Instant;
import kr.hvy.common.aop.logging.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

  @Modifying
  @Query("DELETE FROM SystemLog s WHERE s.created.at < :cutoffDate")
  int deleteByCreatedAtBefore(@Param("cutoffDate") Instant cutoffDate);
}