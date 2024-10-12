package kr.hvy.common.domain.vo;


import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class CreateUpdateDate {

  @Builder.Default
  LocalDateTime createDate = LocalDateTime.now();
  @Builder.Default
  LocalDateTime updateDate = LocalDateTime.now();

  public static CreateUpdateDate defaultValues() {
    return CreateUpdateDate.builder()
        .createDate(LocalDateTime.now())
        .updateDate(LocalDateTime.now())
        .build();
  }
}