package kr.hvy.common.infrastructure.redis.lock;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
// DistributedLockAop(HIGHEST_PRECEDENCE) 보다 뒤에,
// Spring의 TransactionInterceptor(LOWEST_PRECEDENCE) 보다 앞에 실행되도록 설정
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TransactionLoggingAspect {

  @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
  public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
    String methodName = joinPoint.getSignature().toShortString();
    log.debug("### Transaction AOP Start for {}", methodName);
    try {
      return joinPoint.proceed();
    } finally {
      log.debug("### Transaction AOP End for {}", methodName);
    }
  }
}