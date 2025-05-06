package kr.hvy.common.aop.log.service;

import kr.hvy.common.aop.log.model.SystemLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;

public interface SystemLogRepository extends JpaRepository<SystemLogEntity, Long> {

    @Modifying
    int deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}