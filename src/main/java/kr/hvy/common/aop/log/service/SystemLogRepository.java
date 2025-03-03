package kr.hvy.common.aop.log.service;

import kr.hvy.common.aop.log.model.SystemLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemLogRepository extends JpaRepository<SystemLogEntity, Long> {

}