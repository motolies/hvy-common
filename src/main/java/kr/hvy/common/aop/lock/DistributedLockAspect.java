package kr.hvy.common.aop.lock;

import java.util.Optional;
import kr.hvy.common.exception.RedissonLockAcquisitionException;
import kr.hvy.common.redis.RedissonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@ConditionalOnBean(RedissonUtils.class)
@RequiredArgsConstructor
public class DistributedLockAspect {

  private final RedissonUtils redissonUtils;
  private final SpelExpressionParser parser = new SpelExpressionParser();

  @Around("@annotation(distributedLock)")
  public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String[] parameterNames = signature.getParameterNames();
    Object[] args = joinPoint.getArgs();

    // SpEL을 통해 메소드 인자에서 동적으로 락 키 값을 가져옴
    String dynamicKey = getDynamicValue(parameterNames, args, distributedLock.key());
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

  private String getDynamicValue(String[] parameterNames, Object[] args, String expression) {
    StandardEvaluationContext context = new StandardEvaluationContext();
    for (int i = 0; i < parameterNames.length; i++) {
      context.setVariable(parameterNames[i], args[i]);
    }
    return parser.parseExpression(expression).getValue(context, String.class);
  }
}