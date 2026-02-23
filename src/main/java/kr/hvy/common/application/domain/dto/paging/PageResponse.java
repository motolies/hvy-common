package kr.hvy.common.application.domain.dto.paging;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
public class PageResponse<T> extends BasePage {

  private List<T> list;

  public int getTotalPage() {
    return (int) Math.ceil((double) totalCount / pageSize);
  }

  public boolean hasPrevios() {
    return page > 0;
  }

  public boolean hasNext() {
    return page < getTotalPage() - 1;
  }

  public int getBegin() {
    return Math.max(0, page - 4);
  }

  public int getEnd() {
    return Math.min(page + 5, Math.max(getTotalPage() - 1, 0));
  }


}
