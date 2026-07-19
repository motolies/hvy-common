package kr.hvy.common.aop.logging.service;

import java.time.Instant;
import kr.hvy.common.aop.logging.dto.SystemLogCreate;
import kr.hvy.common.aop.logging.entity.SystemLog;
import kr.hvy.common.aop.logging.repository.SystemLogRepository;
import kr.hvy.common.application.domain.mapper.SystemLogDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemLogService {


  private final SystemLogDtoMapper systemLogDtoMapper;
  private final SystemLogRepository systemLogRepository;

  public void save(SystemLogCreate systemLogCreate) {
    SystemLog systemLog = systemLogDtoMapper.toEntity(systemLogCreate);
    systemLogRepository.save(systemLog);
  }

  @Transactional
  public int deleteLogsOlderThan(Instant cutoffDate) {
    return systemLogRepository.deleteByCreatedAtBefore(cutoffDate);
  }


}
