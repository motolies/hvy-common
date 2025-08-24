package kr.hvy.common.application.domain.dto.paging;

import kr.hvy.common.core.code.base.EnumCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Direction implements EnumCode<String> {
  ASCENDING("ASC", "오름차순"),
  DESCENDING("DESC", "내림차순");

  private final String code;
  private final String desc;

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getDesc() {
    return desc;
  }
}
