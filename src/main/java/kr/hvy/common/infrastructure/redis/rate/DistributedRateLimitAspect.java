package kr.hvy.common.infrastructure.redis.rate;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import kr.hvy.common.aop.expression.SpelExpressionService;
import kr.hvy.common.core.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class DistributedRateLimitAspect {

  private final RedissonClient redissonClient;
  private final SpelExpressionService spelExpressionService;

  @Around("@annotation(DistributedRateLimit)")
  public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature sig = (MethodSignature) joinPoint.getSignature();
    Method method = sig.getMethod();
    DistributedRateLimit annotation = method.getAnnotation(DistributedRateLimit.class);

    // 1) 키 결정
    String key;
    if (annotation.key().isEmpty()) {
      key = method.getDeclaringClass().getSimpleName() + ":" + method.getName();
    } else {
      // SpEL을 통해 메소드 인자에서 동적으로 키 값을 가져옴
      String[] parameterNames = sig.getParameterNames();
      Object[] args = joinPoint.getArgs();
      key = spelExpressionService.evaluateExpression(parameterNames, args, annotation.key());
    }

    // 2) 리미터 가져오기/설정
    RateType rateType = annotation.rateType();
    long rate = annotation.rate();
    long permits = annotation.permits();
    Duration interval = toDuration(annotation.unit());

    RRateLimiter limiter = redissonClient.getRateLimiter(key);
    // 최초 설정: 동일 key로 여러번 호출되더라도 첫 설정만 적용됨
    limiter.trySetRate(rateType, rate, interval);

    // 3) 토큰 획득
    boolean ok;
    if (annotation.timeout() > 0) {
      Duration timeout = Duration.of(annotation.timeout(), annotation.unit().toChronoUnit());
      ok = limiter.tryAcquire(permits, timeout);
    } else {
      // 무한 대기
      limiter.acquire(permits);
      ok = true;
    }

    if (!ok) {
      throw new RateLimitExceededException("Rate limit exceeded for key=" + key);
    }

    // 4) 실제 메서드 호출
    return joinPoint.proceed();
  }

  private Duration toDuration(TimeUnit unit) {
    return switch (unit) {
      case MILLISECONDS -> Duration.ofMillis(1);
      case SECONDS -> Duration.ofSeconds(1);
      case MINUTES -> Duration.ofMinutes(1);
      case HOURS -> Duration.ofHours(1);
      default -> Duration.ofSeconds(1);
    };
  }
}