package kr.hvy.common.application.domain.embeddable;

import jakarta.persistence.Embeddable;
import java.time.Instant;
import kr.hvy.common.core.security.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data // VO에서는 @Value를 사용했지만, JPA에서는 @Data가 더 적합합니다.
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventLogEntity {

  // 절대시각(UTC 기준 Instant) — JVM 타임존에 의존하지 않는다
  private Instant at;
  private String by;

  // 기본값을 설정하는 메서드
  public static EventLogEntity defaultValues() {
    return EventLogEntity.builder()
        .at(Instant.now())
        .by(SecurityUtils.getUsername())
        .build();
  }

  // 업데이트 메서드
  public EventLogEntity updated() {
    this.at = Instant.now();
    return this;
  }
}
