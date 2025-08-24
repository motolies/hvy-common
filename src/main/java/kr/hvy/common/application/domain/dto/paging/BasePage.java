package kr.hvy.common.application.domain.dto.paging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public abstract class BasePage {

  @Builder.Default
  protected int page = 0;
  @Builder.Default
  protected int pageSize = 10;
  protected int totalCount;
}
