package kr.hvy.common.infrastructure.redis.lock;

import java.util.Optional;
import kr.hvy.common.aop.expression.SpelExpressionService;
import kr.hvy.common.core.exception.RedissonLockAcquisitionException;
import kr.hvy.common.infrastructure.redis.util.RedissonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class DistributedLockAspect {

  private final RedissonUtils redissonUtils;
  private final SpelExpressionService spelExpressionService;

  @Around("@annotation(distributedLock)")
  public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String[] parameterNames = signature.getParameterNames();
    Object[] args = joinPoint.getArgs();

    // 키가 SpEL 표현식인지 확인하여 적절히 처리
    String dynamicKey;
    if (spelExpressionService.isSpelExpression(distributedLock.key())) {
      // SpEL 표현식인 경우 평가하여 동적 키 생성
      dynamicKey = spelExpressionService.evaluateExpression(parameterNames, args, distributedLock.key());
    } else {
      // 일반 문자열인 경우 그대로 사용
      dynamicKey = distributedLock.key();
    }
    String lockName = String.format("lock:%s", dynamicKey);

    Optional<RLock> optionalRLock = redissonUtils.tryLock(
        distributedLock.waitTime(),
        distributedLock.leaseTime(),
        distributedLock.timeUnit(),
        lockName
    );

    if (optionalRLock.isEmpty()) {
      log.error("redissonLock 획득 실패. lockName: {}", lockName);
      throw new RedissonLockAcquisitionException(lockName);
    }

    RLock rLock = optionalRLock.get();
    try {
      return joinPoint.proceed();
    } finally {
      if (rLock.isHeldByCurrentThread()) {
        rLock.unlock();
      }
    }
  }
}