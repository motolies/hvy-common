package kr.hvy.common.application.domain.vo;


import java.time.LocalDateTime;
import kr.hvy.common.core.security.SecurityUtils;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class EventLog {

  @Builder.Default
  LocalDateTime at = LocalDateTime.now();

  @Builder.Default
  String by = "SYSTEM";

  public static EventLog defaultValues() {
    return EventLog.builder()
        .at(LocalDateTime.now())
        .by(SecurityUtils.getUsername())
        .build();
  }
}