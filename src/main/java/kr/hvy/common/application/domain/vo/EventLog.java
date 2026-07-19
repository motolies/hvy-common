package kr.hvy.common.application.domain.vo;


import java.time.Instant;
import kr.hvy.common.core.security.SecurityUtils;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class EventLog {

  @Builder.Default
  Instant at = Instant.now();

  @Builder.Default
  String by = "SYSTEM";

  public static EventLog defaultValues() {
    return EventLog.builder()
        .at(Instant.now())
        .by(SecurityUtils.getUsername())
        .build();
  }
}
