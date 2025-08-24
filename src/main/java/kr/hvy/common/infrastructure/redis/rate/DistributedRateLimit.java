package kr.hvy.common.infrastructure.redis.rate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 메서드 호출을 분산 레이트리미팅합니다.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DistributedRateLimit {

  /**
   * Redis에 저장될 리미터 이름(key). 비워두면 "패키지명:클래스명:메서드명"으로 자동 생성.
   */
  String key() default "";

  /**
   * 한 번에 획득할 토큰 수
   */
  long permits() default 1;

  /**
   * 최대 대기 시간 (0이면 무한 대기(acquire) 호출)
   */
  long timeout() default 0;

  /**
   * timeout 단위 및 초기 설정 간격 단위
   */
  TimeUnit unit() default TimeUnit.SECONDS;

  /**
   * RateType 설정 (전체 애플리케이션 전체 제한인지, 클라이언트별 제한인지)
   */
  org.redisson.api.RateType rateType() default org.redisson.api.RateType.OVERALL;

  /**
   * 위 단위 당 permits 수 — 예: unit이 SECONDS, permits=2면 초당 2회 허용
   */
  long rate() default 2;
}
