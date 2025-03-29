package kr.hvy.common.aop.log.service;

import kr.hvy.common.aop.log.dto.ApiLogCreate;
import kr.hvy.common.aop.log.model.ApiLogEntity;
import kr.hvy.common.domain.mapper.ApiLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiLogService {

  private final ApiLogMapper apiLogMapper;
  private final ApiLogRepository apiLogRepository;

  @Async
  public void save(ApiLogCreate apiLogCreate) {
    ApiLogEntity systemLogEntity = apiLogMapper.toEntity(apiLogCreate);
    apiLogRepository.save(systemLogEntity);
  }

}
