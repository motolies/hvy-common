package kr.hvy.common.application.domain.dto.paging;

import java.util.ArrayList;
import java.util.List;
import kr.hvy.common.core.security.SecurityUtils;
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
public abstract class PageRequest extends BasePage {

  @Builder.Default
  protected List<OrderBy> orderBy = new ArrayList<>();

  @Builder.Default
  private boolean isAdmin = SecurityUtils.hasAdminRole();

  public int getOffset() {
    return page * pageSize;
  }

}
