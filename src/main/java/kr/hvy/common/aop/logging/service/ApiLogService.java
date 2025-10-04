package kr.hvy.common.aop.logging.service;

import java.time.LocalDateTime;
import kr.hvy.common.aop.logging.dto.ApiLogCreate;
import kr.hvy.common.aop.logging.entity.ApiLog;
import kr.hvy.common.aop.logging.repository.ApiLogRepository;
import kr.hvy.common.application.domain.mapper.ApiLogDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiLogService {

  private final ApiLogDtoMapper apiLogDtoMapper;
  private final ApiLogRepository apiLogRepository;

  @Async
  public void save(ApiLogCreate apiLogCreate) {
    ApiLog systemLogEntity = apiLogDtoMapper.toEntity(apiLogCreate);
    apiLogRepository.save(systemLogEntity);
  }

  @Transactional
  public int deleteLogsOlderThan(LocalDateTime cutoffDate) {
    return apiLogRepository.deleteByCreatedAtBefore(cutoffDate);
  }

}
