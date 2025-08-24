package kr.hvy.common.application.domain.embeddable;

import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
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

  private LocalDateTime at;
  private String by;

  // 기본값을 설정하는 메서드
  public static EventLogEntity defaultValues() {
    return EventLogEntity.builder()
        .at(LocalDateTime.now())
        .by(SecurityUtils.getUsername())
        .build();
  }

  // 업데이트 메서드
  public EventLogEntity updated() {
    this.at = LocalDateTime.now();
    return this;
  }
}