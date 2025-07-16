package kr.hvy.common.aop.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

  /*
   * 락 이름의 동적 키가 될 부분을 SpEL(Spring Expression Language)로 표현합니다.
   */
  String key();

  TimeUnit timeUnit() default TimeUnit.SECONDS;

  long waitTime() default 10L; // 락을 기다리는 시간

  long leaseTime() default 10L; // 락 자동 해제 시간(-1: 해제 없음)
}