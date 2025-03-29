package kr.hvy.common.aop.log.service;

import kr.hvy.common.aop.log.model.ApiLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiLogRepository extends JpaRepository<ApiLogEntity, Long> {

}