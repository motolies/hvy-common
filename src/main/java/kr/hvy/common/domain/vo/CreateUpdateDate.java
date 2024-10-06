package kr.hvy.common.domain.vo;


import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class CreateUpdateDate {
  LocalDateTime createDate;
  LocalDateTime updateDate;

  // 기본값을 설정하는 메서드
  public static CreateUpdateDate defaultValues() {
    return CreateUpdateDate.builder()
        .createDate(LocalDateTime.now())
        .updateDate(LocalDateTime.now())
        .build();
  }

  // 업데이트 메서드
  public CreateUpdateDate updated() {
    return this.withUpdateDate(LocalDateTime.now());
  }
}