package kr.hvy.common.aop.log.service;

import kr.hvy.common.aop.log.dto.SystemLogCreate;
import kr.hvy.common.aop.log.model.SystemLogEntity;
import kr.hvy.common.domain.mapper.SystemLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemLogService {

  private final SystemLogMapper systemLogMapper;
  private final SystemLogRepository systemLogRepository;

  @Async
  public void save(SystemLogCreate systemLogCreate) {
    SystemLogEntity systemLogEntity = systemLogMapper.toEntity(systemLogCreate);
    systemLogRepository.save(systemLogEntity);
  }

}
