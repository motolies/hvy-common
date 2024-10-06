package kr.hvy.common.domain.embeddable;

import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data // VO에서는 @Value를 사용했지만, JPA에서는 @Data가 더 적합합니다.
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUpdateDateEntity {
  private LocalDateTime createDate;
  private LocalDateTime updateDate;

  // 기본값을 설정하는 메서드
  public static CreateUpdateDateEntity defaultValues() {
    return CreateUpdateDateEntity.builder()
        .createDate(LocalDateTime.now())
        .updateDate(LocalDateTime.now())
        .build();
  }

  // 업데이트 메서드
  public CreateUpdateDateEntity updated() {
    this.updateDate = LocalDateTime.now();
    return this;
  }
}