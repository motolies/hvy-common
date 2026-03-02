package kr.hvy.common.aop.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hvy.common.aop.logging.dto.SystemLogCreate;
import kr.hvy.common.aop.logging.service.SystemLogService;
import kr.hvy.common.core.code.ApiResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemLogAsyncService {

  private final ObjectMapper objectMapper;
  private final SystemLogService systemLogService;

  @Async
  public void saveAsync(SystemLogCreate.SystemLogCreateBuilder builder, Object result) {
    if (result instanceof Throwable e) {
      builder.status(ApiResponseStatus.FAIL)
          .stackTrace(ExceptionUtils.getStackTrace(e));
    } else {
      builder.responseBody(writeValueAsString(result));
    }
    systemLogService.save(builder.build());
  }

  private String writeValueAsString(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      log.error("SystemLogAsyncSaver: {}", e.getMessage(), e);
      return "Can not converter data.";
    }
  }

}
