package kr.hvy.common.aop.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import kr.hvy.common.aop.log.dto.SystemLogCreate;
import kr.hvy.common.aop.log.service.SystemLogService;
import kr.hvy.common.code.ApiResponseStatus;
import kr.hvy.common.domain.vo.EventLog;
import kr.hvy.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SystemLogAspect {

  private final ObjectMapper objectMapper;
  private final SystemLogService systemLogService;

  @Pointcut("execution(* *..*Controller.*(..))")
  public void controllerPointcut() {
  }

  @Around("controllerPointcut()")
  public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
    LocalDateTime requestTime = LocalDateTime.now();

    ZonedDateTime start = ZonedDateTime.now();

    // ProceedingJoinPoint를 실행하고 결과를 받아옵니다.
    Object result = null;
    try {
      result = joinPoint.proceed();
    } catch (Throwable e) {
      extracted(joinPoint, requestTime, start, e);
      throw new RuntimeException(e);
    }
    extracted(joinPoint, requestTime, start, result);
    return result;
  }

  @Async
  protected void extracted(ProceedingJoinPoint joinPoint, LocalDateTime requestTime, ZonedDateTime start, Object result) {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    var systemLogCreateBuilder = SystemLogCreate.builder()
        .requestUri(request.getRequestURI())
        .controllerName(joinPoint.getSignature().getDeclaringTypeName())
        .methodName(joinPoint.getSignature().getName())
        .httpMethodType(request.getMethod())
        .paramData(getMethodParameter(joinPoint))
        .remoteAddr(getRemoteAddr(request))
        .created(EventLog.builder()
            .at(requestTime)
            .by(SecurityUtils.getUsername())
            .build())
        .processTime(Duration.between(start, ZonedDateTime.now()).toMillis());

    if (result instanceof Throwable) {
      systemLogCreateBuilder
          .status(ApiResponseStatus.FAIL)
          .stackTrace(ExceptionUtils.getStackTrace((Throwable) result));
    } else {
      systemLogCreateBuilder
          .responseBody(writeValueAsString(result));
    }

    systemLogService.save(systemLogCreateBuilder.build());
  }

  private String getRemoteAddr(HttpServletRequest request) {
    String remoteAddr = request.getHeader("X-Forwarded-For");
    if (remoteAddr == null) {
      remoteAddr = request.getRemoteAddr();
    }
    return remoteAddr;
  }

  private String getMethodParameter(ProceedingJoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    Map<String, String> paramMap = new HashMap<>();
    String[] parameterNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
    for (int i = 0; i < parameterNames.length; i++) {
      if (!(args[i] instanceof Errors || args[i] instanceof HttpServletRequest || args[i] instanceof HttpServletResponse)) {
        paramMap.put(parameterNames[i], writeValueAsString(args[i]));
      }
    }
    return writeValueAsString(paramMap);
  }

  private String writeValueAsString(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      log.error("SystemLogAspect: {}", e.getMessage(), e);
      return "Can not converter data.";
    }
  }


}