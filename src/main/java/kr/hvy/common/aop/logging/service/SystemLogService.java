package kr.hvy.common.aop.logging.service;

import java.time.LocalDateTime;
import kr.hvy.common.aop.logging.dto.SystemLogCreate;
import kr.hvy.common.aop.logging.entity.SystemLog;
import kr.hvy.common.application.domain.mapper.SystemLogDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemLogService {

  private final SystemLogDtoMapper systemLogDtoMapper;
  private final SystemLogRepository systemLogRepository;

  @Async
  public void save(SystemLogCreate systemLogCreate) {
    SystemLog systemLog = systemLogDtoMapper.toEntity(systemLogCreate);
    systemLogRepository.save(systemLog);
  }

  @Transactional
  public int deleteLogsOlderThan(LocalDateTime cutoffDate) {
    return systemLogRepository.deleteByCreatedAtBefore(cutoffDate);
  }
}
