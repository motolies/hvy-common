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
import kr.hvy.common.domain.vo.EventLog;
import kr.hvy.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
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
    Object result = joinPoint.proceed();

    ZonedDateTime end = ZonedDateTime.now();

    // HttpServletRequest를 가져옵니다.
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

    // todo : internal error 시에는 로그를 저장하지 못하는 버그 수정 필요

    SystemLogCreate systemLogCreate = SystemLogCreate.builder()
        .requestUri(request.getRequestURI())
        .controllerName(joinPoint.getSignature().getDeclaringTypeName())
        .methodName(joinPoint.getSignature().getName())
        .httpMethodType(request.getMethod())
        .paramData(getMethodParameter(joinPoint))
        .remoteAddr(request.getRemoteAddr())
        .created(EventLog.builder()
            .at(requestTime)
            .by(SecurityUtils.getUsername())
            .build())
        .processTime(Duration.between(start, end).toMillis())
        .responseBody(writeValueAsString(result))
        .build();

    systemLogService.save(systemLogCreate);

    return result;
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